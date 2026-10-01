package io.github.brainboxemb.eventtiming.timingdata;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class TimingDataCodecExceptionTest {

    @Test
    public void unsupportedVersionCarriesVersionWithoutPretendingV1Envelope() {
        TimingDataCodecException failure =
                TimingDataCodecException.unsupportedVersion(2, "unsupported version");

        assertSame(TimingDataCodecException.Reason.UNSUPPORTED_VERSION, failure.reason());
        assertEquals(Integer.valueOf(2), failure.version());
        assertNull(failure.key());
        assertNull(failure.recordType());
    }

    @Test
    public void unsupportedV1RecordTypeCarriesKnownSourceContext() {
        TimingDataRecordKey key =
                new TimingDataRecordKey("timing-node-01", 7);

        TimingDataCodecException failure =
                TimingDataCodecException.unsupportedRecordType(
                        1,
                        key,
                        "FUTURE_RECORD",
                        "unsupported record type");

        assertSame(
                TimingDataCodecException.Reason.UNSUPPORTED_RECORD_TYPE,
                failure.reason());
        assertEquals(Integer.valueOf(1), failure.version());
        assertEquals(key, failure.key());
        assertEquals("FUTURE_RECORD", failure.recordType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void unsupportedRecordTypeRequiresKey() {
        TimingDataCodecException.unsupportedRecordType(
                1,
                null,
                "FUTURE_RECORD",
                "unsupported record type");
    }
}
