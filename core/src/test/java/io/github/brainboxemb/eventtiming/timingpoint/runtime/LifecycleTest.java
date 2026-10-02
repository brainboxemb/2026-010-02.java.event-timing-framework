package io.github.brainboxemb.eventtiming.timingpoint.runtime;

import io.github.brainboxemb.eventtiming.timingpoint.infra.BuildIdentity;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LifecycleTest {
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
        Lifecycle lifecycle = new Lifecycle(testIdentity());
        assertEquals(Lifecycle.State.NEW, lifecycle.state());

        lifecycle.start();
        assertEquals(Lifecycle.State.RUNNING, lifecycle.state());

        lifecycle.stop();
        assertEquals(Lifecycle.State.STOPPED, lifecycle.state());
    }

    @Test
    public void waitsUntilLifecycleIsClosed() throws Exception {
        Lifecycle lifecycle = new Lifecycle(testIdentity());
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
        assertEquals(Lifecycle.State.STOPPED, lifecycle.state());
        waiter.join(1000);
        assertFalse(waiter.isAlive());
    }

    @Test(expected = IllegalStateException.class)
    public void cannotStartTwice() {
        Lifecycle lifecycle = new Lifecycle(testIdentity());
        lifecycle.start();
        lifecycle.start();
    }

    @Test(expected = IllegalStateException.class)
    public void cannotStopBeforeStart() {
        Lifecycle lifecycle = new Lifecycle(testIdentity());
        lifecycle.stop();
    }

    @Test
    public void closeStopsRunningLifecycle() {
        Lifecycle lifecycle = new Lifecycle(testIdentity());
        lifecycle.start();
        lifecycle.close();
        assertEquals(Lifecycle.State.STOPPED, lifecycle.state());
    }

    @Test
    public void closeBeforeStartLeavesLifecycleStopped() {
        Lifecycle lifecycle = new Lifecycle(testIdentity());
        lifecycle.close();
        assertEquals(Lifecycle.State.STOPPED, lifecycle.state());
    }
}
