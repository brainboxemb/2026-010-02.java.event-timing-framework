package io.github.brainboxemb.eventtiming.presentation.interfaces.remoteapi.websocket;

import io.github.brainboxemb.eventtiming.application.ApplicationStatus;
import io.github.brainboxemb.eventtiming.application.CommandHandler;
import io.github.brainboxemb.eventtiming.domain.timing.TimingNode;
import io.github.brainboxemb.eventtiming.domain.timing.TimingNodeId;
import io.github.brainboxemb.eventtiming.infra.BuildIdentity;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RemoteApiWebSocketServerTest {
    @Test
    public void sendsCompleteSnapshotOnConnectAndReconnect() throws Exception {
        AtomicReference<ApplicationStatus> status = new AtomicReference<>(status("timing-node-01"));
        RemoteApiWebSocketServer server = server(status);
        server.start();

        try {
            String first = connectAndReceiveSnapshot(server.boundPort());
            assertSnapshot(first, "timing-node-01");

            String second = connectAndReceiveSnapshot(server.boundPort());
            assertSnapshot(second, "timing-node-01");
        } finally {
            server.close();
        }
    }

    @Test
    public void broadcastsCompleteStatusChangedEvent() throws Exception {
        AtomicReference<ApplicationStatus> status = new AtomicReference<>(status("timing-node-01"));
        RemoteApiWebSocketServer server = server(status);
        server.start();
        TestClient client = connect(server.boundPort());

        try {
            assertSnapshot(client.awaitMessage(), "timing-node-01");

            status.set(status("timing-node-02"));
            server.publishStatusChanged();

            String changed = client.awaitMessage();
            assertTrue(changed.contains("\"eventType\":\"STATUS_CHANGED\""));
            assertTrue(changed.contains("\"timingNodeId\":\"timing-node-02\""));
            assertTrue(changed.contains("\"lifecycle\":\"CLOSED\""));
        } finally {
            client.closeBlocking();
            server.close();
        }
    }

    private static RemoteApiWebSocketServer server(
            AtomicReference<ApplicationStatus> status) {
        BuildIdentity identity = BuildIdentity.firstApiVersion(
                "event-timing-app",
                "test-version",
                "abc123def456",
                "feature/test",
                "local",
                false);
        CommandHandler handler = new CommandHandler(identity, status::get);
        return new RemoteApiWebSocketServer(
                "127.0.0.1",
                0,
                handler,
                Clock.fixed(Instant.parse("2026-09-25T15:00:00Z"), ZoneOffset.UTC));
    }

    private static ApplicationStatus status(String timingNodeId) {
        return new ApplicationStatus(
                new TimingNodeId(timingNodeId),
                TimingNode.Lifecycle.CLOSED);
    }

    private static String connectAndReceiveSnapshot(int port) throws Exception {
        TestClient client = connect(port);
        try {
            String message = client.awaitMessage();
            assertSnapshot(message, "timing-node-01");
            return message;
        } finally {
            client.closeBlocking();
        }
    }

    private static TestClient connect(int port) throws Exception {
        TestClient client = new TestClient(
                new URI("ws://127.0.0.1:" + port + RemoteApiWebSocketServer.EVENTS_PATH));
        assertTrue(client.connectBlocking(2, TimeUnit.SECONDS));
        return client;
    }

    private static void assertSnapshot(String json, String timingNodeId) {
        assertNotNull(json);
        assertTrue(json.contains("\"eventType\":\"STATUS_SNAPSHOT\""));
        assertTrue(json.contains("\"occurredAt\":\"2026-09-25T15:00:00Z\""));
        assertTrue(json.contains("\"timingNodeId\":\"" + timingNodeId + "\""));
        assertTrue(json.contains("\"lifecycle\":\"CLOSED\""));
        assertTrue(json.contains("\"problems\":[]"));
    }

    private static final class TestClient extends WebSocketClient {
        private final LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<>();

        private TestClient(URI uri) {
            super(uri);
        }

        private String awaitMessage() throws InterruptedException {
            return messages.poll(2, TimeUnit.SECONDS);
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
        }
    }
}
