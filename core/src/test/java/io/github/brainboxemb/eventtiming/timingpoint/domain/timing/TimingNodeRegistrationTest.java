package io.github.brainboxemb.eventtiming.timingpoint.domain.timing;

import io.github.brainboxemb.eventtiming.timingdata.TimingData.ManualTimeSource;
import io.github.brainboxemb.eventtiming.timingdata.TimingData.ManualRegistration;
import io.github.brainboxemb.eventtiming.timingdata.RegistrationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingData;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataFactory.Context;
import io.github.brainboxemb.eventtiming.timingdata.TimingTimestamp;
import io.github.brainboxemb.eventtiming.timingdata.defaultprofile.DefaultTimingDataFactory;
import io.github.brainboxemb.eventtiming.timingpoint.domain.system.TimeSource;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timingdata.TimingDataStore;
import io.github.brainboxemb.eventtiming.timingpoint.platform.execution.SerialWorker;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TimingNodeRegistrationTest {
    private static final TimingTimestamp EFFECTIVE_TIME =
            TimingTimestamp.parse("2026-10-01T12:00:00.000000000Z");
    private static final TimingTimestamp RECORDED_AT =
            TimingTimestamp.parse("2026-10-01T12:00:01.000000000Z");

    @Test
    public void commitsManualRegistrationAfterStoreAppend() {
        RecordingStore store = new RecordingStore();
        TimingNode node = node(store);

        node.start();
        try {
            node.setLocation(new LocationId(24));
            assertEquals(TimingNode.OpenResult.OPENED, node.open());

            TimingNode.RegistrationResult result = node.registerManual(
                    new RegistrationId("1001"),
                    EFFECTIVE_TIME,
                    ManualTimeSource.OPERATOR_ENTERED);

            assertTrue(result.committed());
            assertEquals(
                    TimingNode.RegistrationResult.Outcome.COMMITTED,
                    result.outcome());
            assertEquals(1, store.appended.size());
            assertSame(result.timingData(), store.appended.get(0));

            List<TimingData> snapshot = node.timingDataSnapshot();
            assertEquals(1, snapshot.size());
            assertSame(result.timingData(), snapshot.get(0));

            ManualRegistration data =
                    (ManualRegistration) result.timingData();
            assertEquals("timing-node-01", data.timingNodeId());
            assertEquals(1L, data.sequenceNumber());
            assertEquals(24, data.locationId());
            assertEquals(EFFECTIVE_TIME, data.effectiveTime());
            assertEquals(RECORDED_AT, data.recordedAt());
            assertEquals(new RegistrationId("1001"), data.registrationId());
            assertEquals(
                    ManualTimeSource.OPERATOR_ENTERED,
                    data.timeSource());
        } finally {
            node.stop();
        }
    }

    @Test
    public void assignsSequenceOnlyFromCommittedLogBookState() {
        RecordingStore store = new RecordingStore();
        TimingNode node = node(store);

        node.start();
        try {
            node.setLocation(new LocationId(24));
            node.open();

            TimingNode.RegistrationResult first = node.registerManual(
                    new RegistrationId("1001"),
                    EFFECTIVE_TIME,
                    ManualTimeSource.SYSTEM_ASSIGNED);
            TimingNode.RegistrationResult second = node.registerManual(
                    new RegistrationId("1002"),
                    EFFECTIVE_TIME,
                    ManualTimeSource.SYSTEM_ASSIGNED);

            assertEquals(1L, first.timingData().sequenceNumber());
            assertEquals(2L, second.timingData().sequenceNumber());
            assertEquals(2, node.timingDataSnapshot().size());
        } finally {
            node.stop();
        }
    }

    @Test
    public void closedNodeRejectsWithoutAllocatingOrPersisting() {
        RecordingStore store = new RecordingStore();
        TimingNode node = node(store);

        node.start();
        try {
            node.setLocation(new LocationId(24));

            TimingNode.RegistrationResult rejected = node.registerManual(
                    new RegistrationId("1001"),
                    EFFECTIVE_TIME,
                    ManualTimeSource.OPERATOR_ENTERED);

            assertEquals(
                    TimingNode.RegistrationResult.Outcome.NODE_NOT_OPEN,
                    rejected.outcome());
            assertEquals(0, store.appended.size());
            assertEquals(0, node.timingDataSnapshot().size());

            node.open();
            TimingNode.RegistrationResult committed = node.registerManual(
                    new RegistrationId("1002"),
                    EFFECTIVE_TIME,
                    ManualTimeSource.OPERATOR_ENTERED);
            assertEquals(1L, committed.timingData().sequenceNumber());
        } finally {
            node.stop();
        }
    }

    @Test
    public void appendFailureLeavesLogBookUnchangedAndBlocksLaterCommit() {
        RecordingStore store = new RecordingStore();
        store.failNext = true;
        TimingNode node = node(store);

        node.start();
        try {
            node.setLocation(new LocationId(24));
            node.open();

            try {
                node.registerManual(
                        new RegistrationId("1001"),
                        EFFECTIVE_TIME,
                        ManualTimeSource.OPERATOR_ENTERED);
                fail("expected persistence failure");
            } catch (TimingNode.OperationException expected) {
                assertEquals(
                        TimingNode.OperationException.Reason.FAILED,
                        expected.reason());
            }

            assertEquals(0, node.timingDataSnapshot().size());
            assertEquals(1, store.attempts);

            try {
                node.registerManual(
                        new RegistrationId("1002"),
                        EFFECTIVE_TIME,
                        ManualTimeSource.OPERATOR_ENTERED);
                fail("expected blocked commit");
            } catch (TimingNode.OperationException expected) {
                assertEquals(
                        TimingNode.OperationException.Reason.FAILED,
                        expected.reason());
            }

            assertEquals(1, store.attempts);
            assertEquals(0, node.timingDataSnapshot().size());
        } finally {
            node.stop();
        }
    }

    @Test
    public void startupRecoversCommittedLogBookAndContinuesSequence() {
        RecordingStore store = new RecordingStore();
        store.loaded.add(recoveredData(1L, 11));
        store.loaded.add(recoveredData(2L, 12));
        TimingNode node = node(store);

        node.start();
        try {
            TimingNode.Status status = node.status();
            assertEquals(TimingNode.Lifecycle.CLOSED, status.lifecycle());
            assertFalse(status.hasLocation());
            assertFalse(status.timingDataTailRecovered());
            assertEquals(2, node.timingDataSnapshot().size());

            assertEquals(TimingNode.OpenResult.NO_LOCATION, node.open());

            node.setLocation(new LocationId(24));
            node.open();
            TimingNode.RegistrationResult committed = node.registerManual(
                    new RegistrationId("1003"),
                    EFFECTIVE_TIME,
                    ManualTimeSource.SYSTEM_ASSIGNED);

            assertEquals(3L, committed.timingData().sequenceNumber());
        } finally {
            node.stop();
        }
    }

    @Test
    public void startupReportsRepairedIncompleteTailInStatus() {
        RecordingStore store = new RecordingStore();
        store.loaded.add(recoveredData(1L, 11));
        store.repairedIncompleteTail = true;
        TimingNode node = node(store);

        node.start();
        try {
            assertTrue(node.status().timingDataTailRecovered());
            assertEquals(1, node.timingDataSnapshot().size());
        } finally {
            node.stop();
        }
    }

    @Test
    public void recoveryFailureLeavesWorkerUnavailableAndNodeCannotRetryStart() {
        RecordingStore store = new RecordingStore();
        store.failLoad = true;
        TimingNode node = node(store);

        try {
            node.start();
            fail("expected startup recovery failure");
        } catch (TimingNode.StartupException expected) {
            assertTrue(expected.getCause() instanceof TimingDataStore.StoreException);
        }

        try {
            node.status();
            fail("expected worker to remain unavailable");
        } catch (TimingNode.OperationException expected) {
            assertEquals(
                    TimingNode.OperationException.Reason.UNAVAILABLE,
                    expected.reason());
        }

        try {
            node.start();
            fail("expected failed node not to restart");
        } catch (TimingNode.StartupException expected) {
            assertTrue(expected.getCause() instanceof TimingDataStore.StoreException);
        }
    }

    @Test
    public void lifecycleOnlyNodeReportsTimingDataUnavailable() {
        TimingNode node = new TimingNode(new TimingNodeId("timing-node-01"));

        node.start();
        try {
            try {
                node.timingDataSnapshot();
                fail("expected unavailable TimingData support");
            } catch (TimingNode.OperationException expected) {
                assertEquals(
                        TimingNode.OperationException.Reason.UNAVAILABLE,
                        expected.reason());
            }
        } finally {
            node.stop();
        }
    }

    private static TimingData recoveredData(long sequence, int locationId) {
        return new DefaultTimingDataFactory().createManualRegistration(
                new Context(
                        "timing-node-01",
                        sequence,
                        locationId,
                        EFFECTIVE_TIME,
                        RECORDED_AT),
                new RegistrationId("recovered-" + sequence),
                ManualTimeSource.SYSTEM_ASSIGNED);
    }

    private static TimingNode node(RecordingStore store) {
        TimeSource timeSource = () -> RECORDED_AT;
        return new TimingNode(
                new TimingNodeId("timing-node-01"),
                new SerialWorker(8, "timing-node-registration-test"),
                1000L,
                store,
                new DefaultTimingDataFactory(),
                timeSource);
    }

    private static final class RecordingStore implements TimingDataStore {
        private final List<TimingData> appended = new ArrayList<>();
        private final List<TimingData> loaded = new ArrayList<>();
        private int attempts;
        private boolean failNext;
        private boolean failLoad;
        private boolean repairedIncompleteTail;

        @Override
        public LoadResult load() throws StoreException {
            if (failLoad) {
                throw new StoreException("expected recovery failure");
            }
            return new LoadResult(loaded, repairedIncompleteTail);
        }

        @Override
        public void append(TimingData data) throws StoreException {
            attempts++;
            if (failNext) {
                failNext = false;
                throw new StoreException("expected test failure");
            }
            appended.add(data);
        }
    }
}
