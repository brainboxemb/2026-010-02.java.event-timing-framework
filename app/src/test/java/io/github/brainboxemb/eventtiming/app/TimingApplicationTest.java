package io.github.brainboxemb.eventtiming.app;

import io.github.brainboxemb.eventtiming.domain.TimingNode;
import io.github.brainboxemb.eventtiming.domain.TimingNodeId;
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
        BuildIdentity identity = BuildIdentity.firstApiVersion(
                "event-timing-app",
                "test-version",
                "abc123def456",
                "2026-09-13T06:00:00Z");
        TimingNode timingNode = new TimingNode(new TimingNodeId("timing-node-01"));

        TimingApplication application =
                TimingApplication.builder(identity).timingNode(timingNode).build();

        assertSame(identity, application.buildIdentity());
        assertSame(timingNode, application.timingNode());
        assertEquals("timing-node-01", application.timingNode().timingNodeId().value());
        assertSame(identity, application.commandHandler().version());
        assertEquals(TimingApplicationLifecycle.State.NEW, application.state());
    }

    @Test
    public void loadsConfigurationIntoApplicationComposition() throws Exception {
        File config = temporaryFolder.newFile("application.yml");
        Files.write(
                config.toPath(),
                "timingNodeId: configured-node\n".getBytes(StandardCharsets.UTF_8));
        BuildIdentity identity = BuildIdentity.firstApiVersion(
                "event-timing-app",
                "test-version",
                "abc123def456",
                "2026-09-13T06:00:00Z");

        TimingApplication application = TimingApplication.configured(identity, config.toPath());

        assertSame(identity, application.buildIdentity());
        assertEquals("configured-node", application.timingNode().timingNodeId().value());
        assertEquals(TimingApplicationLifecycle.State.NEW, application.state());
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderRejectsMissingBuildIdentity() {
        TimingApplication.builder(null);
    }

    @Test(expected = IllegalStateException.class)
    public void builderRejectsMissingTimingNode() {
        BuildIdentity identity = BuildIdentity.firstApiVersion(
                "event-timing-app",
                "test-version",
                "abc123def456",
                "2026-09-13T06:00:00Z");

        TimingApplication.builder(identity).build();
    }

    @Test
    public void formatsStableSmokeOutput() {
        BuildIdentity identity = BuildIdentity.firstApiVersion(
                "event-timing-app",
                "test-version",
                "abc123def456",
                "2026-09-13T06:00:00Z");
        assertEquals(
                "event-timing-app lifecycle OK version=test-version state=STOPPED",
                TimingApplication.smokeOutput(identity, TimingApplicationLifecycle.State.STOPPED));
    }
}
