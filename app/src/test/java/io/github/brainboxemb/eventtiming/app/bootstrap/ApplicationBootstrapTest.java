package io.github.brainboxemb.eventtiming.app.bootstrap;

import io.github.brainboxemb.eventtiming.app.TimingApplication;
import io.github.brainboxemb.eventtiming.domain.timing.TimingNodeId;
import io.github.brainboxemb.eventtiming.infra.BuildIdentity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ApplicationBootstrapTest {
    @Test
    public void composesConfiguredTimingNodeIntoRuntime() {
        ApplicationConfig config = new ApplicationConfig(
                new TimingNodeId("configured-node"),
                new PresentationConfig(null, null));

        TimingApplication application = ApplicationBootstrap.compose(identity(), config);
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
