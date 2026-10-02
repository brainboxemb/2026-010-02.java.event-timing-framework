package io.github.brainboxemb.eventtiming.systemtest;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * VC-ST1-001 black-box verification.
 *
 * <p>This test deliberately imports no application/core classes. It starts the packaged
 * application in a separate JVM and uses only externally observable interfaces.</p>
 */
public class FirstExecutableBlackBoxTest {
    private static final String LOOPBACK = "127.0.0.1";
    private static final long START_TIMEOUT_MILLIS = 15000L;
    private static final long EXIT_TIMEOUT_MILLIS = 10000L;

    @Test
    public void verifiesVersionStatusReconnectAndControlledShutdown() throws Exception {
        File appJar = new File(requireProperty("eventTiming.appJar")).getAbsoluteFile();
        assertTrue("Packaged application JAR does not exist: " + appJar, appJar.isFile());

        String expectedVersion = requireProperty("eventTiming.expectedProjectVersion");
        int[] ports = reservePorts(3);
        int shellPort = ports[0];
        int httpPort = ports[1];
        int webSocketPort = ports[2];

        BlackBoxEvidence evidence = BlackBoxEvidence.create(
                requireProperty("eventTiming.evidenceDir"),
                "VC-ST1-001");
        File workDirectory = evidence.directory();
        File configFile = evidence.file("application.yml");
        Files.write(
                configFile.toPath(),
                configuration(shellPort, httpPort, webSocketPort).getBytes(StandardCharsets.UTF_8));

        Process process = new ProcessBuilder(
                javaExecutable(),
                "-jar",
                appJar.getAbsolutePath(),
                configFile.getAbsolutePath())
                .directory(workDirectory)
                .redirectErrorStream(true)
                .start();

        OutputCollector collector = new OutputCollector(process.getInputStream());
        Thread collectorThread = new Thread(collector, "vc-st1-001-process-output");
        collectorThread.setDaemon(true);
        collectorThread.start();

        boolean passed = false;
        Throwable evidenceFailure = null;
        try {
            awaitHttpReady(process, httpPort, collector);

            Response version = get(httpPort, "/api/v1/version");
            assertEquals("Unexpected /version HTTP status", 200, version.status);
            assertContains(version.body, "\"application\":\"event-timing-app\"");
            assertContains(version.body, "\"version\":\"" + expectedVersion + "\"");
            assertContains(version.body, "\"apiVersion\":\"1\"");
            assertTrue(
                    "Expected exact Git revision in /version: " + version.body,
                    version.body.matches(".*\\\"revision\\\":\\\"[0-9a-f]{40}\\\".*"));

            Response status = get(httpPort, "/api/v1/status");
            assertEquals("Unexpected /status HTTP status", 200, status.status);
            assertStatusSemantics(status.body);

            String firstSnapshot = receiveStatusSnapshot(webSocketPort);
            assertSnapshot(firstSnapshot);

            String reconnectSnapshot = receiveStatusSnapshot(webSocketPort);
            assertSnapshot(reconnectSnapshot);

            Response resynchronisedStatus = get(httpPort, "/api/v1/status");
            assertEquals("Unexpected resynchronisation /status HTTP status", 200, resynchronisedStatus.status);
            assertStatusSemantics(resynchronisedStatus.body);
            assertSnapshotSemanticsMatchStatus(reconnectSnapshot, resynchronisedStatus.body);

            requestControlledShutdown(shellPort);
            assertTrue(
                    "Application did not exit after remote-shell quit. Output:\n" + collector.snapshot(),
                    process.waitFor(EXIT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
            collectorThread.join(1000L);
            assertEquals(
                    "Application exited unsuccessfully. Output:\n" + collector.snapshot(),
                    0,
                    process.exitValue());
            passed = true;
        } catch (Throwable failure) {
            evidenceFailure = failure;
            throw new AssertionError(
                    "VC-ST1-001 black-box verification failed. Process output:\n"
                            + collector.snapshot(),
                    failure);
        } finally {
            if (process.isAlive()) {
                process.destroy();
                if (!process.waitFor(2000L, TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(2000L, TimeUnit.MILLISECONDS);
                }
            }
            collectorThread.join(1000L);
            evidence.writeProcessOutput(collector.snapshot());
            evidence.writeResult(passed, evidenceFailure);
        }
    }

    private static void assertSnapshot(String json) {
        assertContains(json, "\"eventType\":\"STATUS_SNAPSHOT\"");
        assertContains(json, "\"nodes\":[{");
        assertContains(json, "\"id\":\"timing-node-blackbox\"");
        assertContains(json, "\"locationId\":null");
        assertContains(json, "\"state\":\"CLOSED\"");
    }

    private static void assertStatusSemantics(String json) {
        assertContains(json, "\"nodes\":[{");
        assertContains(json, "\"id\":\"timing-node-blackbox\"");
        assertContains(json, "\"locationId\":null");
        assertContains(json, "\"state\":\"CLOSED\"");
        assertContains(json, "\"problems\":[]");
    }

    private static void assertSnapshotSemanticsMatchStatus(
            String snapshot,
            String status) {
        for (String field : new String[] {
                "\"id\":\"timing-node-blackbox\"",
                "\"locationId\":null",
                "\"state\":\"CLOSED\""
        }) {
            assertContains(snapshot, field);
            assertContains(status, field);
        }
    }

    private static String receiveStatusSnapshot(int port) throws Exception {
        final CountDownLatch opened = new CountDownLatch(1);
        final CountDownLatch messageReceived = new CountDownLatch(1);
        final CountDownLatch closed = new CountDownLatch(1);
        final AtomicReference<String> message = new AtomicReference<String>();
        final AtomicReference<Exception> failure = new AtomicReference<Exception>();

        WebSocketClient client = new WebSocketClient(
                URI.create("ws://" + LOOPBACK + ":" + port + "/api/v1/events")) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                opened.countDown();
            }

            @Override
            public void onMessage(String value) {
                if (message.compareAndSet(null, value)) {
                    messageReceived.countDown();
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                closed.countDown();
            }

            @Override
            public void onError(Exception ex) {
                failure.compareAndSet(null, ex);
                opened.countDown();
                messageReceived.countDown();
            }
        };

        try {
            client.connect();
            assertTrue("Timed out opening IF-03 WebSocket", opened.await(5L, TimeUnit.SECONDS));
            if (failure.get() != null) {
                throw failure.get();
            }
            assertTrue(
                    "Timed out waiting for STATUS_SNAPSHOT",
                    messageReceived.await(5L, TimeUnit.SECONDS));
            if (failure.get() != null) {
                throw failure.get();
            }
            String value = message.get();
            if (value == null) {
                fail("WebSocket closed without STATUS_SNAPSHOT");
            }
            return value;
        } finally {
            client.close();
            closed.await(2L, TimeUnit.SECONDS);
        }
    }

    private static void awaitHttpReady(
            Process process,
            int httpPort,
            OutputCollector collector) throws Exception {
        long deadline = System.currentTimeMillis() + START_TIMEOUT_MILLIS;
        IOException lastFailure = null;
        while (System.currentTimeMillis() < deadline) {
            if (!process.isAlive()) {
                fail("Application exited before IF-03 became ready. Output:\n" + collector.snapshot());
            }
            try {
                Response response = get(httpPort, "/api/v1/status");
                if (response.status == 200) {
                    return;
                }
            } catch (IOException ex) {
                lastFailure = ex;
            }
            Thread.sleep(100L);
        }
        throw new IOException("Timed out waiting for IF-03 HTTP endpoint", lastFailure);
    }

    private static Response get(int port, String path) throws IOException {
        HttpURLConnection connection =
                (HttpURLConnection) new URL("http://" + LOOPBACK + ":" + port + path)
                        .openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(1000);
        connection.setReadTimeout(2000);
        try {
            int status = connection.getResponseCode();
            InputStream input = status >= 400
                    ? connection.getErrorStream()
                    : connection.getInputStream();
            return new Response(status, input == null ? "" : readFully(input));
        } finally {
            connection.disconnect();
        }
    }

    private static void requestControlledShutdown(int shellPort) throws Exception {
        long deadline = System.currentTimeMillis() + 5000L;
        IOException lastFailure = null;
        while (System.currentTimeMillis() < deadline) {
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(LOOPBACK, shellPort), 500);
                socket.setSoTimeout(2000);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                String ready = reader.readLine();
                if (ready == null || !ready.contains("Remote terminal ready")) {
                    throw new IOException("Unexpected remote terminal greeting: " + ready);
                }
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                writer.println("quit");
                return;
            } catch (IOException ex) {
                lastFailure = ex;
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {
                    // Best-effort test cleanup.
                }
            }
            Thread.sleep(100L);
        }
        throw new IOException("Unable to connect to remote terminal for controlled shutdown", lastFailure);
    }

