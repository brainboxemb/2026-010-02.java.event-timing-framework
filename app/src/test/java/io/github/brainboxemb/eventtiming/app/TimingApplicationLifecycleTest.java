package io.github.brainboxemb.eventtiming.app;

import io.github.brainboxemb.eventtiming.infra.BuildIdentity;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TimingApplicationLifecycleTest {
    private static BuildIdentity testIdentity() {
        return BuildIdentity.firstApiVersion(
                "event-timing-app",
                "test-version",
                "abc123def456",
                "feature/test",
                "local",
                false);
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

    @Test
    public void waitsUntilLifecycleIsClosed() throws Exception {
        TimingApplicationLifecycle lifecycle = new TimingApplicationLifecycle(testIdentity());
        CountDownLatch waiting = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(1);

        lifecycle.start();

        Thread waiter = new Thread(() -> {
            waiting.countDown();
            try {
                lifecycle.awaitStopped();
                completed.countDown();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        waiter.start();

        assertTrue(waiting.await(1, TimeUnit.SECONDS));
        assertFalse(completed.await(50, TimeUnit.MILLISECONDS));

        lifecycle.close();

        assertTrue(completed.await(1, TimeUnit.SECONDS));
        assertEquals(TimingApplicationLifecycle.State.STOPPED, lifecycle.state());
        waiter.join(1000);
        assertFalse(waiter.isAlive());
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
