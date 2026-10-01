package io.github.brainboxemb.eventtiming.timingpoint.infra.bootstrap;

import io.github.brainboxemb.eventtiming.timingdata.TimingNodeId;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNode;
import io.github.brainboxemb.eventtiming.timingpoint.infra.BuildIdentity;
import io.github.brainboxemb.eventtiming.timingpoint.infra.bootstrap.config.ApplicationConfig;
import io.github.brainboxemb.eventtiming.timingpoint.infra.bootstrap.config.PresentationConfig;
import io.github.brainboxemb.eventtiming.timingpoint.runtime.TimingApplication;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ApplicationBootstrapTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void composesConfiguredTimingNodeIntoRuntime() {
        ApplicationConfig config = config(
                temporaryFolder.getRoot().toPath().resolve("timing-data.jsonl"));

        TimingApplication application = ApplicationBootstrap.compose(identity(), config);
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

        TimingApplication application =
                ApplicationBootstrap.compose(identity(), config(file));

        try {
            application.start();
            fail("expected TimingData recovery failure");
        } catch (TimingNode.StartupException expected) {
            // The malformed configured store was actually opened during startup.
        } finally {
            application.close();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void composeRejectsMissingTimingDataPath() {
        ApplicationConfig config = new ApplicationConfig(
                new TimingNodeId("configured-node"),
                new PresentationConfig(null, null));

        ApplicationBootstrap.compose(identity(), config);
    }

    private static ApplicationConfig config(Path timingDataPath) {
        return new ApplicationConfig(
                new TimingNodeId("configured-node"),
                new PresentationConfig(null, null),
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
