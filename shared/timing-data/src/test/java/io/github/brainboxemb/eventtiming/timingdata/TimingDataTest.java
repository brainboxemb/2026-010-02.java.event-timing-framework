package io.github.brainboxemb.eventtiming.timingdata;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TimingDataTest {

    @Test
    public void recordKeyAcceptsFirstAndMaximumSequence() {
        assertEquals(1L, new TimingData.RecordKey(new TimingDataTypes.NodeId("TN-01"), 1L).sequenceNumber());
        assertEquals(
                TimingData.MAX_SEQUENCE_NUMBER,
                new TimingData.RecordKey(
                        new TimingDataTypes.NodeId("TN-01"),
                        TimingData.MAX_SEQUENCE_NUMBER).sequenceNumber());
    }

    @Test(expected = IllegalArgumentException.class)
    public void recordKeyRejectsReservedZeroSequence() {
        new TimingData.RecordKey(new TimingDataTypes.NodeId("TN-01"), 0L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void recordKeyRejectsSequenceBeyondJsonSafeRange() {
        new TimingData.RecordKey(
                new TimingDataTypes.NodeId("TN-01"),
                TimingData.MAX_SEQUENCE_NUMBER + 1L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void recordKeyRejectsMissingNodeIdentity() {
        new TimingData.RecordKey(null, 1L);
    }

    @Test
    public void recordKeyUsesSharedNormalizedNodeIdentity() {
        TimingData.RecordKey key = new TimingData.RecordKey(new TimingDataTypes.NodeId(" TN-01 "), 1L);
        assertEquals(new TimingDataTypes.NodeId("TN-01"), key.timingNodeId());
    }

    @Test
    public void recordKeyEqualityUsesBothSourceAndSequence() {
        TimingData.RecordKey key = new TimingData.RecordKey(new TimingDataTypes.NodeId("TN-01"), 7L);
        assertEquals(key, new TimingData.RecordKey(new TimingDataTypes.NodeId("TN-01"), 7L));
        assertNotEquals(key, new TimingData.RecordKey(new TimingDataTypes.NodeId("timing-node-02"), 7L));
        assertNotEquals(key, new TimingData.RecordKey(new TimingDataTypes.NodeId("TN-01"), 8L));
    }
}
