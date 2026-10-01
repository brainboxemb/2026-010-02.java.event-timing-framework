package io.github.brainboxemb.eventtiming.timingpoint.presentation.interfaces.api;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.github.brainboxemb.eventtiming.timingdata.LocationId;
import io.github.brainboxemb.eventtiming.timingdata.RegistrationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingData;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataCodec;
import io.github.brainboxemb.eventtiming.timingdata.TimingTimestamp;
import io.github.brainboxemb.eventtiming.timingdata.defaultprofile.DefaultTimingDataCodec;
import io.github.brainboxemb.eventtiming.timingpoint.application.CommandHandler;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP/JSON transport for IF-03.
 *
 * <p>This adapter owns only HTTP and JSON mapping. All state-dependent decisions
 * are delegated to {@link CommandHandler}; it never calls TimingNode directly.</p>
 */
public final class HttpEndpoint implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(HttpEndpoint.class);
    private static final int MAX_REQUEST_BODY_BYTES = 64 * 1024;

    private final String bindAddress;
    private final int port;
    private final CommandHandler commandHandler;
    private final TimingDataCodec timingDataCodec;
    private final JsonFactory jsonFactory = new JsonFactory();

    private HttpServer server;
    private ExecutorService executor;

    public HttpEndpoint(String bindAddress, int port, CommandHandler commandHandler) {
        this(bindAddress, port, commandHandler, new DefaultTimingDataCodec());
    }

    HttpEndpoint(
            String bindAddress,
            int port,
            CommandHandler commandHandler,
            TimingDataCodec timingDataCodec) {
        if (bindAddress == null || bindAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("bindAddress must not be blank");
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        if (commandHandler == null) {
            throw new IllegalArgumentException("commandHandler must not be null");
        }
        if (timingDataCodec == null) {
            throw new IllegalArgumentException("timingDataCodec must not be null");
        }
        this.bindAddress = bindAddress.trim();
        this.port = port;
        this.commandHandler = commandHandler;
        this.timingDataCodec = timingDataCodec;
    }

    public synchronized void start() throws IOException {
        if (server != null) {
            throw new IllegalStateException("HTTP status server is already started");
        }

        HttpServer httpServer = HttpServer.create(
                new InetSocketAddress(InetAddress.getByName(bindAddress), port), 0);
        ExecutorService httpExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "event-timing-http");
            thread.setDaemon(true);
            return thread;
        });
        httpServer.setExecutor(httpExecutor);
        httpServer.createContext("/", this::handle);
        httpServer.start();

        executor = httpExecutor;
        server = httpServer;
        LOG.info("HTTP IF-03 listening on {}:{}", bindAddress, httpServer.getAddress().getPort());
    }

    public synchronized int boundPort() {
        if (server == null) {
            throw new IllegalStateException("HTTP IF-03 server is not started");
        }
        return server.getAddress().getPort();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            route(exchange);
        } catch (RequestException ex) {
            sendJson(exchange, 400, MessageWriter.error(ex.code, ex.getMessage()));
        } catch (TimingNode.OperationException ex) {
            sendOperationFailure(exchange, ex);
        } catch (RuntimeException ex) {
            LOG.warn("IF-03 request failed", ex);
            sendJson(
                    exchange,
                    500,
                    MessageWriter.error(
                            "INTERNAL_ERROR",
                            "Unexpected interface failure"));
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        if ("/api/v1/version".equals(path)) {
            requireMethod(exchange, "GET");
            sendJson(exchange, 200, MessageWriter.version(commandHandler.version()));
            return;
        }
        if ("/api/v1/status".equals(path)) {
            requireMethod(exchange, "GET");
            sendJson(
                    exchange,
                    200,
                    MessageWriter.status(commandHandler.version(), commandHandler.status()));
            return;
        }
        if ("/api/v1/capabilities".equals(path)) {
            requireMethod(exchange, "GET");
            sendJson(
                    exchange,
                    200,
                    MessageWriter.capabilities(
                            commandHandler.version(),
                            commandHandler.capabilities()));
            return;
        }
        if ("/api/v1/timing-node/location".equals(path)) {
            requireMethod(exchange, "PUT");
            handleSetLocation(exchange);
            return;
        }
        if ("/api/v1/timing-node/open".equals(path)) {
            requireMethod(exchange, "POST");
            requireEmptyBody(exchange);
            handleOpen(exchange);
            return;
        }
        if ("/api/v1/timing-node/close".equals(path)) {
            requireMethod(exchange, "POST");
            requireEmptyBody(exchange);
            sendJson(
                    exchange,
                    200,
                    MessageWriter.result(
                            commandHandler.version(),
                            commandHandler.close().name()));
            return;
        }
        if ("/api/v1/engineering/accepted-registration".equals(path)) {
            requireMethod(exchange, "POST");
            handleAcceptedRegistration(exchange);
            return;
        }
        if ("/api/v1/timing-data".equals(path)) {
            requireMethod(exchange, "GET");
            handleHistory(exchange);
            return;
        }

        sendJson(
                exchange,
                404,
                MessageWriter.error("NOT_FOUND", "Unknown IF-03 resource"));
    }

    private void handleSetLocation(HttpExchange exchange) throws IOException {
        int value = readLocationRequest(exchange);
        final LocationId locationId;
        try {
            locationId = new LocationId(value);
        } catch (IllegalArgumentException ex) {
            throw invalidValue(ex.getMessage());
        }

        TimingNode.SetLocationResult result = commandHandler.setLocation(locationId);
        if (result == TimingNode.SetLocationResult.NODE_NOT_CLOSED) {
            sendJson(
                    exchange,
                    409,
                    MessageWriter.error(
                            "NODE_NOT_CLOSED",
                            "LocationId can change only while the TimingNode is CLOSED"));
            return;
        }
        sendJson(
                exchange,
                200,
                MessageWriter.result(commandHandler.version(), result.name()));
    }

    private void handleOpen(HttpExchange exchange) throws IOException {
        TimingNode.OpenResult result = commandHandler.open();
        if (result == TimingNode.OpenResult.NO_LOCATION) {
            sendJson(
                    exchange,
                    409,
                    MessageWriter.error(
                            "NO_LOCATION",
                            "A current LocationId is required before OPEN"));
            return;
        }
        sendJson(
                exchange,
                200,
                MessageWriter.result(commandHandler.version(), result.name()));
    }

    private void handleAcceptedRegistration(HttpExchange exchange) throws IOException {
        AcceptedRegistrationRequest request = readAcceptedRegistrationRequest(exchange);

        final RegistrationId registrationId;
        final TimingTimestamp observationTime;
        try {
            registrationId = new RegistrationId(request.registrationId);
            observationTime = TimingTimestamp.parse(request.observationTime);
        } catch (IllegalArgumentException ex) {
            throw invalidValue(ex.getMessage());
        }

        if (!commandHandler.capabilities().directRegistrationSimulationEnabled()) {
            sendJson(
                    exchange,
                    403,
                    MessageWriter.error(
                            "CAPABILITY_NOT_ENABLED",
                            "Direct registration simulation is not enabled"));
            return;
        }

        TimingNode.RegistrationResult result =
                commandHandler.registerAccepted(registrationId, observationTime);
        if (result.outcome() == TimingNode.RegistrationResult.Outcome.NODE_NOT_OPEN) {
            sendJson(
                    exchange,
                    409,
                    MessageWriter.error(
                            "NODE_NOT_OPEN",
                            "Accepted registration requires an OPEN TimingNode"));
            return;
        }
        sendJson(
                exchange,
                200,
                MessageWriter.committedRegistration(
                        commandHandler.version(),
                        result.timingData()));
    }

    private void handleHistory(HttpExchange exchange) throws IOException {
        try {
            sendJson(
                    exchange,
                    200,
                    MessageWriter.history(
                            commandHandler.version(),
                            commandHandler.status().timingNodeId(),
                            commandHandler.timingDataHistory(),
                            timingDataCodec));
        } catch (TimingDataCodec.CodecException ex) {
            throw new IllegalStateException("Could not encode committed TimingData history", ex);
        }
    }

    private void sendOperationFailure(
            HttpExchange exchange,
            TimingNode.OperationException failure)
            throws IOException {
        switch (failure.reason()) {
            case BUSY:
                sendJson(exchange, 503, MessageWriter.error("BUSY", failure.getMessage()));
                return;
            case UNAVAILABLE:
                sendJson(exchange, 503, MessageWriter.error("UNAVAILABLE", failure.getMessage()));
                return;
            case TIMEOUT:
                sendJson(
                        exchange,
                        504,
                        MessageWriter.error("OUTCOME_UNKNOWN", failure.getMessage()));
                return;
            case INTERRUPTED:
                sendJson(exchange, 503, MessageWriter.error("INTERRUPTED", failure.getMessage()));
                return;
            case FAILED:
            default:
                sendJson(
                        exchange,
                        503,
                        MessageWriter.error("OPERATION_FAILED", failure.getMessage()));
        }
    }

    private static void requireMethod(HttpExchange exchange, String required)
            throws IOException {
        if (required.equals(exchange.getRequestMethod())) {
            return;
        }
        sendJson(
                exchange,
                405,
                MessageWriter.error(
                        "METHOD_NOT_ALLOWED",
                        "Expected " + required + " for this resource"));
        throw ResponseAlreadySent.INSTANCE;
    }

    private static void requireEmptyBody(HttpExchange exchange) throws IOException {
        byte[] body = readBody(exchange);
        for (byte value : body) {
            if (!Character.isWhitespace((char) (value & 0xff))) {
                throw malformed("This operation does not accept a request body");
            }
        }
    }

    private int readLocationRequest(HttpExchange exchange) throws IOException {
        byte[] body = readBody(exchange);
        try (JsonParser parser = jsonFactory.createParser(body)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                throw malformed("Request must be one JSON object");
            }
            Integer locationId = null;
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                if (parser.currentToken() != JsonToken.FIELD_NAME) {
                    throw malformed("Expected JSON member name");
                }
                String name = parser.currentName();
                JsonToken value = parser.nextToken();
                if ("locationId".equals(name)) {
                    if (locationId != null || value != JsonToken.VALUE_NUMBER_INT) {
                        throw invalidValue("locationId must be one JSON integer");
                    }
                    locationId = parser.getIntValue();
                } else {
                    throw invalidValue("Unsupported request field: " + name);
                }
            }
            if (parser.nextToken() != null) {
                throw malformed("Unexpected data after request object");
            }
            if (locationId == null) {
                throw invalidValue("Missing required field: locationId");
            }
            return locationId.intValue();
        } catch (RequestException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw malformed("Malformed JSON request", ex);
        }
    }

    private AcceptedRegistrationRequest readAcceptedRegistrationRequest(
            HttpExchange exchange)
            throws IOException {
        byte[] body = readBody(exchange);
        try (JsonParser parser = jsonFactory.createParser(body)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                throw malformed("Request must be one JSON object");
            }
            String registrationId = null;
            String observationTime = null;
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                if (parser.currentToken() != JsonToken.FIELD_NAME) {
                    throw malformed("Expected JSON member name");
                }
                String name = parser.currentName();
                JsonToken value = parser.nextToken();
                if ("registrationId".equals(name)) {
                    if (registrationId != null || value != JsonToken.VALUE_STRING) {
                        throw invalidValue("registrationId must be one JSON string");
                    }
                    registrationId = parser.getText();
                } else if ("observationTime".equals(name)) {
                    if (observationTime != null || value != JsonToken.VALUE_STRING) {
                        throw invalidValue("observationTime must be one JSON string");
                    }
                    observationTime = parser.getText();
                } else {
                    throw invalidValue("Unsupported request field: " + name);
                }
            }
            if (parser.nextToken() != null) {
                throw malformed("Unexpected data after request object");
            }
            if (registrationId == null) {
                throw invalidValue("Missing required field: registrationId");
            }
            if (observationTime == null) {
                throw invalidValue("Missing required field: observationTime");
            }
            return new AcceptedRegistrationRequest(registrationId, observationTime);
        } catch (RequestException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw malformed("Malformed JSON request", ex);
        }
    }

    private static byte[] readBody(HttpExchange exchange) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        int total = 0;
        try (InputStream input = exchange.getRequestBody()) {
            while ((read = input.read(buffer)) >= 0) {
                total += read;
                if (total > MAX_REQUEST_BODY_BYTES) {
                    throw malformed("Request body exceeds 64 KiB");
                }
                output.write(buffer, 0, read);
            }
        }
        return output.toByteArray();
    }

    private static RequestException malformed(String message) {
        return new RequestException("MALFORMED_REQUEST", message, null);
    }

    private static RequestException malformed(String message, Throwable cause) {
        return new RequestException("MALFORMED_REQUEST", message, cause);
    }

    private static RequestException invalidValue(String message) {
        return new RequestException("INVALID_VALUE", message, null);
    }

    private static void sendJson(HttpExchange exchange, int status, String json)
            throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        } finally {
            exchange.close();
        }
    }

    @Override
    public synchronized void close() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private static final class AcceptedRegistrationRequest {
        private final String registrationId;
        private final String observationTime;

        private AcceptedRegistrationRequest(
                String registrationId,
                String observationTime) {
            this.registrationId = registrationId;
            this.observationTime = observationTime;
        }
    }

    private static class RequestException extends RuntimeException {
        private final String code;

        private RequestException(String code, String message, Throwable cause) {
            super(message, cause);
            this.code = code;
        }
    }

    /**
     * Internal control-flow marker used only after a response was already sent.
     *
     * <p>It is deliberately not logged as an interface failure.</p>
     */
    private static final class ResponseAlreadySent extends RuntimeException {
        private static final ResponseAlreadySent INSTANCE = new ResponseAlreadySent();

        private ResponseAlreadySent() {
            super(null, null, false, false);
        }
    }
}
