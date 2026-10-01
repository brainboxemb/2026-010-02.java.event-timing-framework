package io.github.brainboxemb.eventtiming.timingdata;

import java.util.Objects;

/**
 * Positive identity of one timing location.
 *
 * <p>LocationId is part of the common IF-05 TimingData envelope and is also used
 * directly by TimingNode and deployment/profile configuration. Keeping one shared
 * value type prevents the application domain and interchange model from assigning
 * different meaning to the same location identifier.</p>
 */
public final class LocationId {
    private final int value;

    public LocationId(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("LocationId must be positive");
        }
        this.value = value;
    }

    public int value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LocationId)) {
            return false;
        }
        LocationId that = (LocationId) other;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
