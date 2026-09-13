package io.github.brainboxemb.eventtiming.app;

import java.time.OffsetDateTime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BuildIdentityTest {
    @Test
    public void loadsFilteredBuildIdentity() {
        BuildIdentity identity = BuildIdentity.load();
        String expectedProjectVersion = System.getProperty("eventTiming.expectedProjectVersion");

        assertNotNull("Maven must expose the project version to the test JVM", expectedProjectVersion);
        assertEquals("event-timing-app", identity.applicationName());
        assertEquals(expectedProjectVersion, identity.version());
        assertEquals("event-timing-app " + expectedProjectVersion, identity.displayName());

        // These checks deliberately fail if Maven resource filtering leaves ${...} placeholders
        // behind or if the Git metadata plugin stops supplying the expected build properties.
        assertTrue(identity.revision().matches("[0-9a-f]{40}"));
        assertFalse(identity.buildTimestamp().contains("${"));
        OffsetDateTime.parse(identity.buildTimestamp());

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
