package io.github.brainboxemb.eventtiming.app;

import io.github.brainboxemb.eventtiming.application.BuildIdentity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class TimingApplicationTest {
    @Test
    public void builderComposesSharedApplicationBoundary() {
        BuildIdentity identity = BuildIdentity.firstApiVersion(
                "event-timing-app",
                "test-version",
                "abc123def456",
                "2026-09-13T06:00:00Z");

        TimingApplication application = TimingApplication.builder(identity).build();

        assertSame(identity, application.commandHandler().version());
        assertEquals(TimingApplicationLifecycle.State.NEW, application.state());
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderRejectsMissingBuildIdentity() {
        TimingApplication.builder(null);
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
