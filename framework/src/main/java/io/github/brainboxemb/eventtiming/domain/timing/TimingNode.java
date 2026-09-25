package io.github.brainboxemb.eventtiming.domain.timing;

/** Logical timing unit with its own stable identity. */
public final class TimingNode {
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
}
