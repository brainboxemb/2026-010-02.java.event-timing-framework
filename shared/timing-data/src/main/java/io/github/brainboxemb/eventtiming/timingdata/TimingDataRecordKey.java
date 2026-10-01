package io.github.brainboxemb.eventtiming.timingdata;

import java.util.Objects;

/**
 * Stable IF-05 record identity: serialized TimingNode identity plus source sequence number.
 *
 * <p>This shared interchange type deliberately stores the TimingNode identity as the public
 * string value. SI-01 keeps its stronger Domain TimingNodeId type inside the application core and maps
 * that value at the TimingData boundary.</p>
 */
public final class TimingDataRecordKey {
    public static final long MAX_SEQUENCE_NUMBER = TimingDataContext.MAX_SEQUENCE_NUMBER;

    private final String timingNodeId;
    private final long sequenceNumber;

    public TimingDataRecordKey(String timingNodeId, long sequenceNumber) {
        if (timingNodeId == null || timingNodeId.trim().isEmpty()) {
            throw new IllegalArgumentException("timingNodeId must not be blank");
        }
        if (sequenceNumber < 1L || sequenceNumber > MAX_SEQUENCE_NUMBER) {
            throw new IllegalArgumentException(
                    "sequenceNumber must be in range 1.." + MAX_SEQUENCE_NUMBER);
        }
        this.timingNodeId = timingNodeId;
        this.sequenceNumber = sequenceNumber;
    }

    public String timingNodeId() {
        return timingNodeId;
    }

    public long sequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TimingDataRecordKey)) {
            return false;
        }
        TimingDataRecordKey that = (TimingDataRecordKey) other;
        return sequenceNumber == that.sequenceNumber
                && timingNodeId.equals(that.timingNodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timingNodeId, sequenceNumber);
    }

    @Override
    public String toString() {
        return timingNodeId + ":" + sequenceNumber;
    }
}
