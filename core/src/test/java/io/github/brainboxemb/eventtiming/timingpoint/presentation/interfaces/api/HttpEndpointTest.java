package io.github.brainboxemb.eventtiming.timingpoint.presentation.interfaces.api;

import io.github.brainboxemb.eventtiming.timingdata.TimingData;
import io.github.brainboxemb.eventtiming.timingdata.TimingNodeId;
import io.github.brainboxemb.eventtiming.timingdata.TimingTimestamp;
import io.github.brainboxemb.eventtiming.timingdata.defaultprofile.DefaultTimingDataFactory;
import io.github.brainboxemb.eventtiming.timingpoint.application.ApplicationStatus;
import io.github.brainboxemb.eventtiming.timingpoint.application.CommandHandler;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNode;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timingdata.TimingDataPersistence;
import io.github.brainboxemb.eventtiming.timingpoint.infra.BuildIdentity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HttpEndpointTest {
    private static final TimingTimestamp RECORDED_AT =
            TimingTimestamp.parse("2026-10-01T12:00:01.000000000Z");

    @Test
    public void exposesVersionAndStatusOverRealHttp() throws Exception {
        HttpEndpoint server = new HttpEndpoint("127.0.0.1", 0, statusOnlyHandler());
        server.start();

        try {
            Response version = request(server.boundPort(), "GET", "/api/v1/version", null);
            assertEquals(200, version.status);
            assertTrue(version.contentType.startsWith("application/json"));
            assertTrue(version.body.contains("\"application\":\"event-timing-app\""));
            assertTrue(version.body.contains("\"version\":\"test-version\""));
            assertTrue(version.body.contains("\"sourceRef\":\"feature/test\""));
            assertTrue(version.body.contains("\"dirty\":false"));
            assertTrue(version.body.contains("\"apiVersion\":\"1\""));

            Response status = request(server.boundPort(), "GET", "/api/v1/status", null);
            assertEquals(200, status.status);
            assertTrue(status.body.contains("\"nodes\":[{"));
            assertTrue(status.body.contains("\"id\":\"timing-node-01\""));
            assertTrue(status.body.contains("\"locationId\":null"));
            assertTrue(status.body.contains("\"state\":\"CLOSED\""));
            assertTrue(status.body.contains("\"problems\":[]"));
            assertTrue(!status.body.contains("\"build\":"));
            assertTrue(!status.body.contains("\"apiVersion\":"));
        } finally {
            server.close();
        }
    }

    @Test
    public void controlsAndObservesFirstRegistrationThroughPublicHttp() throws Exception {
        Fixture fixture = new Fixture();
        fixture.start();
        HttpEndpoint server = new HttpEndpoint("127.0.0.1", 0, fixture.handler);
        server.start();

        try {
            Response capabilities =
                    request(server.boundPort(), "GET", "/api/v1/capabilities", null);
            assertEquals(200, capabilities.status);
            assertTrue(capabilities.body.contains(
                    "\"id\":\"DIRECT_REGISTRATION_SIMULATION\""));
            assertTrue(capabilities.body.contains("\"supported\":true"));
            assertTrue(capabilities.body.contains("\"enabled\":true"));

            Response openWithoutLocation =
                    request(server.boundPort(), "POST", "/api/v1/node/timing-node-01/open", null);
            assertEquals(409, openWithoutLocation.status);
            assertTrue(openWithoutLocation.body.contains("\"code\":\"NO_LOCATION\""));

            Response setLocation = request(
                    server.boundPort(),
                    "PUT",
                    "/api/v1/node/timing-node-01/location",
                    "{\"locationId\":24}");
            assertEquals(200, setLocation.status);
            assertTrue(setLocation.body.contains("\"result\":\"UPDATED\""));

            Response locatedStatus =
                    request(server.boundPort(), "GET", "/api/v1/status", null);
            assertTrue(locatedStatus.body.contains("\"locationId\":24"));

            Response opened =
                    request(server.boundPort(), "POST", "/api/v1/node/timing-node-01/open", null);
            assertEquals(200, opened.status);
            assertTrue(opened.body.contains("\"result\":\"OPENED\""));

            Response changeWhileOpen = request(
                    server.boundPort(),
                    "PUT",
                    "/api/v1/node/timing-node-01/location",
                    "{\"locationId\":25}");
            assertEquals(409, changeWhileOpen.status);
            assertTrue(changeWhileOpen.body.contains(
                    "\"code\":\"NODE_NOT_CLOSED\""));

            Response registration = request(
                    server.boundPort(),
                    "POST",
                    "/api/v1/dev/node/timing-node-01/auto-reg",
                    "{"
                            + "\"id\":\"N001\","
                            + "\"time\":"
                            + "\"2026-10-01T12:00:00.000000000Z\""
                            + "}");
            assertEquals(200, registration.status);
            assertEquals("{\"seq\":1}", registration.body);

            Response logBookInfo = request(
                    server.boundPort(),
                    "GET",
                    "/api/v1/node/timing-node-01/logbook",
                    null);
            assertEquals(200, logBookInfo.status);
            assertEquals("{\"count\":1,\"first\":1,\"last\":1}", logBookInfo.body);

            Response history = request(
                    server.boundPort(),
                    "GET",
                    "/api/v1/node/timing-node-01/logbook?from=1&limit=100",
                    null);
            assertEquals(200, history.status);
            assertTrue(history.body.contains("\"count\":1"));
            assertTrue(history.body.contains("\"next\":null"));
            assertTrue(history.body.contains("\"sequenceNumber\":1"));
            assertTrue(history.body.contains("\"locationId\":24"));
            assertTrue(history.body.contains("\"registrationId\":\"N001\""));
            assertTrue(history.body.contains("\"origin\":\"AUTOMATIC\""));
            assertTrue(history.body.contains(
                    "\"effectiveTime\":\"2026-10-01T12:00:00.000000000Z\""));

            Response latest = request(
                    server.boundPort(),
                    "GET",
                    "/api/v1/node/timing-node-01/logbook?last=1",
                    null);
            assertEquals(200, latest.status);
            assertTrue(latest.body.contains("\"sequenceNumber\":1"));

            Response wrongNode = request(
                    server.boundPort(),
                    "GET",
                    "/api/v1/node/other-node/logbook",
                    null);
            assertEquals(404, wrongNode.status);
            assertTrue(wrongNode.body.contains("\"code\":\"NODE_NOT_FOUND\""));

            Response closed =
                    request(server.boundPort(), "POST", "/api/v1/node/timing-node-01/close", null);
            assertEquals(200, closed.status);
            assertTrue(closed.body.contains("\"result\":\"CLOSED\""));
        } finally {
            server.close();
            fixture.close();
        }
    }

    @Test
    public void mapsRequestAndMethodFailuresToStableJsonErrors() throws Exception {
        Fixture fixture = new Fixture();
        fixture.start();
        HttpEndpoint server = new HttpEndpoint("127.0.0.1", 0, fixture.handler);
        server.start();

        try {
            Response missing =
                    request(server.boundPort(), "GET", "/api/v1/missing", null);
            assertEquals(404, missing.status);
            assertTrue(missing.body.contains("\"code\":\"NOT_FOUND\""));

            Response method =
                    request(server.boundPort(), "POST", "/api/v1/status", null);
            assertEquals(405, method.status);
            assertTrue(method.body.contains("\"code\":\"METHOD_NOT_ALLOWED\""));
            assertTrue(!method.body.contains("INTERNAL_ERROR"));

            Response malformed = request(
                    server.boundPort(),
                    "PUT",
                    "/api/v1/node/timing-node-01/location",
                    "{not-json}");
            assertEquals(400, malformed.status);
            assertTrue(malformed.body.contains("\"code\":\"MALFORMED_REQUEST\""));

            Response invalid = request(
                    server.boundPort(),
                    "PUT",
                    "/api/v1/node/timing-node-01/location",
                    "{\"locationId\":0}");
            assertEquals(400, invalid.status);
            assertTrue(invalid.body.contains("\"code\":\"INVALID_VALUE\""));
        } finally {
            server.close();
            fixture.close();
        }
    }

    @Test
    public void returnsJson500ForUnexpectedApplicationFailure() throws Exception {
        BuildIdentity identity = identity();
        CommandHandler failing =
                new CommandHandler(identity, () -> {
                    throw new IllegalStateException("test failure");
                });
        HttpEndpoint server = new HttpEndpoint("127.0.0.1", 0, failing);
        server.start();

        try {
            Response response =
                    request(server.boundPort(), "GET", "/api/v1/status", null);
            assertEquals(500, response.status);
            assertTrue(response.body.contains("\"code\":\"INTERNAL_ERROR\""));
            assertTrue(!response.body.contains("\"apiVersion\":"));
        } finally {
            server.close();
        }
    }

    private static CommandHandler statusOnlyHandler() {
        ApplicationStatus status = new ApplicationStatus(
                new TimingNodeId("timing-node-01"),
                TimingNode.Lifecycle.CLOSED);
        return new CommandHandler(identity(), () -> status);
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

    private static Response request(
            int port,
            String method,
            String path,
            String body)
            throws Exception {
        HttpURLConnection connection =
                (HttpURLConnection) new URL("http://127.0.0.1:" + port + path)
                        .openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(1000);
        connection.setReadTimeout(3000);
        connection.setRequestProperty("Accept", "application/json");

        if (body != null) {
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8");
            connection.setFixedLengthStreamingMode(payload.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload);
            }
        }

        int status = connection.getResponseCode();
        InputStream stream =
                status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String responseBody = readAll(stream);
        String contentType = connection.getHeaderField("Content-Type");
        connection.disconnect();
        return new Response(status, contentType, responseBody);
    }

    private static String readAll(InputStream stream) throws Exception {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }
        return result.toString();
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

    private static final class MemoryStore implements TimingDataPersistence {
        @Override
        public LoadResult load() {
            return new LoadResult(Collections.<TimingData>emptyList(), false);
        }

        @Override
        public void append(TimingData data) {
        }
    }

    private static final class Response {
        private final int status;
        private final String contentType;
        private final String body;

        private Response(int status, String contentType, String body) {
            this.status = status;
            this.contentType = contentType;
            this.body = body;
        }
    }
}
