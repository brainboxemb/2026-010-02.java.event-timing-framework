package io.github.brainboxemb.eventtiming.timingpoint.runtime;

import io.github.brainboxemb.eventtiming.timingdata.TimingNodeId;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNode;
import io.github.brainboxemb.eventtiming.timingpoint.infra.BuildIdentity;
import io.github.brainboxemb.eventtiming.timingpoint.runtime.config.Config;
import io.github.brainboxemb.eventtiming.timingpoint.runtime.config.Presentation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class CompositionTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void composesConfiguredTimingNodeIntoRuntime() {
        Config config = config(
                temporaryFolder.getRoot().toPath().resolve("timing-data.jsonl"));

        Application application = Composition.create(identity(), config);
        application.start();
        try {
            assertEquals(
                    "configured-node",
                    application.commandHandler().status().timingNodeId().value());
        } finally {
            application.close();
        }
    }

    @Test
    public void composedApplicationRecoversConfiguredTimingDataFileBeforeWorkerStarts()
            throws Exception {
        Path file = temporaryFolder.getRoot().toPath().resolve("timing-data.jsonl");
        Files.write(
                file,
                "{not-json}\n".getBytes(StandardCharsets.UTF_8));

        Application application = Composition.create(identity(), config(file));

        try {
            application.start();
            fail("expected TimingData recovery failure");
        } catch (TimingNode.StartupException expected) {
            // The configured I/O store was opened during runtime startup.
        } finally {
            application.close();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void createRejectsMissingTimingDataPath() {
        Config config = new Config(
                new TimingNodeId("configured-node"),
                new Presentation(null, null));

        Composition.create(identity(), config);
    }

    private static Config config(Path timingDataPath) {
        return new Config(
                new TimingNodeId("configured-node"),
                new Presentation(null, null),
                null,
                null,
                timingDataPath);
    }

    private static BuildIdentity identity() {
        return BuildIdentity.firstApiVersion(
                "event-timing-app",
                "test-version",
                "abc123def456",
                "feature/test",
                "local",
                false);
    }
}
