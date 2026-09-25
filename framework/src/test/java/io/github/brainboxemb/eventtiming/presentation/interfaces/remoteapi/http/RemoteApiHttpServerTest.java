package io.github.brainboxemb.eventtiming.presentation.interfaces.remoteapi.http;

import io.github.brainboxemb.eventtiming.application.ApplicationStatus;
import io.github.brainboxemb.eventtiming.application.CommandHandler;
import io.github.brainboxemb.eventtiming.domain.timing.TimingNode;
import io.github.brainboxemb.eventtiming.domain.timing.TimingNodeId;
import io.github.brainboxemb.eventtiming.infra.BuildIdentity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RemoteApiHttpServerTest {
    @Test
    public void exposesVersionAndStatusOverRealHttp() throws Exception {
        RemoteApiHttpServer server = new RemoteApiHttpServer("127.0.0.1", 0, commandHandler());
        server.start();

        try {
            Response version = request(server.boundPort(), "GET", "/api/v1/version");
            assertEquals(200, version.status);
            assertTrue(version.contentType.startsWith("application/json"));
            assertTrue(version.body.contains("\"application\":\"event-timing-app\""));
            assertTrue(version.body.contains("\"version\":\"test-version\""));
            assertTrue(version.body.contains("\"sourceRef\":\"feature/test\""));
            assertTrue(version.body.contains("\"dirty\":false"));
            assertTrue(version.body.contains("\"apiVersion\":\"1\""));

            Response status = request(server.boundPort(), "GET", "/api/v1/status");
            assertEquals(200, status.status);
            assertTrue(status.body.contains("\"timingNodeId\":\"timing-node-01\""));
            assertTrue(status.body.contains("\"lifecycle\":\"CLOSED\""));
            assertTrue(status.body.contains("\"problems\":[]"));
        } finally {
            server.close();
        }
    }

    @Test
    public void returnsJsonErrorsForUnknownPathAndUnsupportedMethod() throws Exception {
        RemoteApiHttpServer server = new RemoteApiHttpServer("127.0.0.1", 0, commandHandler());
        server.start();

        try {
            Response missing = request(server.boundPort(), "GET", "/api/v1/missing");
            assertEquals(404, missing.status);
            assertTrue(missing.body.contains("\"code\":\"NOT_FOUND\""));

            Response method = request(server.boundPort(), "POST", "/api/v1/status");
            assertEquals(405, method.status);
            assertTrue(method.body.contains("\"code\":\"METHOD_NOT_ALLOWED\""));
        } finally {
            server.close();
        }
    }

    @Test
    public void returnsJson500ForUnexpectedApplicationFailure() throws Exception {
        BuildIdentity identity = BuildIdentity.firstApiVersion(
                "event-timing-app",
                "test-version",
                "abc123def456",
                "feature/test",
                "local",
                false);
        CommandHandler failing =
                new CommandHandler(identity, () -> {
                    throw new IllegalStateException("test failure");
                });
        RemoteApiHttpServer server = new RemoteApiHttpServer("127.0.0.1", 0, failing);
        server.start();

        try {
            Response response = request(server.boundPort(), "GET", "/api/v1/status");
            assertEquals(500, response.status);
            assertTrue(response.body.contains("\"code\":\"INTERNAL_ERROR\""));
            assertTrue(response.body.contains("\"apiVersion\":\"1\""));
        } finally {
            server.close();
        }
    }

    private static CommandHandler commandHandler() {
        BuildIdentity identity = BuildIdentity.firstApiVersion(
                "event-timing-app",
                "test-version",
                "abc123def456",
                "feature/test",
                "local",
                false);
        ApplicationStatus status = new ApplicationStatus(
                new TimingNodeId("timing-node-01"),
                TimingNode.Lifecycle.CLOSED);
        return new CommandHandler(identity, () -> status);
    }

    private static Response request(int port, String method, String path) throws Exception {
        HttpURLConnection connection =
                (HttpURLConnection) new URL("http://127.0.0.1:" + port + path).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(1000);
        connection.setReadTimeout(2000);

        int status = connection.getResponseCode();
        InputStream stream =
                status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String body = readAll(stream);
        String contentType = connection.getHeaderField("Content-Type");
        connection.disconnect();
        return new Response(status, contentType, body);
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
