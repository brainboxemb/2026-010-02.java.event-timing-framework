package io.github.brainboxemb.eventtiming.application;

import io.github.brainboxemb.eventtiming.domain.timing.TimingNode;
import io.github.brainboxemb.eventtiming.domain.timing.TimingNodeId;
import io.github.brainboxemb.eventtiming.infra.BuildIdentity;

import java.time.Instant;

import org.junit.Test;

import static org.junit.Assert.assertSame;

public class CommandHandlerTest {
    @Test
    public void versionReturnsAuthoritativeBuildIdentity() {
        BuildIdentity identity = identity();
        ApplicationStatus status = status();

        CommandHandler handler = new CommandHandler(identity, () -> status);

        assertSame(identity, handler.version());
    }

    @Test
    public void statusReturnsCurrentSharedStatus() {
        ApplicationStatus status = status();
        CommandHandler handler = new CommandHandler(identity(), () -> status);

        assertSame(status, handler.status());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingBuildIdentity() {
        new CommandHandler(null, CommandHandlerTest::status);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingStatusSupplier() {
        new CommandHandler(identity(), null);
    }

    private static ApplicationStatus status() {
        return new ApplicationStatus(
                "RUNNING",
                Instant.parse("2026-09-25T13:00:00Z"),
                new TimingNodeId("timing-node-01"),
                TimingNode.Lifecycle.CLOSED);
    }

    private static BuildIdentity identity() {
        return BuildIdentity.firstApiVersion(
                "event-timing-app",
                "0.2.2-SNAPSHOT",
                "revision-one",
                "feature/test",
                "local",
                false);
    }
}
