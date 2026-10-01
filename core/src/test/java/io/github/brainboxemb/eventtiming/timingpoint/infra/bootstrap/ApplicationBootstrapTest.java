package io.github.brainboxemb.eventtiming.timingpoint.infra.bootstrap;

import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeId;
import io.github.brainboxemb.eventtiming.timingpoint.infra.bootstrap.config.ApplicationConfig;
import io.github.brainboxemb.eventtiming.timingpoint.infra.bootstrap.config.PresentationConfig;
import io.github.brainboxemb.eventtiming.timingpoint.infra.BuildIdentity;
import io.github.brainboxemb.eventtiming.timingpoint.runtime.TimingApplication;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ApplicationBootstrapTest {
    @Test
    public void composesConfiguredTimingNodeIntoRuntime() {
        ApplicationConfig config = new ApplicationConfig(
                new TimingNodeId("configured-node"),
                new PresentationConfig(null, null));

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
