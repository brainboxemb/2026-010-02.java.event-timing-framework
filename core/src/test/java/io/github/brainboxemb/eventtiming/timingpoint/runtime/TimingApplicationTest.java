package io.github.brainboxemb.eventtiming.timingpoint.runtime;

import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNode;
import io.github.brainboxemb.eventtiming.timingdata.TimingNodeId;
import io.github.brainboxemb.eventtiming.timingpoint.infra.BuildIdentity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class TimingApplicationTest {
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
            assertEquals(
                    "timing-node-01",
                    application.commandHandler().status().timingNodeId().value());
            assertEquals(
                    TimingNode.Lifecycle.CLOSED,
                    application.commandHandler().status().timingNodeLifecycle());
        } finally {
            application.close();
        }

        try {
            timingNode.status();
            org.junit.Assert.fail("expected TimingNode to be unavailable after application close");
        } catch (TimingNode.OperationException expected) {
            assertEquals(
                    TimingNode.OperationException.Reason.UNAVAILABLE,
                    expected.reason());
        }
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
