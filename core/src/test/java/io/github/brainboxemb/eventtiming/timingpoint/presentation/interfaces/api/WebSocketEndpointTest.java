package io.github.brainboxemb.eventtiming.timingpoint.presentation.interfaces.api;

import io.github.brainboxemb.eventtiming.timingdata.LocationId;
import io.github.brainboxemb.eventtiming.timingdata.RegistrationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingData;
import io.github.brainboxemb.eventtiming.timingdata.TimingNodeId;
import io.github.brainboxemb.eventtiming.timingdata.TimingTimestamp;
import io.github.brainboxemb.eventtiming.timingdata.defaultprofile.DefaultTimingDataFactory;
import io.github.brainboxemb.eventtiming.timingpoint.application.CommandHandler;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNode;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timingdata.TimingDataStore;
import io.github.brainboxemb.eventtiming.timingpoint.infra.BuildIdentity;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WebSocketEndpointTest {
    private static final TimingTimestamp OBSERVATION_TIME =
            TimingTimestamp.parse("2026-10-01T12:00:00.000000000Z");
    private static final TimingTimestamp RECORDED_AT =
            TimingTimestamp.parse("2026-10-01T12:00:01.000000000Z");
    private static final Clock EVENT_CLOCK =
            Clock.fixed(Instant.parse("2026-10-01T12:00:02Z"), ZoneOffset.UTC);

    @Test
    public void sendsCompleteSnapshotOnConnectAndReconnectWithoutHistoryReplay()
            throws Exception {
        Fixture fixture = new Fixture();
        fixture.start();

        // Commit history before the WebSocket endpoint/client exists.
        fixture.handler.setLocation(new LocationId(24));
        fixture.handler.open();
        fixture.handler.registerAccepted(
                new RegistrationId("sample-before-connect"),
                OBSERVATION_TIME);

        WebSocketEndpoint server = new WebSocketEndpoint(
                "127.0.0.1",
                0,
                fixture.handler,
                EVENT_CLOCK);
        server.start();

        try {
            TestClient first = connect(server.boundPort());
            try {
                String snapshot = first.awaitMessage();
                assertSnapshot(snapshot, "timing-node-01", "OPEN", "24");
                assertNull(first.pollMessage(250));
            } finally {
                first.closeBlocking();
            }

            TestClient second = connect(server.boundPort());
            try {
                String snapshot = second.awaitMessage();
                assertSnapshot(snapshot, "timing-node-01", "OPEN", "24");
                assertNull(second.pollMessage(250));
            } finally {
                second.closeBlocking();
            }
        } finally {
            server.close();
            fixture.close();
        }
    }

    @Test
    public void broadcastsStatusChangesAndCommittedTimingDataAutomatically()
            throws Exception {
        Fixture fixture = new Fixture();
        fixture.start();
        WebSocketEndpoint server = new WebSocketEndpoint(
                "127.0.0.1",
                0,
                fixture.handler,
                EVENT_CLOCK);
        server.start();
        TestClient client = connect(server.boundPort());

        try {
            assertSnapshot(
                    client.awaitMessage(),
                    "timing-node-01",
                    "CLOSED",
                    "null");

            fixture.handler.setLocation(new LocationId(24));
            String located = client.awaitMessage();
            assertTrue(located.contains("\"eventType\":\"STATUS_CHANGED\""));
            assertTrue(located.contains("\"locationId\":24"));
            assertTrue(located.contains("\"lifecycle\":\"CLOSED\""));

            fixture.handler.open();
            String opened = client.awaitMessage();
            assertTrue(opened.contains("\"eventType\":\"STATUS_CHANGED\""));
            assertTrue(opened.contains("\"lifecycle\":\"OPEN\""));

            fixture.handler.registerAccepted(
                    new RegistrationId("sample-001"),
                    OBSERVATION_TIME);
            String committed = client.awaitMessage();
            assertTrue(committed.contains(
                    "\"eventType\":\"TIMING_DATA_COMMITTED\""));
            assertTrue(committed.contains("\"sequenceNumber\":1"));
            assertTrue(committed.contains("\"locationId\":24"));
            assertTrue(committed.contains(
                    "\"registrationId\":\"sample-001\""));
            assertTrue(committed.contains("\"origin\":\"AUTOMATIC\""));
            assertTrue(committed.contains(
                    "\"occurredAt\":\"2026-10-01T12:00:02Z\""));
        } finally {
            client.closeBlocking();
            server.close();
            fixture.close();
        }
    }

    private static TestClient connect(int port) throws Exception {
        TestClient client = new TestClient(
                new URI("ws://127.0.0.1:" + port + WebSocketEndpoint.EVENTS_PATH));
        assertTrue(client.connectBlocking(2, TimeUnit.SECONDS));
        return client;
    }

    private static void assertSnapshot(
            String json,
            String timingNodeId,
            String lifecycle,
            String locationJson) {
        assertNotNull(json);
        assertTrue(json.contains("\"eventType\":\"STATUS_SNAPSHOT\""));
        assertTrue(json.contains("\"occurredAt\":\"2026-10-01T12:00:02Z\""));
        assertTrue(json.contains("\"timingNodeId\":\"" + timingNodeId + "\""));
        assertTrue(json.contains("\"locationId\":" + locationJson));
        assertTrue(json.contains("\"lifecycle\":\"" + lifecycle + "\""));
        assertTrue(json.contains("\"problems\":[]"));
    }

    private static BuildIdentity identity() {
        return BuildIdentity.firstApiVersion(
                "event-timing-app",
                "test-version",
                "abc123def456",
                "feature/test",
                "local",
                false);
    }

    private static final class Fixture implements AutoCloseable {
        private final TimingNode node;
        private final CommandHandler handler;

        private Fixture() {
            node = new TimingNode(
                    new TimingNodeId("timing-node-01"),
                    new MemoryStore(),
                    new DefaultTimingDataFactory(),
                    () -> RECORDED_AT);
            handler = new CommandHandler(identity(), node);
        }

        private void start() {
            node.start();
        }

        @Override
        public void close() {
            node.stop();
        }
    }

    private static final class MemoryStore implements TimingDataStore {
        @Override
        public LoadResult load() {
            return new LoadResult(Collections.<TimingData>emptyList(), false);
        }

        @Override
        public void append(TimingData data) {
        }
    }

    private static final class TestClient extends WebSocketClient {
        private final LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<>();

        private TestClient(URI uri) {
            super(uri);
        }

        private String awaitMessage() throws InterruptedException {
            return messages.poll(2, TimeUnit.SECONDS);
        }

        private String pollMessage(long timeoutMillis) throws InterruptedException {
            return messages.poll(timeoutMillis, TimeUnit.MILLISECONDS);
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
