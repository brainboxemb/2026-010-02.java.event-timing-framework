package io.github.brainboxemb.eventtiming.systemtest;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * VC-ST1-002 black-box verification of the first committed registration flow.
 *
 * <p>The test starts the packaged application in a separate JVM and imports no
 * application/core classes. HTTP, WebSocket and Remote Shell are the only test
 * boundaries.</p>
 */
public class FirstRegistrationBlackBoxTest {
    private static final String LOOPBACK = "127.0.0.1";
    private static final String NODE_ID = "timing-node-blackbox";
    private static final long START_TIMEOUT_MILLIS = 15000L;
    private static final long EXIT_TIMEOUT_MILLIS = 10000L;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void controlsCommitsRecoversAndDoesNotReplayHistoryAsLive() throws Exception {
        File appJar = new File(requireProperty("eventTiming.appJar")).getAbsoluteFile();
        assertTrue("Packaged application JAR does not exist: " + appJar, appJar.isFile());

        int[] ports = reservePorts(3);
        int shellPort = ports[0];
        int httpPort = ports[1];
        int webSocketPort = ports[2];

        File workDirectory = temporaryFolder.newFolder("vc-st1-002");
        File configFile = new File(workDirectory, "application.yml");
        Files.write(
                configFile.toPath(),
                configuration(shellPort, httpPort, webSocketPort)
                        .getBytes(StandardCharsets.UTF_8));

        Process process = new ProcessBuilder(
                javaExecutable(),
                "-jar",
                appJar.getAbsolutePath(),
                configFile.getAbsolutePath())
                .directory(workDirectory)
                .redirectErrorStream(true)
                .start();

        OutputCollector collector = new OutputCollector(process.getInputStream());
        Thread collectorThread = new Thread(collector, "vc-st1-002-process-output");
        collectorThread.setDaemon(true);
        collectorThread.start();

        EventStream events = null;
        EventStream reconnect = null;
        try {
            awaitHttpReady(process, httpPort, collector);

            events = EventStream.connect(webSocketPort);
            String initialSnapshot = events.awaitEvent("STATUS_SNAPSHOT");
            assertContains(initialSnapshot, "\"id\":\"" + NODE_ID + "\"");
            assertContains(initialSnapshot, "\"locationId\":null");
            assertContains(initialSnapshot, "\"state\":\"CLOSED\"");

            Response capabilities = request(
                    httpPort,
                    "GET",
                    "/api/v1/capabilities",
                    null);
            assertEquals("Unexpected capabilities status", 200, capabilities.status);
            assertContains(
                    capabilities.body,
                    "\"id\":\"DIRECT_REGISTRATION_SIMULATION\"");
            assertContains(capabilities.body, "\"supported\":true");
            assertContains(capabilities.body, "\"enabled\":true");

            Response initialStatus = request(
                    httpPort,
                    "GET",
                    "/api/v1/status",
                    null);
            assertEquals("Unexpected initial status", 200, initialStatus.status);
            assertNodeState(initialStatus.body, null, "CLOSED");

            Response location = request(
                    httpPort,
                    "PUT",
                    nodePath("/location"),
                    "{\"locationId\":24}");
            assertEquals("Unexpected set-location status", 200, location.status);
            assertContains(location.body, "\"result\":\"UPDATED\"");
            String locatedEvent = events.awaitEvent("STATUS_CHANGED");
            assertContains(locatedEvent, "\"locationId\":24");
            assertContains(locatedEvent, "\"state\":\"CLOSED\"");

            Response open = request(
                    httpPort,
                    "POST",
                    nodePath("/open"),
                    "");
            assertEquals("Unexpected open status", 200, open.status);
            assertContains(open.body, "\"result\":\"OPENED\"");
            String openedEvent = events.awaitEvent("STATUS_CHANGED");
            assertContains(openedEvent, "\"locationId\":24");
            assertContains(openedEvent, "\"state\":\"OPEN\"");

            Response rejectedLocation = request(
                    httpPort,
                    "PUT",
                    nodePath("/location"),
                    "{\"locationId\":25}");
            assertEquals(
                    "Expected location change while OPEN to conflict",
                    409,
                    rejectedLocation.status);
            assertContains(
                    rejectedLocation.body,
                    "\"code\":\"NODE_NOT_CLOSED\"");

            Response registration = request(
                    httpPort,
                    "POST",
                    "/api/v1/dev/node/" + NODE_ID + "/auto-reg",
                    "{"
                            + "\"id\":\"N001\","
                            + "\"time\":\"2026-10-01T12:00:00.000000000Z\""
                            + "}");
            assertEquals("Unexpected auto-reg status", 200, registration.status);
            assertContains(registration.body, "\"seq\":1");

            String committedEvent = events.awaitEvent("TIMING_DATA_COMMITTED");
            assertCommittedRegistration(committedEvent);

            Response info = request(
                    httpPort,
                    "GET",
                    nodePath("/logbook"),
                    null);
            assertEquals("Unexpected LogBook metadata status", 200, info.status);
            assertContains(info.body, "\"count\":1");
            assertContains(info.body, "\"first\":1");
            assertContains(info.body, "\"last\":1");

            Response page = request(
                    httpPort,
                    "GET",
                    nodePath("/logbook?from=1&limit=100"),
                    null);
            assertEquals("Unexpected LogBook range status", 200, page.status);
            assertContains(page.body, "\"count\":1");
            assertContains(page.body, "\"next\":null");
            assertCommittedRegistration(page.body);

            Response close = request(
                    httpPort,
                    "POST",
                    nodePath("/close"),
                    "");
            assertEquals("Unexpected close status", 200, close.status);
            assertContains(close.body, "\"result\":\"CLOSED\"");
            String closedEvent = events.awaitEvent("STATUS_CHANGED");
            assertContains(closedEvent, "\"state\":\"CLOSED\"");
            assertContains(closedEvent, "\"locationId\":24");

            events.closeBlocking();
            events = null;

            reconnect = EventStream.connect(webSocketPort);
            String reconnectSnapshot = reconnect.awaitEvent("STATUS_SNAPSHOT");
            assertContains(reconnectSnapshot, "\"id\":\"" + NODE_ID + "\"");
            assertContains(reconnectSnapshot, "\"state\":\"CLOSED\"");
            assertContains(reconnectSnapshot, "\"locationId\":24");
            reconnect.assertNoEvent("TIMING_DATA_COMMITTED", 750L);

            Response rebuiltInfo = request(
                    httpPort,
                    "GET",
                    nodePath("/logbook"),
                    null);
            assertEquals("Unexpected rebuilt LogBook metadata status", 200, rebuiltInfo.status);
            assertContains(rebuiltInfo.body, "\"count\":1");

            Response rebuiltPage = request(
                    httpPort,
                    "GET",
                    nodePath("/logbook?from=1&limit=100"),
                    null);
            assertEquals("Unexpected rebuilt LogBook range status", 200, rebuiltPage.status);
            assertCommittedRegistration(rebuiltPage.body);

            requestControlledShutdown(shellPort);
            assertTrue(
                    "Application did not exit after remote-shell quit. Output:\n"
                            + collector.snapshot(),
                    process.waitFor(EXIT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
            collectorThread.join(1000L);
            assertEquals(
                    "Application exited unsuccessfully. Output:\n"
                            + collector.snapshot(),
                    0,
                    process.exitValue());
        } catch (Throwable failure) {
            throw new AssertionError(
                    "VC-ST1-002 black-box verification failed. Process output:\n"
                            + collector.snapshot(),
                    failure);
        } finally {
            if (events != null) {
                events.closeQuietly();
            }
            if (reconnect != null) {
                reconnect.closeQuietly();
            }
            if (process.isAlive()) {
                process.destroy();
                if (!process.waitFor(2000L, TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(2000L, TimeUnit.MILLISECONDS);
                }
            }
            collectorThread.join(1000L);
        }
    }

    private static String nodePath(String suffix) {
        return "/api/v1/node/" + NODE_ID + suffix;
    }

    private static void assertNodeState(
            String json,
            Integer locationId,
            String state) {
        assertContains(json, "\"nodes\":[{");
        assertContains(json, "\"id\":\"" + NODE_ID + "\"");
        if (locationId == null) {
            assertContains(json, "\"locationId\":null");
        } else {
            assertContains(json, "\"locationId\":" + locationId);
        }
        assertContains(json, "\"state\":\"" + state + "\"");
    }

    private static void assertCommittedRegistration(String json) {
        assertContains(json, "\"timingNodeId\":\"" + NODE_ID + "\"");
        assertContains(json, "\"sequenceNumber\":1");
        assertContains(json, "\"locationId\":24");
        assertContains(json, "\"recordType\":\"REGISTRATION\"");
        assertContains(
                json,
                "\"effectiveTime\":\"2026-10-01T12:00:00.000000000Z\"");
        assertContains(json, "\"registrationId\":\"N001\"");
        assertContains(json, "\"origin\":\"AUTOMATIC\"");
        assertContains(json, "\"timeSource\":\"OBSERVED\"");
    }

    private static void awaitHttpReady(
            Process process,
            int httpPort,
            OutputCollector collector)
            throws Exception {
        long deadline = System.currentTimeMillis() + START_TIMEOUT_MILLIS;
        IOException lastFailure = null;
        while (System.currentTimeMillis() < deadline) {
            if (!process.isAlive()) {
                fail(
                        "Application exited before IF-03 became ready. Output:\n"
                                + collector.snapshot());
            }
            try {
                Response response = request(
                        httpPort,
                        "GET",
                        "/api/v1/status",
                        null);
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

    private static Response request(
            int port,
            String method,
            String path,
            String json)
            throws IOException {
        HttpURLConnection connection =
                (HttpURLConnection) new URL(
                        "http://" + LOOPBACK + ":" + port + path)
                        .openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(1000);
        connection.setReadTimeout(3000);
        connection.setRequestProperty("Accept", "application/json");

        if (json != null) {
            byte[] payload = json.getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8");
            connection.setFixedLengthStreamingMode(payload.length);
            OutputStream output = connection.getOutputStream();
            try {
                output.write(payload);
            } finally {
                output.close();
            }
        }

        try {
            int status = connection.getResponseCode();
            InputStream input = status >= 400
                    ? connection.getErrorStream()
                    : connection.getInputStream();
            return new Response(
                    status,
                    input == null ? "" : readFully(input));
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
                socket.connect(
                        new InetSocketAddress(LOOPBACK, shellPort),
                        500);
                socket.setSoTimeout(2000);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream(),
                                StandardCharsets.UTF_8));
                String ready = reader.readLine();
                if (ready == null || !ready.contains("Remote terminal ready")) {
                    throw new IOException(
                            "Unexpected remote terminal greeting: " + ready);
                }
                PrintWriter writer =
                        new PrintWriter(socket.getOutputStream(), true);
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
        throw new IOException(
                "Unable to connect to remote terminal for controlled shutdown",
                lastFailure);
    }

    private static String configuration(
            int shellPort,
            int httpPort,
            int webSocketPort) {
        return "timingNodeId: " + NODE_ID + "\n"
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
                sockets[i] = new ServerSocket(
                        0,
                        50,
                        InetAddress.getByName(LOOPBACK));
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
        return new File(
                new File(System.getProperty("java.home"), "bin"),
                executable)
                .getAbsolutePath();
    }

    private static String requireProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Missing required system property: " + name);
        }
        return value;
    }

    private static void assertContains(String actual, String expected) {
        assertTrue(
                "Expected <" + expected + "> in <" + actual + ">",
                actual.contains(expected));
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

    private static final class EventStream extends WebSocketClient {
        private final LinkedBlockingQueue<String> messages =
                new LinkedBlockingQueue<String>();
        private final AtomicReference<Exception> failure =
                new AtomicReference<Exception>();

        private EventStream(URI uri) {
            super(uri);
        }

        private static EventStream connect(int port) throws Exception {
            EventStream stream = new EventStream(
                    URI.create(
                            "ws://" + LOOPBACK + ":" + port + "/api/v1/events"));
            assertTrue(
                    "Timed out opening IF-03 WebSocket",
                    stream.connectBlocking(5L, TimeUnit.SECONDS));
            if (stream.failure.get() != null) {
                throw stream.failure.get();
            }
            return stream;
        }

        private String awaitEvent(String eventType) throws Exception {
            long deadline = System.currentTimeMillis() + 5000L;
            while (System.currentTimeMillis() < deadline) {
                if (failure.get() != null) {
                    throw failure.get();
                }
                long remaining = deadline - System.currentTimeMillis();
                String message = messages.poll(
                        Math.max(1L, remaining),
                        TimeUnit.MILLISECONDS);
                if (message == null) {
                    break;
                }
                if (message.contains(
                        "\"eventType\":\"" + eventType + "\"")) {
                    return message;
                }
            }
            fail("Timed out waiting for IF-03 event " + eventType);
            return null;
        }

        private void assertNoEvent(String eventType, long timeoutMillis)
                throws Exception {
            long deadline = System.currentTimeMillis() + timeoutMillis;
            while (System.currentTimeMillis() < deadline) {
                if (failure.get() != null) {
                    throw failure.get();
                }
                long remaining = deadline - System.currentTimeMillis();
                String message = messages.poll(
                        Math.max(1L, remaining),
                        TimeUnit.MILLISECONDS);
                if (message == null) {
                    return;
                }
                if (message.contains(
                        "\"eventType\":\"" + eventType + "\"")) {
                    fail(
                            "Unexpected replayed IF-03 event "
                                    + eventType
                                    + ": "
                                    + message);
                }
            }
        }

        private void closeQuietly() {
            try {
                closeBlocking();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
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
        public void onClose(
                int code,
                String reason,
                boolean remote) {
        }

        @Override
        public void onError(Exception ex) {
            failure.compareAndSet(null, ex);
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
                        new InputStreamReader(
                                input,
                                StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (text) {
                        text.append(line)
                                .append(System.lineSeparator());
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
