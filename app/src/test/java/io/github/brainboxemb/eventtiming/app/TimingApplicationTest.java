package io.github.brainboxemb.eventtiming.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimingApplicationTest {
    @Test
    public void consumesFrameworkLibrary() {
        assertEquals("core", TimingApplication.frameworkComponent());
    }

    @Test
    public void formatsStableSmokeOutput() {
        BuildIdentity identity = new BuildIdentity("event-timing-app", "test-version");
        assertEquals(
                "event-timing-app lifecycle OK version=test-version state=STOPPED",
                TimingApplication.smokeOutput(identity, TimingApplicationLifecycle.State.STOPPED));
    }
}
