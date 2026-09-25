package io.github.brainboxemb.eventtiming.presentation.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.github.brainboxemb.eventtiming.application.CommandHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Lightweight HTTP/JSON adapter for the IF-03 first-executable resources. */
public final class HttpStatusServer implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(HttpStatusServer.class);

    private final String bindAddress;
    private final int port;
    private final CommandHandler commandHandler;

    private HttpServer server;
    private ExecutorService executor;

    public HttpStatusServer(String bindAddress, int port, CommandHandler commandHandler) {
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
        LOG.info("HTTP status listening on {}:{}", bindAddress, httpServer.getAddress().getPort());
    }

    public synchronized int boundPort() {
        if (server == null) {
            throw new IllegalStateException("HTTP status server is not started");
        }
        return server.getAddress().getPort();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            route(exchange);
        } catch (RuntimeException ex) {
            LOG.warn("IF-03 request failed", ex);
            sendJson(
                    exchange,
                    500,
                    ApplicationControlJson.error(
                            "INTERNAL_ERROR",
                            "Unexpected interface failure"));
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(
                    exchange,
                    405,
                    ApplicationControlJson.error(
                            "METHOD_NOT_ALLOWED",
                            "Only GET is supported for this resource"));
            return;
        }

        if ("/api/v1/version".equals(path)) {
            sendJson(exchange, 200, ApplicationControlJson.version(commandHandler.version()));
            return;
        }
        if ("/api/v1/status".equals(path)) {
            sendJson(
                    exchange,
                    200,
                    ApplicationControlJson.status(
                            commandHandler.version(),
                            commandHandler.status()));
            return;
        }

        sendJson(
                exchange,
                404,
                ApplicationControlJson.error("NOT_FOUND", "Unknown IF-03 resource"));
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
}
