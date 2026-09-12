package io.github.brainboxemb.eventtiming.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimingApplicationLifecycleTest {
    private static BuildIdentity testIdentity() {
        return new BuildIdentity("event-timing-app", "test-version");
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
