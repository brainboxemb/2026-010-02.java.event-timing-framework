package io.github.brainboxemb.eventtiming.app.logging;

import io.github.brainboxemb.eventtiming.infra.bootstrap.config.LoggingConfig;
import io.github.brainboxemb.eventtiming.infra.bootstrap.config.LoggingFileConfig;
import io.github.brainboxemb.eventtiming.infra.bootstrap.config.LoggingLiveConfig;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Logger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RuntimeLoggingTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void writesFileStreamsLiveAndChangesRuntimeLevel() throws Exception {
        File log = new File(temporaryFolder.getRoot(), "event-timing.log");
        LoggingConfig config = new LoggingConfig(
                LoggingConfig.Level.INFO,
                new LoggingFileConfig(log.getAbsolutePath(), 4096, 1),
                new LoggingLiveConfig("127.0.0.1", 0));

        try (RuntimeLogging runtime = RuntimeLogging.start(config);
                Socket socket = new Socket("127.0.0.1", runtime.livePort());
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            assertTrue(readUntil(reader, "\"type\":\"level\"").contains("\"INFO\""));

            Logger logger = Logger.getLogger("test.a08.live");
            logger.info("A08 live message");
            assertTrue(readUntil(reader, "A08 live message").contains("\"type\":\"log\""));

            writer.write("SET_LEVEL DEBUG");
            writer.newLine();
            writer.flush();
            assertTrue(readUntil(reader, "\"type\":\"level\"").contains("\"DEBUG\""));
            assertEquals(LoggingConfig.Level.DEBUG, runtime.level());

            logger.fine("A08 debug message");
            assertTrue(readUntil(reader, "A08 debug message").contains("A08 debug message"));
        }

        String fileText = new String(Files.readAllBytes(log.toPath()), StandardCharsets.UTF_8);
        assertTrue(fileText.contains("A08 live message"));
        assertTrue(fileText.contains("A08 debug message"));
    }

    @Test
    public void rotatesWithinConfiguredRetention() throws Exception {
        File log = new File(temporaryFolder.getRoot(), "rotating.log");
        LoggingConfig config = new LoggingConfig(
                LoggingConfig.Level.INFO,
                new LoggingFileConfig(log.getAbsolutePath(), 512, 2),
                null);

        try (RuntimeLogging ignored = RuntimeLogging.start(config)) {
            Logger logger = Logger.getLogger("test.a08.rotation");
            for (int i = 0; i < 100; i++) {
                logger.info("rotation-" + i + "-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
            }
        }

        File[] generations = temporaryFolder.getRoot().listFiles(
                (dir, name) -> name.startsWith("rotating.log"));
        assertTrue(generations != null && generations.length >= 1);
        assertTrue("retention exceeded: " + generations.length, generations.length <= 2);
    }

    private static String readUntil(BufferedReader reader, String expected) throws Exception {
        long deadline = System.currentTimeMillis() + 3000L;
        while (System.currentTimeMillis() < deadline) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            if (line.contains(expected)) {
                return line;
            }
        }
        throw new AssertionError("Did not receive line containing: " + expected);
    }
}
