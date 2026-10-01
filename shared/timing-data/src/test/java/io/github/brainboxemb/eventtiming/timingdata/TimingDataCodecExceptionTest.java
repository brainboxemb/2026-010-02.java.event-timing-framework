package io.github.brainboxemb.eventtiming.timingdata;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class TimingDataCodecExceptionTest {
    private static final TimingTimestamp EFFECTIVE =
            TimingTimestamp.parse("2026-09-30T20:01:39.123000000Z");
    private static final TimingTimestamp RECORDED =
            TimingTimestamp.parse("2026-09-30T20:01:40.000000000Z");

    @Test
    public void unsupportedVersionCarriesVersionWithoutPretendingV1Envelope() {
        TimingDataCodecException failure =
                TimingDataCodecException.unsupportedVersion(2, "unsupported version");

        assertSame(TimingDataCodecException.Reason.UNSUPPORTED_VERSION, failure.reason());
        assertEquals(Integer.valueOf(2), failure.version());
        assertNull(failure.key());
        assertNull(failure.locationId());
        assertNull(failure.recordType());
        assertNull(failure.effectiveTime());
        assertNull(failure.recordedAt());
    }

    @Test
    public void unsupportedV1RecordTypeCarriesReadableCommonEnvelope() {
        TimingDataRecordKey key =
                new TimingDataRecordKey("timing-node-01", 7);

        TimingDataCodecException failure =
                TimingDataCodecException.unsupportedRecordType(
                        1,
                        key,
                        12,
                        "FUTURE_RECORD",
                        EFFECTIVE,
                        RECORDED,
                        "unsupported record type");

        assertSame(
                TimingDataCodecException.Reason.UNSUPPORTED_RECORD_TYPE,
                failure.reason());
        assertEquals(Integer.valueOf(1), failure.version());
        assertEquals(key, failure.key());
        assertEquals(Integer.valueOf(12), failure.locationId());
        assertEquals("FUTURE_RECORD", failure.recordType());
        assertEquals(EFFECTIVE, failure.effectiveTime());
        assertEquals(RECORDED, failure.recordedAt());
    }

    @Test(expected = IllegalArgumentException.class)
    public void unsupportedRecordTypeRequiresKey() {
        TimingDataCodecException.unsupportedRecordType(
                1,
                null,
                12,
                "FUTURE_RECORD",
                EFFECTIVE,
                RECORDED,
                "unsupported record type");
    }
}
