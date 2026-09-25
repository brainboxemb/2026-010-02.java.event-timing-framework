package io.github.brainboxemb.eventtiming.testclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Independent IF-03 WebSocket client used by the JavaFX development tool. */
public final class StatusWebSocketClient implements AutoCloseable {
    public interface Listener {
        void onConnected();

        void onEvent(StatusEvent event);

        void onClosed(int statusCode, String reason);

        void onError(String message);
    }

    public record StatusEvent(
            String eventType,
            Instant occurredAt,
            ApplicationControlClient.StatusResult status,
            String rawJson) {
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private WebSocket webSocket;
    private boolean connecting;

    public synchronized CompletableFuture<Void> connect(URI endpoint, Listener listener) {
        validateEndpoint(endpoint);
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        if (webSocket != null || connecting) {
            throw new IllegalStateException("WebSocket client is already connected or connecting");
        }

        connecting = true;
        ClientListener bridge = new ClientListener(listener);
        CompletableFuture<Void> result = new CompletableFuture<>();

        httpClient.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .buildAsync(endpoint, bridge)
                .whenComplete((socket, error) -> {
                    if (error != null) {
                        synchronized (StatusWebSocketClient.this) {
                            connecting = false;
                        }
                        result.completeExceptionally(error);
                        return;
                    }

                    synchronized (StatusWebSocketClient.this) {
                        webSocket = socket;
                        connecting = false;
                    }
                    result.complete(null);
                });

        return result;
    }

    public synchronized boolean isConnected() {
        return webSocket != null && !webSocket.isOutputClosed();
    }

    public void disconnect() {
        WebSocket current;
        synchronized (this) {
            current = webSocket;
            webSocket = null;
            connecting = false;
        }
        if (current != null && !current.isOutputClosed()) {
            current.sendClose(WebSocket.NORMAL_CLOSURE, "client disconnect");
        }
    }

    static StatusEvent parseEvent(String rawJson) throws IOException {
        JsonNode root = JSON.readTree(rawJson);
        String eventType = ApplicationControlClient.requiredText(root, "eventType");
        Instant occurredAt = Instant.parse(
                ApplicationControlClient.requiredText(root, "occurredAt"));
        JsonNode payload = ApplicationControlClient.required(root, "payload");
        String payloadJson = JSON.writeValueAsString(payload);
        return new StatusEvent(
                eventType,
                occurredAt,
                ApplicationControlClient.parseStatus(payloadJson),
                rawJson);
    }

    private static void validateEndpoint(URI endpoint) {
        if (endpoint == null || endpoint.getScheme() == null || endpoint.getHost() == null) {
            throw new IllegalArgumentException("endpoint must be an absolute WebSocket URI");
        }
        if (!"ws".equalsIgnoreCase(endpoint.getScheme())
                && !"wss".equalsIgnoreCase(endpoint.getScheme())) {
            throw new IllegalArgumentException("endpoint scheme must be ws or wss");
        }
    }

    private synchronized void disconnected(WebSocket socket) {
        if (webSocket == socket) {
            webSocket = null;
        }
        connecting = false;
    }

    @Override
    public void close() {
        disconnect();
    }

    private final class ClientListener implements WebSocket.Listener {
        private final Listener listener;
        private final StringBuilder text = new StringBuilder();

        private ClientListener(Listener listener) {
            this.listener = listener;
        }

        @Override
        public void onOpen(WebSocket socket) {
            socket.request(1);
            listener.onConnected();
        }

        @Override
        public CompletionStage<?> onText(
                WebSocket socket,
                CharSequence data,
                boolean last) {
            text.append(data);
            if (last) {
                String rawJson = text.toString();
                text.setLength(0);
                try {
                    listener.onEvent(parseEvent(rawJson));
                } catch (Exception ex) {
                    listener.onError(message(ex));
                }
            }
            socket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(
                WebSocket socket,
                int statusCode,
                String reason) {
            disconnected(socket);
            listener.onClosed(statusCode, reason);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket socket, Throwable error) {
            disconnected(socket);
            listener.onError(message(error));
        }
    }

    private static String message(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String value = cause.getMessage();
        return value == null || value.trim().isEmpty() ? cause.toString() : value;
    }
}
