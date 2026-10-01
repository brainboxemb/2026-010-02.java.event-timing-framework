package io.github.brainboxemb.eventtiming.timingdata;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class TimingDataContextTest {
    private static final TimingTimestamp EFFECTIVE =
            TimingTimestamp.parse("2026-09-30T20:01:39.123000000Z");
    private static final TimingTimestamp RECORDED =
            TimingTimestamp.parse("2026-09-30T20:01:40.000000000Z");

    @Test
    public void carriesCommonTimingDataConstructionValues() {
        TimingDataContext context =
                new TimingDataContext("timing-node-01", 7L, 12, EFFECTIVE, RECORDED);

        assertEquals("timing-node-01", context.timingNodeId());
        assertEquals(7L, context.sequenceNumber());
        assertEquals(12, context.locationId());
        assertSame(EFFECTIVE, context.effectiveTime());
        assertSame(RECORDED, context.recordedAt());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsZeroSequence() {
        new TimingDataContext("timing-node-01", 0L, 12, EFFECTIVE, RECORDED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonPositiveLocationId() {
        new TimingDataContext("timing-node-01", 1L, 0, EFFECTIVE, RECORDED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsSequenceAboveJsonSafeRange() {
        new TimingDataContext(
                "timing-node-01",
                TimingDataContext.MAX_SEQUENCE_NUMBER + 1L,
                12,
                EFFECTIVE,
                RECORDED);
    }
}
