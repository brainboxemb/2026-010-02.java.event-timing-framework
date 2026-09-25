package io.github.brainboxemb.eventtiming.app;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

public class ApplicationConfigLoaderTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void loadsSingleTimingNodeId() throws Exception {
        ApplicationConfig config = load("timingNodeId: timing-node-01\n");

        assertEquals("timing-node-01", config.timingNodeId().value());
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
    public void rejectsUnimplementedConfigurationFields() throws Exception {
        load("timingNodeId: timing-node-01\npresentation: {}\n");
    }

    private ApplicationConfig load(String yaml) throws Exception {
        File file = temporaryFolder.newFile("application.yml");
        Files.write(file.toPath(), yaml.getBytes(StandardCharsets.UTF_8));
        return ApplicationConfigLoader.load(file.toPath());
    }
}
