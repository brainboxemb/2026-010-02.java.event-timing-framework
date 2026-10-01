package io.github.brainboxemb.eventtiming.timingdata;

/**
 * Checked codec failure with enough semantic context to distinguish malformed
 * data from supported-version compatibility cases.
 */
public final class TimingDataCodecException extends Exception {

    public enum Reason {
        INVALID_DATA,
        UNSUPPORTED_VERSION,
        UNSUPPORTED_RECORD_TYPE,
        ENCODE_FAILURE
    }

    private final Reason reason;
    private final Integer version;
    private final TimingDataRecordKey key;
    private final Integer locationId;
    private final String recordType;
    private final TimingTimestamp effectiveTime;
    private final TimingTimestamp recordedAt;

    public TimingDataCodecException(Reason reason, String message) {
        this(reason, message, null, null, null, null, null, null, null);
    }

    public TimingDataCodecException(Reason reason, String message, Throwable cause) {
        this(reason, message, cause, null, null, null, null, null, null);
    }

    private TimingDataCodecException(
            Reason reason,
            String message,
            Throwable cause,
            Integer version,
            TimingDataRecordKey key,
            Integer locationId,
            String recordType,
            TimingTimestamp effectiveTime,
            TimingTimestamp recordedAt) {
        super(message, cause);
        if (reason == null) {
            throw new IllegalArgumentException("reason must not be null");
        }
        this.reason = reason;
        this.version = version;
        this.key = key;
        this.locationId = locationId;
        this.recordType = recordType;
        this.effectiveTime = effectiveTime;
        this.recordedAt = recordedAt;
    }

    public static TimingDataCodecException unsupportedVersion(
            int version,
            String message) {
        return new TimingDataCodecException(
                Reason.UNSUPPORTED_VERSION,
                message,
                null,
                version,
                null,
                null,
                null,
                null,
                null);
    }

    /**
     * Reports an unknown record type in an otherwise readable supported-version
     * envelope. The original encoded bytes remain owned by the caller.
     */
    public static TimingDataCodecException unsupportedRecordType(
            int version,
            TimingDataRecordKey key,
            int locationId,
            String recordType,
            TimingTimestamp effectiveTime,
            TimingTimestamp recordedAt,
            String message) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        if (locationId < TimingDataContext.MIN_LOCATION_ID) {
            throw new IllegalArgumentException("locationId must be positive");
        }
        if (recordType == null || recordType.trim().isEmpty()) {
            throw new IllegalArgumentException("recordType must not be blank");
        }
        if (effectiveTime == null) {
            throw new IllegalArgumentException("effectiveTime must not be null");
        }
        if (recordedAt == null) {
            throw new IllegalArgumentException("recordedAt must not be null");
        }
        return new TimingDataCodecException(
                Reason.UNSUPPORTED_RECORD_TYPE,
                message,
                null,
                version,
                key,
                locationId,
                recordType,
                effectiveTime,
                recordedAt);
    }

    public Reason reason() {
        return reason;
    }

    public Integer version() {
        return version;
    }

    public TimingDataRecordKey key() {
        return key;
    }

    public Integer locationId() {
        return locationId;
    }

    public String recordType() {
        return recordType;
    }

    public TimingTimestamp effectiveTime() {
        return effectiveTime;
    }

    public TimingTimestamp recordedAt() {
        return recordedAt;
    }
}
