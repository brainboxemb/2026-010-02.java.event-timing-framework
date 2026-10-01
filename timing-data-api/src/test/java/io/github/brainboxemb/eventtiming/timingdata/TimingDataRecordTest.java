package io.github.brainboxemb.eventtiming.timingdata;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class TimingDataRecordTest {
    private static final TimingTimestamp EFFECTIVE =
            TimingTimestamp.parse("2026-09-30T20:01:39.123000000Z");
    private static final TimingTimestamp RECORDED =
            TimingTimestamp.parse("2026-09-30T20:02:05.456000000Z");

    @Test
    public void createsLifecycleRecordWithOnlyLifecyclePayload() {
        TimingDataRecord record = TimingDataRecord.timingNodeState(
                key(1),
                7,
                EFFECTIVE,
                RECORDED,
                TimingNodeState.OPEN);

        assertEquals(1, record.version());
        assertEquals("timing-node-01", record.timingNodeId());
        assertEquals(1L, record.sequenceNumber());
        assertEquals(7, record.locationId());
        assertSame(TimingDataRecordType.TIMING_NODE_STATE, record.recordType());
        assertSame(TimingNodeState.OPEN, record.state());
        assertNull(record.registrationIdentity());
        assertNull(record.reference());
    }

    @Test
    public void createsAutomaticObservedRegistration() {
        RegistrationIdentity identity =
                new RegistrationIdentity(RegistrationIdentity.Type.STANDARD, 42);

        TimingDataRecord record = TimingDataRecord.registration(
                key(2),
                7,
                EFFECTIVE,
                RECORDED,
                identity,
                RegistrationOrigin.AUTOMATIC,
                RegistrationTimeSource.OBSERVED);

        assertSame(TimingDataRecordType.REGISTRATION, record.recordType());
        assertEquals(identity, record.registrationIdentity());
        assertSame(RegistrationOrigin.AUTOMATIC, record.origin());
        assertSame(RegistrationTimeSource.OBSERVED, record.timeSource());
        assertNull(record.state());
        assertNull(record.reference());
    }

    @Test
    public void createsRevocationOfEarlierRegistrationInSameStream() {
        RegistrationIdentity identity =
                new RegistrationIdentity(RegistrationIdentity.Type.STANDARD, 42);

        TimingDataRecord record = TimingDataRecord.registrationRevoked(
                key(3),
                7,
                EFFECTIVE,
                RECORDED,
                identity,
                RegistrationOrigin.AUTOMATIC,
                RegistrationTimeSource.OBSERVED,
                key(2));

        assertSame(TimingDataRecordType.REGISTRATION_REVOKED, record.recordType());
        assertEquals(key(2), record.reference());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsLocationOutsideIf05Range() {
        TimingDataRecord.timingNodeState(
                key(1),
                0,
                EFFECTIVE,
                RECORDED,
                TimingNodeState.OPEN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsIdentityAtIncompatibleLocation() {
        TimingDataRecord.registration(
                key(1),
                24,
                EFFECTIVE,
                RECORDED,
                new RegistrationIdentity(RegistrationIdentity.Type.STANDARD, 42),
                RegistrationOrigin.AUTOMATIC,
                RegistrationTimeSource.OBSERVED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsAutomaticRegistrationWithoutObservedTime() {
        TimingDataRecord.registration(
                key(1),
                7,
                EFFECTIVE,
                RECORDED,
                new RegistrationIdentity(RegistrationIdentity.Type.STANDARD, 42),
                RegistrationOrigin.AUTOMATIC,
                RegistrationTimeSource.SYSTEM_ASSIGNED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsManualRegistrationWithObservedTime() {
        TimingDataRecord.registration(
                key(1),
                7,
                EFFECTIVE,
                RECORDED,
                new RegistrationIdentity(RegistrationIdentity.Type.STANDARD, 42),
                RegistrationOrigin.MANUAL,
                RegistrationTimeSource.OBSERVED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsRevocationReferenceFromOtherStream() {
        TimingDataRecord.registrationRevoked(
                key(3),
                7,
                EFFECTIVE,
                RECORDED,
                new RegistrationIdentity(RegistrationIdentity.Type.STANDARD, 42),
                RegistrationOrigin.AUTOMATIC,
                RegistrationTimeSource.OBSERVED,
                new TimingDataRecordKey("timing-node-02", 2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsRevocationReferenceToSameOrLaterSequence() {
        TimingDataRecord.registrationRevoked(
                key(3),
                7,
                EFFECTIVE,
                RECORDED,
                new RegistrationIdentity(RegistrationIdentity.Type.STANDARD, 42),
                RegistrationOrigin.AUTOMATIC,
                RegistrationTimeSource.OBSERVED,
                key(3));
    }

    private static TimingDataRecordKey key(long sequence) {
        return new TimingDataRecordKey("timing-node-01", sequence);
    }
}
