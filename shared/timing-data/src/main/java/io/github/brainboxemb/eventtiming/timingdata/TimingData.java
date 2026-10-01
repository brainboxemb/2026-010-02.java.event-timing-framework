package io.github.brainboxemb.eventtiming.timingdata;

import java.util.Objects;

/**
 * Common immutable TimingData contract shared by all concrete profiles.
 *
 * <p>The top-level interface exposes the common IF-05 envelope. Small semantic
 * types that only make sense as part of TimingData are grouped here so consumers
 * can discover the public model from one place instead of navigating many
 * one-type source files.</p>
 *
 * <p>Typical usage:</p>
 *
 * <pre>{@code
 * TimingData data = ...;
 *
 * if (data instanceof TimingData.ManualRegistration) {
 *     TimingData.ManualRegistration manual =
 *             (TimingData.ManualRegistration) data;
 *     RegistrationId registrationId = manual.registrationId();
 *     TimingData.ManualTimeSource source = manual.timeSource();
 * }
 * }</pre>
 */
public interface TimingData {
    /** Smallest valid public IF-05 LocationId value. */
    int MIN_LOCATION_ID = 1;

    /** Largest JSON-safe IF-05 sequence value: 2^53 - 1. */
    long MAX_SEQUENCE_NUMBER = 9007199254740991L;

    String timingNodeId();

    long sequenceNumber();

    int locationId();

    TimingTimestamp effectiveTime();

    TimingTimestamp recordedAt();

    /**
     * Type-safe semantic contract for one automatic/tag registration.
     *
     * <p>The effective time is the accepted observed time by definition, so no
     * separate origin or time-source property is required on this interface.</p>
     */
    interface AutomaticRegistration extends TimingData {
        RegistrationId registrationId();
    }

    /** Type-safe semantic contract for one manual registration. */
    interface ManualRegistration extends TimingData {
        RegistrationId registrationId();

        ManualTimeSource timeSource();
    }

    /** Source of the effective time selected for a manual registration. */
    enum ManualTimeSource {
        SYSTEM_ASSIGNED,
        OPERATOR_ENTERED
    }

    /**
     * Stable IF-05 record identity: serialized TimingNode identity plus source
     * sequence number.
     *
     * <p>The shared interchange type deliberately stores TimingNode identity as
     * its public string value. SI-01 retains its stronger domain TimingNodeId and
     * maps that value at the TimingData boundary.</p>
     */
    final class RecordKey {
        private final String timingNodeId;
        private final long sequenceNumber;

        public RecordKey(String timingNodeId, long sequenceNumber) {
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
            if (!(other instanceof RecordKey)) {
                return false;
            }
            RecordKey that = (RecordKey) other;
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
}
