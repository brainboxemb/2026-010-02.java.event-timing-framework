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
    private final String recordType;

    public TimingDataCodecException(Reason reason, String message) {
        this(reason, message, null, null, null, null);
    }

    public TimingDataCodecException(Reason reason, String message, Throwable cause) {
        this(reason, message, cause, null, null, null);
    }

    private TimingDataCodecException(
            Reason reason,
            String message,
            Throwable cause,
            Integer version,
            TimingDataRecordKey key,
            String recordType) {
        super(message, cause);
        if (reason == null) {
            throw new IllegalArgumentException("reason must not be null");
        }
        this.reason = reason;
        this.version = version;
        this.key = key;
        this.recordType = recordType;
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
                null);
    }

    public static TimingDataCodecException unsupportedRecordType(
            int version,
            TimingDataRecordKey key,
            String recordType,
            String message) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        if (recordType == null || recordType.trim().isEmpty()) {
            throw new IllegalArgumentException("recordType must not be blank");
        }
        return new TimingDataCodecException(
                Reason.UNSUPPORTED_RECORD_TYPE,
                message,
                null,
                version,
                key,
                recordType);
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

    public String recordType() {
        return recordType;
    }
}
