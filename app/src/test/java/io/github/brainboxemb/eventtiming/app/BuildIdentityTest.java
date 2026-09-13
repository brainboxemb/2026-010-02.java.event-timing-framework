package io.github.brainboxemb.eventtiming.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BuildIdentityTest {
    @Test
    public void loadsFilteredBuildIdentity() {
        BuildIdentity identity = BuildIdentity.load();

        assertEquals("event-timing-app", identity.applicationName());
        assertEquals("0.1.0-SNAPSHOT", identity.version());
        assertEquals("event-timing-app 0.1.0-SNAPSHOT", identity.displayName());
        assertFalse(identity.revision().trim().isEmpty());
        assertFalse(identity.buildTimestamp().trim().isEmpty());
        assertTrue(identity.provenance().contains("revision=" + identity.revision()));
        assertTrue(identity.provenance().contains("built=" + identity.buildTimestamp()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankApplicationName() {
        new BuildIdentity(" ", "test-version", "abc123", "2026-09-13T06:00:00Z");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankVersion() {
        new BuildIdentity("event-timing-app", " ", "abc123", "2026-09-13T06:00:00Z");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankRevision() {
        new BuildIdentity("event-timing-app", "test-version", " ", "2026-09-13T06:00:00Z");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankBuildTimestamp() {
        new BuildIdentity("event-timing-app", "test-version", "abc123", " ");
    }
}
