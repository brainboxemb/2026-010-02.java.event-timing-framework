package io.github.brainboxemb.eventtiming.timingdata;

/**
 * Stable configured software/source identity of one TimingNode.
 *
 * <p>The shared library owns the value representation used by TimingData and
 * other Java boundaries. Deployment configuration still chooses the actual
 * TimingNode identity; this type does not define deployment topology or event
 * policy.</p>
 */
public final class TimingNodeId {
    private final String value;

    public TimingNodeId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("TimingNodeId must not be blank");
        }
        this.value = value.trim();
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TimingNodeId)) {
            return false;
        }
        TimingNodeId that = (TimingNodeId) other;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