    private static String configuration(int shellPort, int httpPort, int webSocketPort) {
        return "timingNodeId: timing-node-blackbox\n"
                + "io:\n"
                + "  storage:\n"
                + "    timingData:\n"
                + "      path: timing-data.jsonl\n"
                + "presentation:\n"
                + "  remoteShell:\n"
                + "    bindAddress: 127.0.0.1\n"
                + "    port: " + shellPort + "\n"
                + "  api:\n"
                + "    http:\n"
                + "      bindAddress: 127.0.0.1\n"
                + "      port: " + httpPort + "\n"
                + "    webSocket:\n"
                + "      bindAddress: 127.0.0.1\n"
                + "      port: " + webSocketPort + "\n";
    }

    private static int[] reservePorts(int count) throws IOException {
        ServerSocket[] sockets = new ServerSocket[count];
        int[] ports = new int[count];
        try {
            for (int i = 0; i < count; i++) {
                sockets[i] = new ServerSocket(0, 50, InetAddress.getByName(LOOPBACK));
                ports[i] = sockets[i].getLocalPort();
            }
            return ports;
        } finally {
            for (ServerSocket socket : sockets) {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                        // Best-effort reservation cleanup.
                    }
                }
            }
        }
    }

    private static String javaExecutable() {
        String executable = System.getProperty("os.name", "")
                .toLowerCase()
                .contains("win") ? "java.exe" : "java";
        return new File(new File(System.getProperty("java.home"), "bin"), executable)
                .getAbsolutePath();
    }

    private static String requireProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing required system property: " + name);
        }
        return value;
    }

    private static void assertContains(String actual, String expected) {
        assertTrue("Expected <" + expected + "> in <" + actual + ">", actual.contains(expected));
    }

    private static String readFully(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private static final class Response {
        private final int status;
        private final String body;

        private Response(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    private static final class OutputCollector implements Runnable {
        private final InputStream input;
        private final StringBuilder text = new StringBuilder();

        private OutputCollector(InputStream input) {
            this.input = input;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(input, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (text) {
                        text.append(line).append(System.lineSeparator());
                    }
                }
            } catch (IOException ex) {
                synchronized (text) {
                    text.append("[output collector failed: ")
                            .append(ex.getMessage())
                            .append(']')
                            .append(System.lineSeparator());
                }
            }
        }

        private String snapshot() {
            synchronized (text) {
                return text.toString();
            }
        }
    }
}
