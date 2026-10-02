package io.github.brainboxemb.eventtiming.systemtest.framework;

import java.net.URI;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

/** Queue-backed WebSocket event client for black-box verification. */
public final class EventStream extends WebSocketClient {
    private static final String LOOPBACK = "127.0.0.1";

    private final LinkedBlockingQueue<String> messages =
            new LinkedBlockingQueue<String>();
    private final AtomicReference<Exception> failure =
            new AtomicReference<Exception>();

    private EventStream(URI uri) {
        super(uri);
    }

    public static EventStream connect(int port) throws Exception {
        EventStream stream = new EventStream(
                URI.create("ws://" + LOOPBACK + ":" + port + "/api/v1/events"));
        if (!stream.connectBlocking(5L, TimeUnit.SECONDS)) {
            throw new AssertionError("Timed out opening IF-03 WebSocket");
        }
        stream.throwFailure();
        return stream;
    }

    public String awaitEvent(String eventType) throws Exception {
        long deadline = System.currentTimeMillis() + 5000L;
        while (System.currentTimeMillis() < deadline) {
            throwFailure();
            long remaining = deadline - System.currentTimeMillis();
            String message = messages.poll(
                    Math.max(1L, remaining),
                    TimeUnit.MILLISECONDS);
            if (message == null) {
                break;
            }
            if (message.contains("\"eventType\":\"" + eventType + "\"")) {
                return message;
            }
        }
        throw new AssertionError(
                "Timed out waiting for IF-03 event " + eventType);
    }

    public void assertNoEvent(String eventType, long timeoutMillis)
            throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            throwFailure();
            long remaining = deadline - System.currentTimeMillis();
            String message = messages.poll(
                    Math.max(1L, remaining),
                    TimeUnit.MILLISECONDS);
            if (message == null) {
                return;
            }
            if (message.contains("\"eventType\":\"" + eventType + "\"")) {
                throw new AssertionError(
                        "Unexpected IF-03 event "
                                + eventType
                                + ": "
                                + message);
            }
        }
    }

    public void closeQuietly() {
        try {
            closeBlocking();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void throwFailure() throws Exception {
        if (failure.get() != null) {
            throw failure.get();
        }
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
    }

    @Override
    public void onMessage(String message) {
        messages.offer(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
    }

    @Override
    public void onError(Exception ex) {
        failure.compareAndSet(null, ex);
    }
}
