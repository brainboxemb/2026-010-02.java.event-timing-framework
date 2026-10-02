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
import org.junit.Test;

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
    private static final String NODE_ID = "Test";
    private static final long START_TIMEOUT_MILLIS = 15000L;
    private static final long EXIT_TIMEOUT_MILLIS = 10000L;

    @Test
    public void controlsCommitsReconnectsRestartsAndRecoversLogBook() throws Exception {
        File appJar = new File(requireProperty("eventTiming.appJar")).getAbsoluteFile();
        assertTrue("Packaged application JAR does not exist: " + appJar, appJar.isFile());

        BlackBoxEvidence evidence = BlackBoxEvidence.create(
                requireProperty("eventTiming.evidenceDir"),
                "VC-ST1-002");
        File workDirectory = evidence.directory();

        int[] firstPorts = reservePorts(3);
        int firstShellPort = firstPorts[0];
        int firstHttpPort = firstPorts[1];
        int firstWebSocketPort = firstPorts[2];
        File firstConfig = evidence.file("application-run-1.yml");
        Files.write(
                firstConfig.toPath(),
                configuration(firstShellPort, firstHttpPort, firstWebSocketPort)
                        .getBytes(StandardCharsets.UTF_8));

        ProcessRun firstRun = null;
        ProcessRun secondRun = null;
        EventStream events = null;
        EventStream reconnect = null;
        EventStream recoveredEvents = null;
        boolean passed = false;
        Throwable evidenceFailure = null;
        try {
            firstRun = startApplication(
                    appJar,
                    firstConfig,
                    workDirectory,
                    "vc-st1-002-run-1-output");
            awaitHttpReady(firstRun.process, firstHttpPort, firstRun.collector);

            events = EventStream.connect(firstWebSocketPort);
            String initialSnapshot = events.awaitEvent("STATUS_SNAPSHOT");
            assertContains(initialSnapshot, "\"id\":\"" + NODE_ID + "\"");
            assertContains(initialSnapshot, "\"locationId\":null");
            assertContains(initialSnapshot, "\"state\":\"CLOSED\"");

            Response capabilities = request(
                    firstHttpPort,
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
                    firstHttpPort,
                    "GET",
                    "/api/v1/status",
                    null);
            assertEquals("Unexpected initial status", 200, initialStatus.status);
            assertNodeState(initialStatus.body, null, "CLOSED");

            Response location = request(
                    firstHttpPort,
                    "PUT",
                    nodePath("/location"),
                    "{\"locationId\":24}");
            assertEquals("Unexpected set-location status", 200, location.status);
            assertContains(location.body, "\"result\":\"UPDATED\"");
            String locatedEvent = events.awaitEvent("STATUS_CHANGED");
            assertContains(locatedEvent, "\"locationId\":24");
            assertContains(locatedEvent, "\"state\":\"CLOSED\"");

            Response open = request(
                    firstHttpPort,
                    "POST",
                    nodePath("/open"),
                    "");
            assertEquals("Unexpected open status", 200, open.status);
            assertContains(open.body, "\"result\":\"OPENED\"");
            String openedEvent = events.awaitEvent("STATUS_CHANGED");
            assertContains(openedEvent, "\"locationId\":24");
            assertContains(openedEvent, "\"state\":\"OPEN\"");

            Response rejectedLocation = request(
                    firstHttpPort,
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
                    firstHttpPort,
                    "POST",
                    "/api/v1/dev/node/" + NODE_ID + "/auto-reg",
                    "{"
                            + "\"id\":\"N001\","
                            + "\"time\":\"2026-10-01T12:00:00Z\""
                            + "}");
            assertEquals("Unexpected auto-reg status", 200, registration.status);
            assertContains(registration.body, "\"seq\":1");

            String committedEvent = events.awaitEvent("TIMING_DATA_COMMITTED");
            assertCommittedRegistration(committedEvent);

            Response info = request(
                    firstHttpPort,
                    "GET",
                    nodePath("/logbook"),
                    null);
            assertEquals("Unexpected LogBook metadata status", 200, info.status);
            assertContains(info.body, "\"count\":1");
            assertContains(info.body, "\"first\":1");
            assertContains(info.body, "\"last\":1");

            Response page = request(
                    firstHttpPort,
                    "GET",
                    nodePath("/logbook?from=1&limit=100"),
                    null);
            assertEquals("Unexpected LogBook range status", 200, page.status);
            assertContains(page.body, "\"count\":1");
            assertContains(page.body, "\"next\":null");
            assertCommittedRegistration(page.body);

            Response close = request(
                    firstHttpPort,
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

            reconnect = EventStream.connect(firstWebSocketPort);
            String reconnectSnapshot = reconnect.awaitEvent("STATUS_SNAPSHOT");
            assertContains(reconnectSnapshot, "\"id\":\"" + NODE_ID + "\"");
            assertContains(reconnectSnapshot, "\"state\":\"CLOSED\"");
            assertContains(reconnectSnapshot, "\"locationId\":24");
            reconnect.assertNoEvent("TIMING_DATA_COMMITTED", 750L);

            Response reconnectedInfo = request(
                    firstHttpPort,
                    "GET",
                    nodePath("/logbook"),
                    null);
            assertEquals(
                    "Unexpected LogBook metadata after WebSocket reconnect",
                    200,
                    reconnectedInfo.status);
            assertContains(reconnectedInfo.body, "\"count\":1");

            Response reconnectedPage = request(
                    firstHttpPort,
                    "GET",
                    nodePath("/logbook?from=1&limit=100"),
                    null);
            assertEquals(
                    "Unexpected LogBook range after WebSocket reconnect",
                    200,
                    reconnectedPage.status);
            assertCommittedRegistration(reconnectedPage.body);

            reconnect.closeBlocking();
            reconnect = null;
            assertControlledShutdown(firstRun, firstShellPort);

            File persistedTimingData = evidence.file("timing-data.jsonl");
            assertTrue(
                    "First run did not retain timing-data.jsonl",
                    persistedTimingData.isFile() && persistedTimingData.length() > 0L);

            int[] secondPorts = reservePorts(3);
            int secondShellPort = secondPorts[0];
            int secondHttpPort = secondPorts[1];
            int secondWebSocketPort = secondPorts[2];
            File secondConfig = evidence.file("application-run-2.yml");
            Files.write(
                    secondConfig.toPath(),
                    configuration(secondShellPort, secondHttpPort, secondWebSocketPort)
                            .getBytes(StandardCharsets.UTF_8));

            secondRun = startApplication(
                    appJar,
                    secondConfig,
                    workDirectory,
                    "vc-st1-002-run-2-output");
            awaitHttpReady(secondRun.process, secondHttpPort, secondRun.collector);

            recoveredEvents = EventStream.connect(secondWebSocketPort);
            String recoveredSnapshot = recoveredEvents.awaitEvent("STATUS_SNAPSHOT");
            assertContains(recoveredSnapshot, "\"id\":\"" + NODE_ID + "\"");
            assertContains(recoveredSnapshot, "\"state\":\"CLOSED\"");
            assertContains(recoveredSnapshot, "\"locationId\":null");
            recoveredEvents.assertNoEvent("TIMING_DATA_COMMITTED", 750L);

            Response recoveredStatus = request(
                    secondHttpPort,
                    "GET",
                    "/api/v1/status",
                    null);
            assertEquals("Unexpected status after restart", 200, recoveredStatus.status);
            assertNodeState(recoveredStatus.body, null, "CLOSED");

            Response recoveredInfo = request(
                    secondHttpPort,
                    "GET",
                    nodePath("/logbook"),
                    null);
            assertEquals(
                    "Unexpected recovered LogBook metadata status",
                    200,
                    recoveredInfo.status);
            assertContains(recoveredInfo.body, "\"count\":1");
            assertContains(recoveredInfo.body, "\"first\":1");
            assertContains(recoveredInfo.body, "\"last\":1");

            Response recoveredPage = request(
                    secondHttpPort,
                    "GET",
                    nodePath("/logbook?from=1&limit=100"),
                    null);
            assertEquals(
                    "Unexpected recovered LogBook range status",
                    200,
                    recoveredPage.status);
            assertContains(recoveredPage.body, "\"count\":1");
            assertContains(recoveredPage.body, "\"next\":null");
            assertCommittedRegistration(recoveredPage.body);

            recoveredEvents.closeBlocking();
            recoveredEvents = null;
            assertControlledShutdown(secondRun, secondShellPort);

            evidence.verifyRuntimeLogging(combinedOutput(firstRun, secondRun));
            passed = true;
        } catch (Throwable failure) {
            evidenceFailure = failure;
            throw new AssertionError(
                    "VC-ST1-002 black-box verification failed. Process output:\n"
                            + combinedOutput(firstRun, secondRun),
                    failure);
        } finally {
            if (events != null) {
                events.closeQuietly();
            }
            if (reconnect != null) {
                reconnect.closeQuietly();
            }
            if (recoveredEvents != null) {
                recoveredEvents.closeQuietly();
            }
            cleanupProcess(firstRun);
            cleanupProcess(secondRun);
            evidence.writeProcessOutput(combinedOutput(firstRun, secondRun));
            evidence.writeResult(passed, evidenceFailure);
        }
    }

    private static ProcessRun startApplication(
            File appJar,
            File configFile,
            File workDirectory,
            String collectorName)
            throws IOException {
        Process process = new ProcessBuilder(
                javaExecutable(),
                "-jar",
                appJar.getAbsolutePath(),
                configFile.getAbsolutePath())
                .directory(workDirectory)
                .redirectErrorStream(true)
                .start();

        OutputCollector collector = new OutputCollector(process.getInputStream());
        Thread collectorThread = new Thread(collector, collectorName);
        collectorThread.setDaemon(true);
        collectorThread.start();
        return new ProcessRun(process, collector, collectorThread);
    }

    private static void assertControlledShutdown(ProcessRun run, int shellPort)
            throws Exception {
        requestControlledShutdown(shellPort);
        assertTrue(
                "Application did not exit after remote-shell quit. Output:\n"
                        + run.collector.snapshot(),
                run.process.waitFor(EXIT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
        run.collectorThread.join(1000L);
        assertEquals(
                "Application exited unsuccessfully. Output:\n"
                        + run.collector.snapshot(),
                0,
                run.process.exitValue());
    }

    private static void cleanupProcess(ProcessRun run) throws InterruptedException {
        if (run == null) {
            return;
        }
        if (run.process.isAlive()) {
            run.process.destroy();
            if (!run.process.waitFor(2000L, TimeUnit.MILLISECONDS)) {
                run.process.destroyForcibly();
                run.process.waitFor(2000L, TimeUnit.MILLISECONDS);
            }
        }
        run.collectorThread.join(1000L);
    }

    private static String combinedOutput(ProcessRun firstRun, ProcessRun secondRun) {
        StringBuilder output = new StringBuilder();
        if (firstRun != null) {
            output.append("=== run 1: commit and WebSocket reconnect ===")
                    .append(System.lineSeparator())
                    .append(firstRun.collector.snapshot());
        }
        if (secondRun != null) {
            output.append("=== run 2: process restart and LogBook recovery ===")
                    .append(System.lineSeparator())
                    .append(secondRun.collector.snapshot());
        }
        return output.toString();
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
        assertContains(json, "\"v\":1");
        assertContains(json, "\"nodeId\":\"" + NODE_ID + "\"");
        assertContains(json, "\"seqNr\":1");
        assertContains(json, "\"locId\":24");
        assertContains(json, "\"recType\":\"AUTO_REG\"");
        assertContains(
                json,
                "\"time\":\"2026-10-01T12:00:00Z\"");
        assertContains(json, "\"regId\":\"N001\"");
        assertContains(json, "\"code\":[\"ADD\"]");
        assertContains(json, "\"recTime\":");
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
                + "      port: " + webSocketPort + "\n"
                + "logging:\n"
                + "  level: INFO\n"
                + "  file:\n"
                + "    path: logs\n"
                + "    rotateBytes: 1048576\n"
                + "    retainedFiles: 5\n";
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

    private static final class ProcessRun {
        private final Process process;
        private final OutputCollector collector;
        private final Thread collectorThread;

        private ProcessRun(
                Process process,
                OutputCollector collector,
                Thread collectorThread) {
            this.process = process;
            this.collector = collector;
            this.collectorThread = collectorThread;
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
