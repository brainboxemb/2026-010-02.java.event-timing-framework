package io.github.brainboxemb.eventtiming.timingdata;

/**
 * Value-only construction context for fields shared by every TimingData value.
 *
 * <p>The context contains no TimingNode, LogBook, store or other mutable
 * collaborator. Stateful policy and sequence selection happen before this
 * object is created.</p>
 */
public final class TimingDataContext {
    public static final int MIN_LOCATION_ID = 1;
    public static final long MAX_SEQUENCE_NUMBER = 9007199254740991L;

    private final String timingNodeId;
    private final long sequenceNumber;
    private final int locationId;
    private final TimingTimestamp effectiveTime;
    private final TimingTimestamp recordedAt;

    public TimingDataContext(
            String timingNodeId,
            long sequenceNumber,
            int locationId,
            TimingTimestamp effectiveTime,
            TimingTimestamp recordedAt) {
        if (timingNodeId == null || timingNodeId.trim().isEmpty()) {
            throw new IllegalArgumentException("timingNodeId must not be blank");
        }
        if (sequenceNumber < 1L || sequenceNumber > MAX_SEQUENCE_NUMBER) {
            throw new IllegalArgumentException(
                    "sequenceNumber must be in range 1.." + MAX_SEQUENCE_NUMBER);
        }
        if (locationId < MIN_LOCATION_ID) {
            throw new IllegalArgumentException("locationId must be positive");
        }
        if (effectiveTime == null) {
            throw new IllegalArgumentException("effectiveTime must not be null");
        }
        if (recordedAt == null) {
            throw new IllegalArgumentException("recordedAt must not be null");
        }
        this.timingNodeId = timingNodeId;
        this.sequenceNumber = sequenceNumber;
        this.locationId = locationId;
        this.effectiveTime = effectiveTime;
        this.recordedAt = recordedAt;
    }

    public String timingNodeId() {
        return timingNodeId;
    }

    public long sequenceNumber() {
        return sequenceNumber;
    }

    public int locationId() {
        return locationId;
    }

    public TimingTimestamp effectiveTime() {
        return effectiveTime;
    }

    public TimingTimestamp recordedAt() {
        return recordedAt;
    }
}
