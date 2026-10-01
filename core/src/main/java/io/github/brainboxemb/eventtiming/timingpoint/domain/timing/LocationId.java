package io.github.brainboxemb.eventtiming.timingpoint.domain.timing;

/** Positive configured identity of one timing location. */
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
        return Integer.valueOf(value).hashCode();
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
