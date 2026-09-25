package io.github.brainboxemb.eventtiming.app;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ApplicationConfigLoaderTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void loadsSingleTimingNodeIdWithoutPresentation() throws Exception {
        ApplicationConfig config = load("timingNodeId: timing-node-01\n");

        assertEquals("timing-node-01", config.timingNodeId().value());
        assertNull(config.presentation().remoteShell());
        assertNull(config.presentation().remoteApi());
    }

    @Test
    public void loadsImplementedPresentationConfig() throws Exception {
        ApplicationConfig config = load(
                "timingNodeId: timing-node-01\n"
                        + "presentation:\n"
                        + "  remoteShell:\n"
                        + "    bindAddress: 127.0.0.1\n"
                        + "    port: 8023\n"
                        + "  remoteApi:\n"
                        + "    http:\n"
                        + "      bindAddress: 127.0.0.1\n"
                        + "      port: 8081\n"
                        + "    webSocket:\n"
                        + "      bindAddress: 127.0.0.1\n"
                        + "      port: 8082\n");

        assertEquals("127.0.0.1", config.presentation().remoteShell().bindAddress());
        assertEquals(8023, config.presentation().remoteShell().port());
        assertEquals("127.0.0.1", config.presentation().remoteApi().http().bindAddress());
        assertEquals(8081, config.presentation().remoteApi().http().port());
        assertEquals(
                "127.0.0.1",
                config.presentation().remoteApi().webSocket().bindAddress());
        assertEquals(8082, config.presentation().remoteApi().webSocket().port());
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
                        + "  remoteApi:\n"
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
                        + "  remoteApi:\n"
                        + "    http:\n"
                        + "      port: 8081\n");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidHttpPort() throws Exception {
        load(
                "timingNodeId: timing-node-01\n"
                        + "presentation:\n"
                        + "  remoteApi:\n"
                        + "    http:\n"
                        + "      bindAddress: 127.0.0.1\n"
                        + "      port: 70000\n");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyRemoteApi() throws Exception {
        load(
                "timingNodeId: timing-node-01\n"
                        + "presentation:\n"
                        + "  remoteApi: {}\n");
    }

    private ApplicationConfig load(String yaml) throws Exception {
        File file = temporaryFolder.newFile("application.yml");
        Files.write(file.toPath(), yaml.getBytes(StandardCharsets.UTF_8));
        return ApplicationConfigLoader.load(file.toPath());
    }
}
