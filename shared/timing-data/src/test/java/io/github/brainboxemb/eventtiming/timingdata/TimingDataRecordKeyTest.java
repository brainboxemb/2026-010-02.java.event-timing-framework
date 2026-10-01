package io.github.brainboxemb.eventtiming.timingdata;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TimingDataRecordKeyTest {

    @Test
    public void acceptsFirstAndMaximumSequence() {
        assertEquals(1L, new TimingDataRecordKey("timing-node-01", 1L).sequenceNumber());
        assertEquals(
                TimingDataRecordKey.MAX_SEQUENCE_NUMBER,
                new TimingDataRecordKey(
                        "timing-node-01",
                        TimingDataRecordKey.MAX_SEQUENCE_NUMBER).sequenceNumber());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsReservedZeroSequence() {
        new TimingDataRecordKey("timing-node-01", 0L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsSequenceBeyondJsonSafeRange() {
        new TimingDataRecordKey(
                "timing-node-01",
                TimingDataRecordKey.MAX_SEQUENCE_NUMBER + 1L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankTimingNodeIdentity() {
        new TimingDataRecordKey("   ", 1L);
    }

    @Test
    public void preservesSerializedTimingNodeIdentityWithoutNormalizingIt() {
        TimingDataRecordKey key = new TimingDataRecordKey(" timing-node-01 ", 1L);
        assertEquals(" timing-node-01 ", key.timingNodeId());
    }

    @Test
    public void equalityUsesBothSourceAndSequence() {
        TimingDataRecordKey key = new TimingDataRecordKey("timing-node-01", 7L);
        assertEquals(key, new TimingDataRecordKey("timing-node-01", 7L));
        assertNotEquals(key, new TimingDataRecordKey("timing-node-02", 7L));
        assertNotEquals(key, new TimingDataRecordKey("timing-node-01", 8L));
    }
}
