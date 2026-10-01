package io.github.brainboxemb.eventtiming.timingdata;

import java.util.Objects;

/**
 * Immutable Java representation of one committed IF-05 TimingData v1 record.
 *
 * <p>Use the record-family factory methods so type-specific fields cannot be
 * combined with the wrong record type.</p>
 */
public final class TimingDataRecord {
    public static final int VERSION = 1;
    public static final int MIN_LOCATION_ID = 1;
    public static final int MAX_LOCATION_ID = 25;

    private final TimingDataRecordKey key;
    private final int locationId;
    private final TimingDataRecordType recordType;
    private final TimingTimestamp effectiveTime;
    private final TimingTimestamp recordedAt;
    private final TimingNodeState state;
    private final RegistrationIdentity registrationIdentity;
    private final RegistrationOrigin origin;
    private final RegistrationTimeSource timeSource;
    private final TimingDataRecordKey reference;

    private TimingDataRecord(
            TimingDataRecordKey key,
            int locationId,
            TimingDataRecordType recordType,
            TimingTimestamp effectiveTime,
            TimingTimestamp recordedAt,
            TimingNodeState state,
            RegistrationIdentity registrationIdentity,
            RegistrationOrigin origin,
            RegistrationTimeSource timeSource,
            TimingDataRecordKey reference) {
        this.key = requireValue(key, "key");
        validateLocationId(locationId);
        this.locationId = locationId;
        this.recordType = requireValue(recordType, "recordType");
        this.effectiveTime = requireValue(effectiveTime, "effectiveTime");
        this.recordedAt = requireValue(recordedAt, "recordedAt");
        this.state = state;
        this.registrationIdentity = registrationIdentity;
        this.origin = origin;
        this.timeSource = timeSource;
        this.reference = reference;
    }

    public static TimingDataRecord timingNodeState(
            TimingDataRecordKey key,
            int locationId,
            TimingTimestamp effectiveTime,
            TimingTimestamp recordedAt,
            TimingNodeState state) {
        return new TimingDataRecord(
                key,
                locationId,
                TimingDataRecordType.TIMING_NODE_STATE,
                effectiveTime,
                recordedAt,
                requireValue(state, "state"),
                null,
                null,
                null,
                null);
    }

    public static TimingDataRecord registration(
            TimingDataRecordKey key,
            int locationId,
            TimingTimestamp effectiveTime,
            TimingTimestamp recordedAt,
            RegistrationIdentity registrationIdentity,
            RegistrationOrigin origin,
            RegistrationTimeSource timeSource) {
        validateRegistration(locationId, registrationIdentity, origin, timeSource);
        return new TimingDataRecord(
                key,
                locationId,
                TimingDataRecordType.REGISTRATION,
                effectiveTime,
                recordedAt,
                null,
                registrationIdentity,
                origin,
                timeSource,
                null);
    }

    public static TimingDataRecord registrationRevoked(
            TimingDataRecordKey key,
            int locationId,
            TimingTimestamp effectiveTime,
            TimingTimestamp recordedAt,
            RegistrationIdentity registrationIdentity,
            RegistrationOrigin origin,
            RegistrationTimeSource timeSource,
            TimingDataRecordKey reference) {
        validateRegistration(locationId, registrationIdentity, origin, timeSource);
        validateReference(key, reference);
        return new TimingDataRecord(
                key,
                locationId,
                TimingDataRecordType.REGISTRATION_REVOKED,
                effectiveTime,
                recordedAt,
                null,
                registrationIdentity,
                origin,
                timeSource,
                reference);
    }

    public int version() {
        return VERSION;
    }

    public TimingDataRecordKey key() {
        return key;
    }

    public String timingNodeId() {
        return key.timingNodeId();
    }

    public long sequenceNumber() {
        return key.sequenceNumber();
    }

    public int locationId() {
        return locationId;
    }

    public TimingDataRecordType recordType() {
        return recordType;
    }

    public TimingTimestamp effectiveTime() {
        return effectiveTime;
    }

    public TimingTimestamp recordedAt() {
        return recordedAt;
    }

    /** Returns the state for TIMING_NODE_STATE, otherwise null. */
    public TimingNodeState state() {
        return state;
    }

    /** Returns the participant identity for registration records, otherwise null. */
    public RegistrationIdentity registrationIdentity() {
        return registrationIdentity;
    }

    /** Returns the registration origin for registration records, otherwise null. */
    public RegistrationOrigin origin() {
        return origin;
    }

    /** Returns the registration time source for registration records, otherwise null. */
    public RegistrationTimeSource timeSource() {
        return timeSource;
    }

    /** Returns the referenced registration key for REGISTRATION_REVOKED, otherwise null. */
    public TimingDataRecordKey reference() {
        return reference;
    }

    private static void validateLocationId(int locationId) {
        if (locationId < MIN_LOCATION_ID || locationId > MAX_LOCATION_ID) {
            throw new IllegalArgumentException(
                    "locationId must be in range " + MIN_LOCATION_ID + ".." + MAX_LOCATION_ID);
        }
    }

    private static void validateRegistration(
            int locationId,
            RegistrationIdentity registrationIdentity,
            RegistrationOrigin origin,
            RegistrationTimeSource timeSource) {
        validateLocationId(locationId);
        RegistrationIdentity identity = requireValue(
                registrationIdentity,
                "registrationIdentity");
        RegistrationOrigin registrationOrigin = requireValue(origin, "origin");
        RegistrationTimeSource registrationTimeSource = requireValue(
                timeSource,
                "timeSource");

        if (!identity.supportsLocationId(locationId)) {
            throw new IllegalArgumentException(
                    "registrationIdentity " + identity + " is not valid for locationId " + locationId);
        }
        if (registrationOrigin == RegistrationOrigin.AUTOMATIC
                && registrationTimeSource != RegistrationTimeSource.OBSERVED) {
            throw new IllegalArgumentException(
                    "AUTOMATIC registration requires OBSERVED timeSource");
        }
        if (registrationOrigin == RegistrationOrigin.MANUAL
                && registrationTimeSource == RegistrationTimeSource.OBSERVED) {
            throw new IllegalArgumentException(
                    "MANUAL registration requires SYSTEM_ASSIGNED or OPERATOR_ENTERED timeSource");
        }
    }

    private static void validateReference(
            TimingDataRecordKey key,
            TimingDataRecordKey reference) {
        TimingDataRecordKey recordKey = requireValue(key, "key");
        TimingDataRecordKey registrationReference = requireValue(reference, "reference");
        if (!recordKey.timingNodeId().equals(registrationReference.timingNodeId())) {
            throw new IllegalArgumentException(
                    "reference must belong to the same timingNodeId stream");
        }
        if (registrationReference.sequenceNumber() >= recordKey.sequenceNumber()) {
            throw new IllegalArgumentException(
                    "reference sequence must be lower than revocation sequence");
        }
    }

    private static <T> T requireValue(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TimingDataRecord)) {
            return false;
        }
        TimingDataRecord that = (TimingDataRecord) other;
        return locationId == that.locationId
                && key.equals(that.key)
                && recordType == that.recordType
                && effectiveTime.equals(that.effectiveTime)
                && recordedAt.equals(that.recordedAt)
                && state == that.state
                && Objects.equals(registrationIdentity, that.registrationIdentity)
                && origin == that.origin
                && timeSource == that.timeSource
                && Objects.equals(reference, that.reference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                key,
                locationId,
                recordType,
                effectiveTime,
                recordedAt,
                state,
                registrationIdentity,
                origin,
                timeSource,
                reference);
    }
}
