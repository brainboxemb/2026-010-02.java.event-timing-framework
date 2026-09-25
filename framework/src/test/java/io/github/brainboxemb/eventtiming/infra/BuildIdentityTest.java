package io.github.brainboxemb.eventtiming.infra;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class BuildIdentityTest {
    @Test
    public void firstApiVersionUsesPublishedMajorVersionAndProvenance() {
        BuildIdentity identity = BuildIdentity.firstApiVersion(
                "event-timing-app",
                "0.2.2-SNAPSHOT",
                "abc123",
                "feature/test",
                "local",
                false);

        assertEquals("event-timing-app", identity.application());
        assertEquals("0.2.2-SNAPSHOT", identity.version());
        assertEquals("abc123", identity.revision());
        assertEquals("feature/test", identity.sourceRef());
        assertEquals("local", identity.buildOrigin());
        assertFalse(identity.dirty());
        assertEquals("1", identity.apiVersion());
        assertEquals("event-timing-app 0.2.2-SNAPSHOT", identity.displayName());
        assertEquals(
                "revision=abc123 sourceRef=feature/test buildOrigin=local dirty=false",
                identity.provenance());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankSourceRef() {
        BuildIdentity.firstApiVersion("app", "version", "revision", " ", "local", false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankBuildOrigin() {
        BuildIdentity.firstApiVersion("app", "version", "revision", "main", " ", false);
    }
}
