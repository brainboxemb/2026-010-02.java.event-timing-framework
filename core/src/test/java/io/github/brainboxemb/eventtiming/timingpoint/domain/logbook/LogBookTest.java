package io.github.brainboxemb.eventtiming.timingpoint.domain.logbook;

import io.github.brainboxemb.eventtiming.timingdata.LocationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingData.ManualTimeSource;
import io.github.brainboxemb.eventtiming.timingdata.RegistrationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingData;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataFactory.Context;
import io.github.brainboxemb.eventtiming.timingdata.TimingTimestamp;
import io.github.brainboxemb.eventtiming.timingdata.TimingNodeId;
import io.github.brainboxemb.eventtiming.timingdata.defaultprofile.DefaultTimingDataFactory;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class LogBookTest {
    private final DefaultTimingDataFactory factory = new DefaultTimingDataFactory();

    @Test
    public void nextSequenceFollowsCommittedStateWithoutConsumingIt() {
        LogBook logBook = new LogBook(new TimingNodeId("timing-node-01"));

        assertEquals(1L, logBook.nextSequence());
        assertEquals(1L, logBook.nextSequence());

        logBook.add(data("timing-node-01", 1L, "1001"));

        assertEquals(2L, logBook.nextSequence());
        assertEquals(1, logBook.size());
    }

    @Test
    public void rejectsSequenceGap() {
        LogBook logBook = new LogBook(new TimingNodeId("timing-node-01"));

        try {
            logBook.add(data("timing-node-01", 2L, "1002"));
            fail("expected sequence validation");
        } catch (IllegalArgumentException expected) {
            assertEquals(0, logBook.size());
            assertEquals(1L, logBook.nextSequence());
        }
    }

    @Test
    public void rejectsDataFromDifferentTimingNode() {
        LogBook logBook = new LogBook(new TimingNodeId("timing-node-01"));

        try {
            logBook.add(data("timing-node-02", 1L, "1001"));
            fail("expected TimingNode validation");
        } catch (IllegalArgumentException expected) {
            assertEquals(0, logBook.size());
        }
    }

    @Test
    public void snapshotIsStableAndImmutable() {
        LogBook logBook = new LogBook(new TimingNodeId("timing-node-01"));
        TimingData first = data("timing-node-01", 1L, "1001");
        TimingData second = data("timing-node-01", 2L, "1002");

        logBook.add(first);
        List<TimingData> snapshot = logBook.snapshot();
        logBook.add(second);

        assertEquals(1, snapshot.size());
        assertEquals(first, snapshot.get(0));
        assertEquals(2, logBook.snapshot().size());

        try {
            snapshot.add(second);
            fail("expected immutable snapshot");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }

    private TimingData data(String nodeId, long sequence, String registrationId) {
        TimingTimestamp effective =
                TimingTimestamp.parse("2026-10-01T12:00:00.000000000Z");
        TimingTimestamp recorded =
                TimingTimestamp.parse("2026-10-01T12:00:01.000000000Z");
        Context context =
                new Context(new TimingNodeId(nodeId), sequence, new LocationId(24), effective, recorded);
        return factory.createManualRegistration(
                context,
                new RegistrationId(registrationId),
                ManualTimeSource.OPERATOR_ENTERED);
    }
}
