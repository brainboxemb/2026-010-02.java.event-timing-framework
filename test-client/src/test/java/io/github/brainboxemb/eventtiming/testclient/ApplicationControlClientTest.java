package io.github.brainboxemb.eventtiming.testclient;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ApplicationControlClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void readsVersionAndStatusFromPublicHttpContract() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/version", exchange -> respond(exchange,
                "{"
                        + "\"application\":\"event-timing-app\","
                        + "\"version\":\"0.2.2-SNAPSHOT\","
                        + "\"revision\":\"abc123\","
                        + "\"sourceRef\":\"feature/test\","
                        + "\"buildOrigin\":\"local\","
                        + "\"dirty\":false,"
                        + "\"apiVersion\":\"1\""
                        + "}"));
        server.createContext("/api/v1/status", exchange -> respond(exchange,
                "{"
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
                        + "\"application\":{"
                        + "\"state\":\"RUNNING\","
                        + "\"startedAt\":\"2026-09-25T13:00:00Z\""
                        + "},"
                        + "\"timingNodes\":[{"
                        + "\"timingNodeId\":\"timing-node-01\","
                        + "\"lifecycle\":\"CLOSED\""
                        + "}],"
                        + "\"problems\":[]"
                        + "}"));
        server.start();

        ApplicationControlClient client = new ApplicationControlClient(
                URI.create("http://127.0.0.1:" + server.getAddress().getPort()));

        var version = client.getVersion();
        assertEquals("event-timing-app", version.build().application());
        assertEquals("0.2.2-SNAPSHOT", version.build().version());
        assertEquals("feature/test", version.build().sourceRef());
        assertFalse(version.build().dirty());

        var status = client.getStatus();
        assertEquals("RUNNING", status.applicationState());
        assertEquals("timing-node-01", status.timingNodes().get(0).timingNodeId());
        assertEquals("CLOSED", status.timingNodes().get(0).lifecycle());
        assertEquals("abc123", status.build().revision());
    }

    private static void respond(HttpExchange exchange, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }
}
