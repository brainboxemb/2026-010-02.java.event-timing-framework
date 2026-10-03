package io.github.brainboxemb.eventtiming.timingpoint.presentation.interfaces.api;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.github.brainboxemb.eventtiming.timingdata.TimingDataTypes.LocationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataTypes.RegistrationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataCodec;
import io.github.brainboxemb.eventtiming.timingdata.TimingTimestamp;
import io.github.brainboxemb.eventtiming.timingdata.defaultprofile.DefaultTimingDataCodec;
import io.github.brainboxemb.eventtiming.timingpoint.application.CommandHandler;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeTypes.CloseResult;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeTypes.OpenResult;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeTypes.OperationException;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeTypes.RegistrationResult;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeTypes.SetLocationResult;

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
    private static final int MAX_LOGBOOK_LIMIT = 1000;

    private final String bindAddress;
    private final int port;
    private final CommandHandler commandHandler;
    private final TimingDataCodec timingDataCodec;
    private final JsonFactory jsonFactory = new JsonFactory();

    private HttpServer server;
    private ExecutorService executor;

    public HttpEndpoint(String bindAddress, int port, CommandHandler commandHandler) {
        if (bindAddress == null || bindAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("bindAddress must not be blank");
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        if (commandHandler == null) {
            throw new IllegalArgumentException("commandHandler must not be null");
        }
        this.bindAddress = bindAddress.trim();
        this.port = port;
        this.commandHandler = commandHandler;
        this.timingDataCodec = new DefaultTimingDataCodec();
    }

    public synchronized void start() throws IOException {
        if (server != null) {
            throw new IllegalStateException("HTTP status server is already started");
        }

        HttpServer httpServer = HttpServer.create(
                new InetSocketAddress(InetAddress.getByName(bindAddress), port), 0);
        ExecutorService httpExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "tp-prl-api-http");
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
        } catch (ResponseAlreadySent ignored) {
            // Method validation already wrote and closed the HTTP response.
        } catch (RequestException ex) {
            sendJson(exchange, 400, MessageWriter.error(ex.code, ex.getMessage()));
        } catch (OperationException ex) {
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
            sendJson(exchange, 200, MessageWriter.status(commandHandler.status()));
            return;
        }
        if ("/api/v1/capabilities".equals(path)) {
            requireMethod(exchange, "GET");
            sendJson(
                    exchange,
                    200,
                    MessageWriter.capabilities(commandHandler.capabilities()));
            return;
        }
        if (path.startsWith("/api/v1/node/")) {
            routeNode(exchange, path.substring("/api/v1/node/".length()));
            return;
        }
        if (path.startsWith("/api/v1/dev/node/")) {
            routeDevNode(exchange, path.substring("/api/v1/dev/node/".length()));
            return;
        }

        sendJson(
                exchange,
                404,
                MessageWriter.error("NOT_FOUND", "Unknown IF-03 resource"));
    }

    private void routeNode(HttpExchange exchange, String remainder) throws IOException {
        NodeRoute route = nodeRoute(remainder);
        if (route == null) {
            sendJson(
                    exchange,
                    404,
                    MessageWriter.error("NOT_FOUND", "Unknown TimingNode resource"));
            return;
        }
        requireCurrentNode(exchange, route.nodeId);

        if ("/location".equals(route.resource)) {
            requireMethod(exchange, "PUT");
            handleSetLocation(exchange);
            return;
        }
        if ("/open".equals(route.resource)) {
            requireMethod(exchange, "POST");
            requireEmptyBody(exchange);
            handleOpen(exchange);
            return;
        }
        if ("/close".equals(route.resource)) {
            requireMethod(exchange, "POST");
            requireEmptyBody(exchange);
            sendJson(
                    exchange,
                    200,
                    MessageWriter.result(commandHandler.close().name()));
            return;
        }
        if ("/logbook".equals(route.resource)) {
            requireMethod(exchange, "GET");
            handleLogBook(exchange);
            return;
        }

        sendJson(
                exchange,
                404,
                MessageWriter.error("NOT_FOUND", "Unknown TimingNode resource"));
    }

    private void routeDevNode(HttpExchange exchange, String remainder) throws IOException {
        NodeRoute route = nodeRoute(remainder);
        if (route == null) {
            sendJson(
                    exchange,
                    404,
                    MessageWriter.error("NOT_FOUND", "Unknown IF-03 dev resource"));
            return;
        }
        requireCurrentNode(exchange, route.nodeId);

        if ("/auto-reg".equals(route.resource)) {
            requireMethod(exchange, "POST");
            handleAutoRegistration(exchange);
            return;
        }

        sendJson(
                exchange,
                404,
                MessageWriter.error("NOT_FOUND", "Unknown IF-03 dev resource"));
    }

    private static NodeRoute nodeRoute(String remainder) {
        int slash = remainder.indexOf('/');
        if (slash <= 0 || slash == remainder.length() - 1) {
            return null;
        }
        return new NodeRoute(
                remainder.substring(0, slash),
                remainder.substring(slash));
    }

    private void requireCurrentNode(HttpExchange exchange, String nodeId)
            throws IOException {
        if (commandHandler.status().timingNodeId().value().equals(nodeId)) {
            return;
        }
        sendJson(
                exchange,
                404,
                MessageWriter.error(
                        "NODE_NOT_FOUND",
                        "Unknown TimingNode id"));
        throw ResponseAlreadySent.INSTANCE;
    }

    private void handleSetLocation(HttpExchange exchange) throws IOException {
        int value = readLocationRequest(exchange);
        final LocationId locationId;
        try {
            locationId = new LocationId(value);
        } catch (IllegalArgumentException ex) {
            throw invalidValue(ex.getMessage());
        }

        SetLocationResult result = commandHandler.setLocation(locationId);
        if (result == SetLocationResult.NODE_NOT_CLOSED) {
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
                MessageWriter.result(result.name()));
    }

    private void handleOpen(HttpExchange exchange) throws IOException {
        OpenResult result = commandHandler.open();
        if (result == OpenResult.NO_LOCATION) {
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
                MessageWriter.result(result.name()));
    }

    private void handleAutoRegistration(HttpExchange exchange) throws IOException {
        AutoRegistrationRequest request = readAutoRegistrationRequest(exchange);

        final RegistrationId registrationId;
        final TimingTimestamp observationTime;
        try {
            registrationId = new RegistrationId(request.id);
            observationTime = TimingTimestamp.parse(request.time);
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

        RegistrationResult result =
                commandHandler.commitAutomaticRegistration(registrationId, observationTime);
        if (result.outcome() == RegistrationResult.Outcome.NODE_NOT_OPEN) {
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
                        result.timingData()));
    }

    private void handleLogBook(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isEmpty()) {
            sendJson(
                    exchange,
                    200,
                    MessageWriter.logBookInfo(commandHandler.logBookCount()));
            return;
        }

        LogBookQuery request = readLogBookQuery(query);
        StringBuilder records = new StringBuilder();
        int count;
        if (request.last != null) {
            count = commandHandler.visitLatestLogBook(
                    request.last.intValue(),
                    data -> MessageWriter.appendLogBookRecord(
                            records,
                            data,
                            timingDataCodec));
        } else {
            count = commandHandler.visitLogBookFrom(
                    request.from.longValue(),
                    request.limit.intValue(),
                    data -> MessageWriter.appendLogBookRecord(
                            records,
                            data,
                            timingDataCodec));
        }

        Long next = null;
        if (request.from != null && request.from.longValue() <= count) {
            long lastReturned = Math.min(
                    (long) count,
                    request.from.longValue()
                            + request.limit.intValue()
                            - 1L);
            long candidate = lastReturned + 1L;
            if (candidate <= count) {
                next = Long.valueOf(candidate);
            }
        }

        sendJson(
                exchange,
                200,
                MessageWriter.logBookPage(
                        count,
                        next,
                        records));
    }

    private LogBookQuery readLogBookQuery(String query) {
        Long from = null;
        Integer limit = null;
        Integer last = null;

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int equals = pair.indexOf('=');
            if (equals <= 0 || equals == pair.length() - 1) {
                throw invalidValue("LogBook query parameters require a value");
            }
            String name = pair.substring(0, equals);
            String value = pair.substring(equals + 1);

            if ("from".equals(name)) {
                if (from != null) {
                    throw invalidValue("Duplicate LogBook query field: from");
                }
                from = Long.valueOf(parsePositiveLong("from", value));
            } else if ("limit".equals(name)) {
                if (limit != null) {
                    throw invalidValue("Duplicate LogBook query field: limit");
                }
                limit = Integer.valueOf(parseLogBookLimit("limit", value));
            } else if ("last".equals(name)) {
                if (last != null) {
                    throw invalidValue("Duplicate LogBook query field: last");
                }
                last = Integer.valueOf(parseLogBookLimit("last", value));
            } else {
                throw invalidValue("Unsupported LogBook query field: " + name);
            }
        }

        if (last != null) {
            if (from != null || limit != null) {
                throw invalidValue("last cannot be combined with from or limit");
            }
            return new LogBookQuery(null, null, last);
        }
        if (from == null || limit == null) {
            throw invalidValue("LogBook range requires both from and limit");
        }
        return new LogBookQuery(from, limit, null);
    }

    private static long parsePositiveLong(String name, String value) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed < 1L) {
                throw invalidValue(name + " must be >= 1");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw invalidValue(name + " must be a positive integer");
        }
    }

    private static int parseLogBookLimit(String name, String value) {
        long parsed = parsePositiveLong(name, value);
        if (parsed > MAX_LOGBOOK_LIMIT) {
            throw invalidValue(name + " must be <= " + MAX_LOGBOOK_LIMIT);
        }
        return (int) parsed;
    }

    private void sendOperationFailure(
            HttpExchange exchange,
            OperationException failure)
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

    private AutoRegistrationRequest readAutoRegistrationRequest(
            HttpExchange exchange)
            throws IOException {
        byte[] body = readBody(exchange);
        try (JsonParser parser = jsonFactory.createParser(body)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                throw malformed("Request must be one JSON object");
            }
            String id = null;
            String time = null;
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                if (parser.currentToken() != JsonToken.FIELD_NAME) {
                    throw malformed("Expected JSON member name");
                }
                String name = parser.currentName();
                JsonToken value = parser.nextToken();
                if ("id".equals(name)) {
                    if (id != null || value != JsonToken.VALUE_STRING) {
                        throw invalidValue("id must be one JSON string");
                    }
                    id = parser.getText();
                } else if ("time".equals(name)) {
                    if (time != null || value != JsonToken.VALUE_STRING) {
                        throw invalidValue("time must be one JSON string");
                    }
                    time = parser.getText();
                } else {
                    throw invalidValue("Unsupported request field: " + name);
                }
            }
            if (parser.nextToken() != null) {
                throw malformed("Unexpected data after request object");
            }
            if (id == null) {
                throw invalidValue("Missing required field: id");
            }
            if (time == null) {
                throw invalidValue("Missing required field: time");
            }
            return new AutoRegistrationRequest(id, time);
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

    private static final class NodeRoute {
        private final String nodeId;
        private final String resource;

        private NodeRoute(String nodeId, String resource) {
            this.nodeId = nodeId;
            this.resource = resource;
        }
    }

    private static final class LogBookQuery {
        private final Long from;
        private final Integer limit;
        private final Integer last;

        private LogBookQuery(Long from, Integer limit, Integer last) {
            this.from = from;
            this.limit = limit;
            this.last = last;
        }
    }

    private static final class AutoRegistrationRequest {
        private final String id;
        private final String time;

        private AutoRegistrationRequest(
                String id,
                String time) {
            this.id = id;
            this.time = time;
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
