package io.github.brainboxemb.eventtiming.domain.timing;

/** Logical timing unit with its own stable identity and lifecycle. */
public final class TimingNode {
    public enum Lifecycle {
        CLOSED
    }

    private final TimingNodeId timingNodeId;

    public TimingNode(TimingNodeId timingNodeId) {
        if (timingNodeId == null) {
            throw new IllegalArgumentException("timingNodeId must not be null");
        }
        this.timingNodeId = timingNodeId;
    }

    public TimingNodeId timingNodeId() {
        return timingNodeId;
    }

    public Lifecycle lifecycle() {
        return Lifecycle.CLOSED;
    }
}
