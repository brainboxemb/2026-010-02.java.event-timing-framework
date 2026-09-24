package io.github.brainboxemb.eventtiming.infra;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BuildIdentityTest {
    @Test
    public void firstApiVersionUsesPublishedMajorVersion() {
        BuildIdentity identity = BuildIdentity.firstApiVersion(
                "event-timing-app",
                "0.2.0-SNAPSHOT",
                "abc123",
                "2026-09-13T06:00:00Z");

        assertEquals("event-timing-app", identity.application());
        assertEquals("0.2.0-SNAPSHOT", identity.version());
        assertEquals("abc123", identity.revision());
        assertEquals("2026-09-13T06:00:00Z", identity.buildTime());
        assertEquals("1", identity.apiVersion());
        assertEquals("event-timing-app 0.2.0-SNAPSHOT", identity.displayName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankApplication() {
        BuildIdentity.firstApiVersion(" ", "version", "revision", "2026-09-13T06:00:00Z");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankVersion() {
        BuildIdentity.firstApiVersion("app", " ", "revision", "2026-09-13T06:00:00Z");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankRevision() {
        BuildIdentity.firstApiVersion("app", "version", " ", "2026-09-13T06:00:00Z");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankBuildTime() {
        BuildIdentity.firstApiVersion("app", "version", "revision", " ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankApiVersion() {
        new BuildIdentity("app", "version", "revision", "2026-09-13T06:00:00Z", " ");
    }
}
