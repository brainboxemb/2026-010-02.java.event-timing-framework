package io.github.brainboxemb.eventtiming.application;

import io.github.brainboxemb.eventtiming.infra.BuildIdentity;

import org.junit.Test;

import static org.junit.Assert.assertSame;

public class CommandHandlerTest {
    @Test
    public void versionReturnsAuthoritativeBuildIdentity() {
        BuildIdentity identity = BuildIdentity.firstApiVersion(
                "event-timing-app",
                "0.2.0-SNAPSHOT",
                "revision-one",
                "2026-09-24T18:00:00Z");

        CommandHandler handler = new CommandHandler(identity);

        assertSame(identity, handler.version());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingBuildIdentity() {
        new CommandHandler(null);
    }
}
