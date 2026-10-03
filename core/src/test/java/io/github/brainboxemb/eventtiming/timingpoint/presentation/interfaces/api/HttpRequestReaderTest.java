package io.github.brainboxemb.eventtiming.timingpoint.presentation.interfaces.api;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class HttpRequestReaderTest {
    private final HttpRequestReader reader = new HttpRequestReader();

    @Test
    public void parsesNodeRouteAndSupportedLogBookQueries() {
        HttpRequestReader.NodeRoute route =
                reader.nodeRoute("timing-node-01/logbook");
        assertEquals("timing-node-01", route.nodeId);
        assertEquals("/logbook", route.resource);
        assertNull(reader.nodeRoute("timing-node-01"));
        assertNull(reader.nodeRoute("timing-node-01/"));

        HttpRequestReader.LogBookQuery range =
                reader.readLogBookQuery("from=2&limit=100");
        assertEquals(Long.valueOf(2L), range.from);
        assertEquals(Integer.valueOf(100), range.limit);
        assertNull(range.last);

        HttpRequestReader.LogBookQuery latest =
                reader.readLogBookQuery("last=10");
        assertNull(latest.from);
        assertNull(latest.limit);
        assertEquals(Integer.valueOf(10), latest.last);
    }

    @Test
    public void parsesSupportedJsonRequestBodies() {
        assertEquals(
                24,
                reader.parseLocationIdBody(bytes("{\"locationId\":24}")));

        HttpRequestReader.AutoRegistrationRequest request =
                reader.parseAutoRegistrationBody(bytes(
                        "{"
                                + "\"id\":\"N0001\","
                                + "\"time\":\"2026-10-01T12:00:00.000000000Z\""
                                + "}"));
        assertEquals("N0001", request.id);
        assertEquals("2026-10-01T12:00:00.000000000Z", request.time);
    }

    @Test
    public void rejectsInvalidRequestShapesWithStableCodes() {
        assertFailure(
                "INVALID_VALUE",
                "last cannot be combined",
                () -> reader.readLogBookQuery("last=1&limit=1"));
        assertFailure(
                "INVALID_VALUE",
                "limit must be <= 1000",
                () -> reader.readLogBookQuery("from=1&limit=1001"));
        assertFailure(
                "INVALID_VALUE",
                "Unsupported request field",
                () -> reader.parseLocationIdBody(bytes(
                        "{\"locationId\":24,\"extra\":1}")));
        assertFailure(
                "MALFORMED_REQUEST",
                "Malformed JSON request",
                () -> reader.parseAutoRegistrationBody(bytes("{not-json}")));
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static void assertFailure(
            String code,
            String messagePart,
            Runnable operation) {
        try {
            operation.run();
            fail("Expected request parsing failure");
        } catch (HttpRequestReader.RequestException ex) {
            assertEquals(code, ex.code());
            if (!ex.getMessage().contains(messagePart)) {
                fail("Unexpected message: " + ex.getMessage());
            }
        }
    }
}
