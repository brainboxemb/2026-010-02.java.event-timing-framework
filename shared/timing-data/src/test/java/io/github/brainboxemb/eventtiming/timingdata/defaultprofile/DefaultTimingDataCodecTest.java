package io.github.brainboxemb.eventtiming.timingdata.defaultprofile;

import io.github.brainboxemb.eventtiming.timingdata.LocationId;
import io.github.brainboxemb.eventtiming.timingdata.RegistrationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingData;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataCodec;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataFactory;
import io.github.brainboxemb.eventtiming.timingdata.TimingTimestamp;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DefaultTimingDataCodecTest {
    private static final TimingTimestamp EFFECTIVE =
            TimingTimestamp.parse("2026-09-30T20:01:39.123000000Z");
    private static final TimingTimestamp RECORDED =
            TimingTimestamp.parse("2026-09-30T20:01:45.456000000Z");

    private final DefaultTimingDataFactory factory = new DefaultTimingDataFactory();
    private final DefaultTimingDataCodec codec = new DefaultTimingDataCodec();

    @Test
    public void encodesAutomaticRegistrationInCanonicalMemberOrder() throws Exception {
        TimingData data = factory.createAutomaticRegistration(
                context(1L),
                new RegistrationId("registration-0042"));

        String json = new String(codec.encode(data), StandardCharsets.UTF_8);

        assertEquals(
                "{\"version\":1,"
                        + "\"timingNodeId\":\"timing-node-01\","
                        + "\"sequenceNumber\":1,"
                        + "\"locationId\":7,"
                        + "\"recordType\":\"REGISTRATION\","
                        + "\"effectiveTime\":\"2026-09-30T20:01:39.123000000Z\","
                        + "\"recordedAt\":\"2026-09-30T20:01:45.456000000Z\","
                        + "\"registrationId\":\"registration-0042\","
                        + "\"origin\":\"AUTOMATIC\","
                        + "\"timeSource\":\"OBSERVED\"}",
                json);
        assertFalse(json.endsWith("\n"));
    }

    @Test
    public void encodesAndDecodesManualRegistration() throws Exception {
        TimingData.ManualRegistration original =
                factory.createManualRegistration(
                        context(2L),
                        new RegistrationId("registration-\"0042"),
                        TimingData.ManualTimeSource.OPERATOR_ENTERED);

        TimingData decoded = codec.decode(codec.encode(original));

        assertTrue(decoded instanceof TimingData.ManualRegistration);
        TimingData.ManualRegistration manual =
                (TimingData.ManualRegistration) decoded;
        assertCommon(manual, 2L);
        assertEquals(
                new RegistrationId("registration-\"0042"),
                manual.registrationId());
        assertSame(
                TimingData.ManualTimeSource.OPERATOR_ENTERED,
                manual.timeSource());
    }

    @Test
    public void decodesAutomaticRegistration() throws Exception {
        TimingData decoded = codec.decode(json(
                "{"
                        + "\"version\":1,"
                        + "\"timingNodeId\":\"timing-node-01\","
                        + "\"sequenceNumber\":1,"
                        + "\"locationId\":7,"
                        + "\"recordType\":\"REGISTRATION\","
                        + "\"effectiveTime\":\"2026-09-30T20:01:39.123000000Z\","
                        + "\"recordedAt\":\"2026-09-30T20:01:45.456000000Z\","
                        + "\"registrationId\":\"registration-0042\","
                        + "\"origin\":\"AUTOMATIC\","
                        + "\"timeSource\":\"OBSERVED\""
                        + "}"));

        assertTrue(decoded instanceof TimingData.AutomaticRegistration);
        TimingData.AutomaticRegistration automatic =
                (TimingData.AutomaticRegistration) decoded;
        assertCommon(automatic, 1L);
        assertEquals(
                new RegistrationId("registration-0042"),
                automatic.registrationId());
    }

    @Test
    public void readerIgnoresAdditionalMembersAndMemberOrder() throws Exception {
        TimingData decoded = codec.decode(json(
                "{"
                        + "\"extra\":{\"future\":[1,2,3]},"
                        + "\"registrationId\":\"registration-0042\","
                        + "\"recordedAt\":\"2026-09-30T20:01:45.456000000Z\","
                        + "\"version\":1,"
                        + "\"origin\":\"MANUAL\","
                        + "\"locationId\":7,"
                        + "\"sequenceNumber\":2,"
                        + "\"timeSource\":\"SYSTEM_ASSIGNED\","
                        + "\"recordType\":\"REGISTRATION\","
                        + "\"effectiveTime\":\"2026-09-30T20:01:39.123000000Z\","
                        + "\"timingNodeId\":\"timing-node-01\""
                        + "}"));

        assertTrue(decoded instanceof TimingData.ManualRegistration);
        assertSame(
                TimingData.ManualTimeSource.SYSTEM_ASSIGNED,
                ((TimingData.ManualRegistration) decoded).timeSource());
    }

    @Test
    public void unsupportedVersionIsReportedSeparately() throws Exception {
        try {
            codec.decode(json("{\"version\":2}"));
            fail("expected unsupported version");
        } catch (TimingDataCodec.CodecException expected) {
            assertSame(
                    TimingDataCodec.CodecException.Reason.UNSUPPORTED_VERSION,
                    expected.reason());
            assertEquals(Integer.valueOf(2), expected.version());
        }
    }

    @Test
    public void unknownV1RecordTypeRetainsReadableCommonEnvelope() throws Exception {
        try {
            codec.decode(json(
                    "{"
                            + "\"version\":1,"
                            + "\"timingNodeId\":\"timing-node-01\","
                            + "\"sequenceNumber\":9,"
                            + "\"locationId\":7,"
                            + "\"recordType\":\"FUTURE_RECORD\","
                            + "\"effectiveTime\":\"2026-09-30T20:01:39.123000000Z\","
                            + "\"recordedAt\":\"2026-09-30T20:01:45.456000000Z\""
                            + "}"));
            fail("expected unsupported record type");
        } catch (TimingDataCodec.CodecException expected) {
            assertSame(
                    TimingDataCodec.CodecException.Reason.UNSUPPORTED_RECORD_TYPE,
                    expected.reason());
            assertEquals(
                    new TimingData.RecordKey("timing-node-01", 9L),
                    expected.key());
            assertEquals(new LocationId(7), expected.locationId());
            assertEquals("FUTURE_RECORD", expected.recordType());
            assertEquals(EFFECTIVE, expected.effectiveTime());
            assertEquals(RECORDED, expected.recordedAt());
        }
    }

    @Test
    public void malformedOrSemanticallyInvalidRecordsAreInvalidData() throws Exception {
        assertInvalid(json("{\"version\":1"));
        assertInvalid(json(
                "{"
                        + "\"version\":1,"
                        + "\"timingNodeId\":\"timing-node-01\","
                        + "\"sequenceNumber\":1,"
                        + "\"locationId\":7,"
                        + "\"recordType\":\"REGISTRATION\","
                        + "\"effectiveTime\":\"2026-09-30T20:01:39.123000000Z\","
                        + "\"recordedAt\":\"2026-09-30T20:01:45.456000000Z\","
                        + "\"registrationId\":\"registration-0042\","
                        + "\"origin\":\"AUTOMATIC\","
                        + "\"timeSource\":\"OPERATOR_ENTERED\""
                        + "}"));
        assertInvalid(json(
                "{"
                        + "\"version\":1,"
                        + "\"version\":1,"
                        + "\"timingNodeId\":\"timing-node-01\""
                        + "}"));
    }

    @Test
    public void rejectsUtf8Bom() throws Exception {
        byte[] body = json("{\"version\":1}");
        byte[] encoded = new byte[body.length + 3];
        encoded[0] = (byte) 0xef;
        encoded[1] = (byte) 0xbb;
        encoded[2] = (byte) 0xbf;
        System.arraycopy(body, 0, encoded, 3, body.length);

        assertInvalid(encoded);
    }

    private static TimingDataFactory.Context context(long sequence) {
        return new TimingDataFactory.Context(
                "timing-node-01",
                sequence,
                new LocationId(7),
                EFFECTIVE,
                RECORDED);
    }

    private static byte[] json(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static void assertCommon(TimingData data, long sequence) {
        assertEquals("timing-node-01", data.timingNodeId());
        assertEquals(sequence, data.sequenceNumber());
        assertEquals(new LocationId(7), data.locationId());
        assertEquals(EFFECTIVE, data.effectiveTime());
        assertEquals(RECORDED, data.recordedAt());
    }

    private void assertInvalid(byte[] encoded) throws Exception {
        try {
            codec.decode(encoded);
            fail("expected invalid data");
        } catch (TimingDataCodec.CodecException expected) {
            assertSame(
                    TimingDataCodec.CodecException.Reason.INVALID_DATA,
                    expected.reason());
        }
    }
}
