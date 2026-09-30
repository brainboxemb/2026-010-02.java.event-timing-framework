package io.github.brainboxemb.eventtiming.timingpoint.infra.bootstrap.config;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class YamlApplicationConfigLoaderTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void loadsSingleTimingNodeIdWithoutPresentation() throws Exception {
        ApplicationConfig config = load("timingNodeId: timing-node-01\n");

        assertEquals("timing-node-01", config.timingNodeId().value());
        assertNull(config.presentation().remoteShell());
        assertNull(config.presentation().api());
        assertNull(config.logging());
        assertNull(config.loggingServer());
    }

    @Test
    public void loadsImplementedPresentationConfig() throws Exception {
        ApplicationConfig config = load(
                "timingNodeId: timing-node-01\n"
                        + "presentation:\n"
                        + "  remoteShell:\n"
                        + "    bindAddress: 127.0.0.1\n"
                        + "    port: 8023\n"
                        + "  api:\n"
                        + "    http:\n"
                        + "      bindAddress: 127.0.0.1\n"
                        + "      port: 8081\n"
                        + "    webSocket:\n"
                        + "      bindAddress: 127.0.0.1\n"
                        + "      port: 8082\n");

        assertEquals("127.0.0.1", config.presentation().remoteShell().bindAddress());
        assertEquals(8023, config.presentation().remoteShell().port());
        assertEquals("127.0.0.1", config.presentation().api().http().bindAddress());
        assertEquals(8081, config.presentation().api().http().port());
        assertEquals(
                "127.0.0.1",
                config.presentation().api().webSocket().bindAddress());
        assertEquals(8082, config.presentation().api().webSocket().port());
    }

    @Test
    public void loadsRuntimeLoggingConfig() throws Exception {
        ApplicationConfig config = load(
                "timingNodeId: timing-node-01\n"
                        + "logging:\n"
                        + "  level: DEBUG\n"
                        + "  file:\n"
                        + "    path: logs\n"
                        + "    rotateBytes: 1048576\n"
                        + "    retainedFiles: 5\n"
                        + "  live:\n"
                        + "    bindAddress: 127.0.0.1\n"
                        + "    port: 8030\n");

        assertEquals(
                io.github.brainboxemb.eventtiming.timingpoint.infra.logging.LoggingLevel.DEBUG,
                config.logging().level());
        assertEquals("logs", config.logging().file().path());
        assertEquals(1048576, config.logging().file().rotateBytes());
        assertEquals(5, config.logging().file().retainedFiles());
        assertEquals("127.0.0.1", config.loggingServer().bindAddress());
        assertEquals(8030, config.loggingServer().port());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnsupportedLoggingLevel() throws Exception {
        load(
                "timingNodeId: timing-node-01\n"
                        + "logging:\n"
                        + "  level: VERBOSE\n"
                        + "  file:\n"
                        + "    path: logs\n"
                        + "    rotateBytes: 1024\n"
                        + "    retainedFiles: 2\n");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingTimingNodeId() throws Exception {
        load("{}\n");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankTimingNodeId() throws Exception {
        load("timingNodeId: '   '\n");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownRootField() throws Exception {
        load("timingNodeId: timing-node-01\nunknown: true\n");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownHttpField() throws Exception {
        load(
                "timingNodeId: timing-node-01\n"
                        + "presentation:\n"
                        + "  api:\n"
                        + "    http:\n"
                        + "      bindAddress: 127.0.0.1\n"
                        + "      port: 8081\n"
                        + "      protocol: https\n");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingHttpBindAddress() throws Exception {
        load(
                "timingNodeId: timing-node-01\n"
                        + "presentation:\n"
                        + "  api:\n"
                        + "    http:\n"
                        + "      port: 8081\n");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidHttpPort() throws Exception {
        load(
                "timingNodeId: timing-node-01\n"
                        + "presentation:\n"
                        + "  api:\n"
                        + "    http:\n"
                        + "      bindAddress: 127.0.0.1\n"
                        + "      port: 70000\n");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyApi() throws Exception {
        load(
                "timingNodeId: timing-node-01\n"
                        + "presentation:\n"
                        + "  api: {}\n");
    }

    private ApplicationConfig load(String yaml) throws Exception {
        File file = temporaryFolder.newFile("application.yml");
        Files.write(file.toPath(), yaml.getBytes(StandardCharsets.UTF_8));
        return YamlApplicationConfigLoader.load(file.toPath());
    }
}
