package io.github.brainboxemb.eventtiming.app;

import io.github.brainboxemb.eventtiming.application.BuildIdentity;

import java.time.OffsetDateTime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BuildIdentityTest {
    @Test
    public void loadsFilteredBuildIdentityIntoSharedApplicationValue() {
        BuildIdentity identity = BuildIdentityLoader.load();
        String expectedProjectVersion = System.getProperty("eventTiming.expectedProjectVersion");

        assertNotNull("Maven must expose the project version to the test JVM", expectedProjectVersion);
        assertEquals("event-timing-app", identity.application());
        assertEquals(expectedProjectVersion, identity.version());
        assertEquals("1", identity.apiVersion());
        assertEquals("event-timing-app " + expectedProjectVersion, identity.displayName());

        // These checks deliberately fail if Maven resource filtering leaves ${...} placeholders
        // behind or if the Git metadata plugin stops supplying the expected build properties.
        assertTrue(identity.revision().matches("[0-9a-f]{40}"));
        assertFalse(identity.buildTime().contains("${"));
        OffsetDateTime.parse(identity.buildTime());

        assertTrue(identity.provenance().contains("revision=" + identity.revision()));
        assertTrue(identity.provenance().contains("built=" + identity.buildTime()));
    }
}
