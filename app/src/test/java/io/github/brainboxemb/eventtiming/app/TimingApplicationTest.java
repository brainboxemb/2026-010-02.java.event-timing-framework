package io.github.brainboxemb.eventtiming.app;

import io.github.brainboxemb.eventtiming.domain.timing.TimingNode;
import io.github.brainboxemb.eventtiming.domain.timing.TimingNodeId;
import io.github.brainboxemb.eventtiming.infra.BuildIdentity;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class TimingApplicationTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void builderComposesConfiguredTimingNodeAndSharedBoundary() {
        BuildIdentity identity = identity();
        TimingNode timingNode = new TimingNode(new TimingNodeId("timing-node-01"));
        TimingApplication application =
                TimingApplication.builder(identity).timingNode(timingNode).build();

        assertSame(identity, application.buildIdentity());
        assertSame(timingNode, application.timingNode());
        assertSame(identity, application.commandHandler().version());
        assertEquals(TimingApplicationLifecycle.State.NEW, application.state());

        application.start();
        try {
            assertEquals("RUNNING", application.commandHandler().status().applicationState());
            assertEquals(
                    "timing-node-01",
                    application.commandHandler().status().timingNodeId().value());
            assertEquals(
                    TimingNode.Lifecycle.CLOSED,
                    application.commandHandler().status().timingNodeLifecycle());
        } finally {
            application.close();
        }
    }

    @Test
    public void loadsConfigurationIntoApplicationComposition() throws Exception {
        File config = temporaryFolder.newFile("application.yml");
        Files.write(
                config.toPath(),
                ("timingNodeId: configured-node\n"
                        + "presentation:\n"
                        + "  remoteShell:\n"
                        + "    bindAddress: 127.0.0.1\n"
                        + "    port: 8023\n"
                        + "  http:\n"
                        + "    bindAddress: 127.0.0.1\n"
                        + "    port: 8081\n")
                        .getBytes(StandardCharsets.UTF_8));

        TimingApplication application = TimingApplication.configured(identity(), config.toPath());

        assertEquals("configured-node", application.timingNode().timingNodeId().value());
        assertEquals("127.0.0.1", application.remoteShellConfig().bindAddress());
        assertEquals(8023, application.remoteShellConfig().port());
        assertEquals("127.0.0.1", application.httpConfig().bindAddress());
        assertEquals(8081, application.httpConfig().port());
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderRejectsMissingBuildIdentity() {
        TimingApplication.builder(null);
    }

    @Test(expected = IllegalStateException.class)
    public void builderRejectsMissingTimingNode() {
        TimingApplication.builder(identity()).build();
    }

    @Test
    public void formatsStableSmokeOutput() {
        assertEquals(
                "event-timing-app lifecycle OK version=test-version state=STOPPED",
                TimingApplication.smokeOutput(
                        identity(), TimingApplicationLifecycle.State.STOPPED));
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
