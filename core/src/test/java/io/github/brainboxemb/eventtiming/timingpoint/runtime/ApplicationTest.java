package io.github.brainboxemb.eventtiming.timingpoint.runtime;

import io.github.brainboxemb.eventtiming.timingdata.TimingNodeId;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNode;
import io.github.brainboxemb.eventtiming.timingpoint.infra.BuildIdentity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class ApplicationTest {
    @Test
    public void createsSharedBoundaryForConfiguredTimingNode() {
        BuildIdentity identity = identity();
        TimingNode timingNode = new TimingNode(new TimingNodeId("timing-node-01"));
        Application application = new Application(identity, timingNode);

        assertSame(identity, application.buildIdentity());
        assertSame(timingNode, application.timingNode());
        assertSame(identity, application.commandHandler().version());
        assertEquals(Lifecycle.State.NEW, application.state());

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
            fail("expected TimingNode to be unavailable after application close");
        } catch (TimingNode.OperationException expected) {
            assertEquals(
                    TimingNode.OperationException.Reason.UNAVAILABLE,
                    expected.reason());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingBuildIdentity() {
        new Application(null, new TimingNode(new TimingNodeId("timing-node-01")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingTimingNode() {
        new Application(identity(), null);
    }

    @Test
    public void formatsStableSmokeOutput() {
        assertEquals(
                "event-timing-app lifecycle OK version=test-version state=STOPPED",
                Application.smokeOutput(identity(), Lifecycle.State.STOPPED));
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
