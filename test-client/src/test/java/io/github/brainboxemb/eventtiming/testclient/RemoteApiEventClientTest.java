package io.github.brainboxemb.eventtiming.testclient;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RemoteApiEventClientTest {
    @Test
    void parsesCompleteIf03StatusEvent() throws Exception {
        String json = "{"
                + "\"apiVersion\":\"1\","
                + "\"eventType\":\"STATUS_SNAPSHOT\","
                + "\"occurredAt\":\"2026-09-25T15:00:00Z\","
                + "\"payload\":{"
                + "\"apiVersion\":\"1\","
                + "\"build\":{"
                + "\"application\":\"event-timing-app\","
                + "\"version\":\"0.2.2-SNAPSHOT\","
                + "\"revision\":\"abc123\","
                + "\"sourceRef\":\"feature/test\","
                + "\"buildOrigin\":\"local\","
                + "\"dirty\":false,"
                + "\"apiVersion\":\"1\""
                + "},"
                + "\"timingNodes\":[{"
                + "\"timingNodeId\":\"timing-node-01\","
                + "\"lifecycle\":\"CLOSED\""
                + "}],"
                + "\"problems\":[]"
                + "}"
                + "}";

        var event = RemoteApiEventClient.parseEvent(json);

        assertEquals("STATUS_SNAPSHOT", event.eventType());
        assertEquals(Instant.parse("2026-09-25T15:00:00Z"), event.occurredAt());
        assertEquals("event-timing-app", event.status().build().application());
        assertEquals("timing-node-01", event.status().timingNodes().get(0).timingNodeId());
        assertEquals("CLOSED", event.status().timingNodes().get(0).lifecycle());
        assertEquals(json, event.rawJson());
    }

    @Test
    void rejectsNonWebSocketEndpoint() {
        RemoteApiEventClient client = new RemoteApiEventClient();
        try {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> client.connect(
                            java.net.URI.create("http://127.0.0.1:8082/api/v1/events"),
                            new NoOpListener()));
        } finally {
            client.close();
        }
    }

    private static final class NoOpListener implements RemoteApiEventClient.Listener {
        @Override
        public void onConnected() {
        }

        @Override
        public void onEvent(RemoteApiEventClient.StatusEvent event) {
        }

        @Override
        public void onClosed(int statusCode, String reason) {
        }

        @Override
        public void onError(String message) {
        }
    }
}
