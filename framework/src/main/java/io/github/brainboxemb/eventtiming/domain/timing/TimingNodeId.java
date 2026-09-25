package io.github.brainboxemb.eventtiming.domain.timing;

/** Stable software identity of one {@link TimingNode}. */
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
