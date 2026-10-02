package io.github.brainboxemb.eventtiming.timingpoint.application;

import io.github.brainboxemb.eventtiming.timingdata.LocationId;
import io.github.brainboxemb.eventtiming.timingdata.RegistrationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingData;
import io.github.brainboxemb.eventtiming.timingdata.TimingNodeId;
import io.github.brainboxemb.eventtiming.timingdata.TimingTimestamp;
import io.github.brainboxemb.eventtiming.timingdata.defaultprofile.DefaultTimingDataFactory;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNode;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeTypes;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timingdata.TimingDataPersistence;
import io.github.brainboxemb.eventtiming.timingpoint.infra.BuildIdentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CommandHandlerTest {
    private static final TimingTimestamp OBSERVATION_TIME =
            TimingTimestamp.parse("2026-10-01T12:00:00.000000000Z");
    private static final TimingTimestamp RECORDED_AT =
            TimingTimestamp.parse("2026-10-01T12:00:01.000000000Z");

    @Test
    public void versionReturnsAuthoritativeBuildIdentity() {
        BuildIdentity identity = identity();
        ApplicationStatus status = status();

        CommandHandler handler = new CommandHandler(identity, () -> status);

        assertSame(identity, handler.version());
    }

    @Test
    public void statusOnlyConstructorStillSupportsSimplePresentationTests() {
        ApplicationStatus status = status();
        CommandHandler handler = new CommandHandler(identity(), () -> status);

        assertSame(status, handler.status());
    }

    @Test
    public void fullHandlerOwnsFirstRegistrationApplicationBoundary() {
        RecordingStore store = new RecordingStore();
        TimingNode node = node(store);
        CommandHandler handler = new CommandHandler(identity(), node);
        List<ApplicationStatus> statusChanges = new ArrayList<>();
        List<TimingData> committed = new ArrayList<>();

        node.start();
        try {
            handler.statusChanged().subscribe(statusChanges::add);
            handler.newTimingData().subscribe(committed::add);

            CommandHandler.Capabilities capabilities = handler.capabilities();
            assertTrue(capabilities.directRegistrationSimulationSupported());
            assertTrue(capabilities.directRegistrationSimulationEnabled());

            assertFalse(handler.status().hasLocation());

            assertEquals(
                    TimingNodeTypes.SetLocationResult.UPDATED,
                    handler.setLocation(new LocationId(24)));
            assertEquals(1, statusChanges.size());
            assertEquals(new LocationId(24), statusChanges.get(0).locationId());

            // The domain returns UPDATED again, but the authoritative state did
            // not change, so no duplicate STATUS_CHANGED event is manufactured.
            assertEquals(
                    TimingNodeTypes.SetLocationResult.UPDATED,
                    handler.setLocation(new LocationId(24)));
            assertEquals(1, statusChanges.size());

            assertEquals(TimingNodeTypes.OpenResult.OPENED, handler.open());
            assertEquals(2, statusChanges.size());
            assertEquals(
                    TimingNodeTypes.Lifecycle.OPEN,
                    statusChanges.get(1).timingNodeLifecycle());

            TimingNodeTypes.RegistrationResult registration = handler.commitAutomaticRegistration(
                    new RegistrationId("N001"),
                    OBSERVATION_TIME);
            assertTrue(registration.committed());
            assertEquals(1, committed.size());
            assertSame(registration.timingData(), committed.get(0));
            assertEquals(1, handler.logBookCount());
            assertEquals(1, handler.logBookFrom(1L, 10).size());
            assertSame(registration.timingData(), handler.logBookFrom(1L, 10).get(0));
            assertSame(registration.timingData(), handler.latestLogBook(10).get(0));

            assertEquals(TimingNodeTypes.CloseResult.CLOSED, handler.close());
            assertEquals(3, statusChanges.size());
            assertEquals(
                    TimingNodeTypes.Lifecycle.CLOSED,
                    statusChanges.get(2).timingNodeLifecycle());
        } finally {
            node.stop();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void statusOnlyHandlerRejectsOperationalCommands() {
        new CommandHandler(identity(), CommandHandlerTest::status).capabilities();
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingBuildIdentity() {
        new CommandHandler(null, CommandHandlerTest::status);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingStatusSupplier() {
        new CommandHandler(identity(), (java.util.function.Supplier<ApplicationStatus>) null);
    }

    private static ApplicationStatus status() {
        return new ApplicationStatus(
                new TimingNodeId("timing-node-01"),
                TimingNodeTypes.Lifecycle.CLOSED);
    }

    private static TimingNode node(RecordingStore store) {
        return new TimingNode(
                new TimingNodeId("timing-node-01"),
                store,
                new DefaultTimingDataFactory(),
                () -> RECORDED_AT);
    }

    private static BuildIdentity identity() {
        return BuildIdentity.firstApiVersion(
                "event-timing-app",
                "0.2.3-SNAPSHOT",
                "revision-one",
                "feature/test",
                "local",
                false);
    }

    private static final class RecordingStore implements TimingDataPersistence {
        private final List<TimingData> appended = new ArrayList<>();

        @Override
        public LoadResult load() {
            return new LoadResult(Collections.<TimingData>emptyList(), false);
        }

        @Override
        public void append(TimingData data) {
            appended.add(data);
        }
    }
}
