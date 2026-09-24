package io.github.brainboxemb.eventtiming.app;

import io.github.brainboxemb.eventtiming.infra.BuildIdentity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimingApplicationLifecycleTest {
    private static BuildIdentity testIdentity() {
        return BuildIdentity.firstApiVersion(
                "event-timing-app",
                "test-version",
                "abc123def456",
                "2026-09-13T06:00:00Z");
    }

    @Test
    public void startsAndStopsCleanly() {
        TimingApplicationLifecycle lifecycle = new TimingApplicationLifecycle(testIdentity());
        assertEquals(TimingApplicationLifecycle.State.NEW, lifecycle.state());

        lifecycle.start();
        assertEquals(TimingApplicationLifecycle.State.RUNNING, lifecycle.state());

        lifecycle.stop();
        assertEquals(TimingApplicationLifecycle.State.STOPPED, lifecycle.state());
    }

    @Test(expected = IllegalStateException.class)
    public void cannotStartTwice() {
        TimingApplicationLifecycle lifecycle = new TimingApplicationLifecycle(testIdentity());
        lifecycle.start();
        lifecycle.start();
    }

    @Test(expected = IllegalStateException.class)
    public void cannotStopBeforeStart() {
        TimingApplicationLifecycle lifecycle = new TimingApplicationLifecycle(testIdentity());
        lifecycle.stop();
    }

    @Test
    public void closeStopsRunningLifecycle() {
        TimingApplicationLifecycle lifecycle = new TimingApplicationLifecycle(testIdentity());
        lifecycle.start();

        lifecycle.close();

        assertEquals(TimingApplicationLifecycle.State.STOPPED, lifecycle.state());
    }

    @Test
    public void closeBeforeStartLeavesLifecycleStopped() {
        TimingApplicationLifecycle lifecycle = new TimingApplicationLifecycle(testIdentity());

        lifecycle.close();

        assertEquals(TimingApplicationLifecycle.State.STOPPED, lifecycle.state());
    }
}
