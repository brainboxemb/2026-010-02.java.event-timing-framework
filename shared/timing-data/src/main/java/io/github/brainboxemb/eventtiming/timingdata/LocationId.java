package io.github.brainboxemb.eventtiming.timingdata;

import java.util.Objects;

/**
 * Shared representation of a timing-location identity.
 *
 * <p>The numeric shape is part of the common TimingData/IF-05 contract. The
 * semantic meaning and the set of LocationIds that are valid for a concrete
 * event or deployment belong to that event/profile configuration, not to this
 * value type.</p>
 *
 * <p>This class therefore enforces only the structural invariant shared by the
 * current protocol: the value is positive.</p>
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
