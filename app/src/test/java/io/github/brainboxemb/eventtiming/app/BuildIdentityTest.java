package io.github.brainboxemb.eventtiming.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BuildIdentityTest {
    @Test
    public void loadsFilteredBuildIdentity() {
        BuildIdentity identity = BuildIdentity.load();

        assertEquals("event-timing-app", identity.applicationName());
        assertEquals("0.1.0-SNAPSHOT", identity.version());
        assertEquals("event-timing-app 0.1.0-SNAPSHOT", identity.displayName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankApplicationName() {
        new BuildIdentity(" ", "test-version");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankVersion() {
        new BuildIdentity("event-timing-app", " ");
    }
}
