package io.github.brainboxemb.eventtiming.timingpoint.infra.logging;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class LoggingTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void appliesCompactFormatterToConsoleAndRestoresPreviousFormatter() throws Exception {
        Logger root = Logger.getLogger("");
        ConsoleHandler console = new ConsoleHandler();
        Formatter previousFormatter = console.getFormatter();
        root.addHandler(console);
        try {
            File logDirectory = temporaryFolder.newFolder("console");
            LoggingConfig config = new LoggingConfig(
                    LoggingLevel.INFO,
                    new LoggingFileConfig(logDirectory.getAbsolutePath(), 4096, 1));

            try (Logging ignored = Logging.start(config)) {
                assertTrue(console.getFormatter() instanceof CompactLogFormatter);

                LogRecord record = new LogRecord(Level.INFO, "Application lifecycle state=RUNNING");
                record.setSourceClassName(
                        "io.github.brainboxemb.eventtiming.timingpoint.runtime.TimingApplicationLifecycle");
                record.setSourceMethodName("start");
                String line = console.getFormatter().format(record);
                assertTrue(line.matches(
                        "\\d{2}:\\d{2}:\\d{2}\\.\\d{3} - \\[INFO\\] - "
                                + "Application lifecycle state=RUNNING - "
                                + "\\[runtime\\.TimingApplicationLifecycle\\.start\\]\\R"));
            }

            assertSame(previousFormatter, console.getFormatter());
        } finally {
            root.removeHandler(console);
            console.close();
        }
    }

    @Test
    public void keepsLastProjectPackageSegmentForSourceContext() {
        LogRecord record = new LogRecord(Level.INFO, "Remote terminal listening");
        record.setSourceClassName(
                "io.github.brainboxemb.eventtiming.timingpoint.presentation.interfaces.shell.RemoteShellServer");
        record.setSourceMethodName("start");

        assertEquals(
                "shell.RemoteShellServer.start",
                CompactLogFormatter.source(record));
    }

    @Test
    public void keepsNonProjectSourceQualified() {
        LogRecord record = new LogRecord(Level.INFO, "External component");
        record.setSourceClassName("com.example.transport.ExternalAdapter");
        record.setSourceMethodName("start");

        assertEquals(
                "com.example.transport.ExternalAdapter.start",
                CompactLogFormatter.source(record));
    }

    @Test
    public void rotatesWithinConfiguredRetention() throws Exception {
        File logDirectory = temporaryFolder.newFolder("rotating");
        LoggingConfig config = new LoggingConfig(
                LoggingLevel.INFO,
                new LoggingFileConfig(logDirectory.getAbsolutePath(), 512, 2));

        try (Logging ignored = Logging.start(config)) {
            Logger logger = Logger.getLogger("test.a08.rotation");
            for (int i = 0; i < 100; i++) {
                logger.info("rotation-" + i + "-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
            }
        }

        File[] generations = timestampedLogs(logDirectory);
        assertTrue(generations.length >= 1);
        assertTrue("retention exceeded: " + generations.length, generations.length <= 2);
        for (File generation : generations) {
            assertTrue(generation.getName().matches(
                    "\\d{8}-\\d{6}(?:-\\d{2,})?\\.txt"));
        }
    }

    @Test
    public void repeatedWallClockTimestampNeverOverwritesExistingLog() throws Exception {
        File logDirectory = temporaryFolder.newFolder("repeated-clock");
        Path existing = logDirectory.toPath().resolve("20250514-101657.txt");
        Files.write(existing, "previous-session".getBytes(StandardCharsets.UTF_8));

        Clock repeatedClock = Clock.fixed(
                Instant.parse("2025-05-14T08:16:57Z"),
                ZoneId.of("Europe/Amsterdam"));

        Path current;
        TimestampedFileLogHandler handler =
                new TimestampedFileLogHandler(logDirectory.toPath(), 4096, 10, repeatedClock);
        try {
            current = handler.currentFile();
            assertEquals("20250514-101657-01.txt", current.getFileName().toString());
            assertTrue(Files.exists(current));
        } finally {
            handler.close();
        }

        assertEquals(
                "previous-session",
                new String(Files.readAllBytes(existing), StandardCharsets.UTF_8));
        assertTrue(Files.exists(current));
    }

    @Test
    public void staleWallClockCannotPruneActiveLog() throws Exception {
        File logDirectory = temporaryFolder.newFolder("stale-clock-retention");
        Files.write(
                logDirectory.toPath().resolve("20300101-000000.txt"),
                "future-a".getBytes(StandardCharsets.UTF_8));
        Files.write(
                logDirectory.toPath().resolve("20400101-000000.txt"),
                "future-b".getBytes(StandardCharsets.UTF_8));

        Clock staleClock = Clock.fixed(
                Instant.parse("2025-05-14T08:16:57Z"),
                ZoneId.of("Europe/Amsterdam"));

        Path current;
        TimestampedFileLogHandler handler =
                new TimestampedFileLogHandler(logDirectory.toPath(), 4096, 2, staleClock);
        try {
            current = handler.currentFile();
            assertEquals("20250514-101657.txt", current.getFileName().toString());
            assertTrue(Files.exists(current));
        } finally {
            handler.close();
        }

        assertTrue(Files.exists(current));
        assertEquals(2, timestampedLogs(logDirectory).length);
    }

    @Test
    public void formatsRetainedRecordLikeExistingOperatorLogs() {
        LogRecord record = new LogRecord(Level.INFO, "WebServer started");
        record.setMillis(1747210618109L);
        record.setSourceClassName("com.bata.ebart.next.web.WebServer");
        record.setSourceMethodName("<init>");

        String line = new CompactLogFormatter(java.time.ZoneId.of("Europe/Amsterdam"))
                .format(record);

        assertTrue(line.matches(
                "\\d{2}:\\d{2}:\\d{2}\\.\\d{3} - \\[INFO\\] - WebServer started - "
                        + "\\[com\\.bata\\.ebart\\.next\\.web\\.WebServer\\.<init>\\]"
                        + "\\R"));
    }

    private static File[] timestampedLogs(File directory) {
        File[] files = directory.listFiles(
                (dir, name) -> name.matches(
                        "\\d{8}-\\d{6}(?:-\\d{2,})?\\.txt"));
        if (files == null) {
            return new File[0];
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        return files;
    }

}
