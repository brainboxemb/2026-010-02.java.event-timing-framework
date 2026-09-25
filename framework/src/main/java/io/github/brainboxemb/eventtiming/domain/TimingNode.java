package io.github.brainboxemb.eventtiming.domain;

/**
 * Minimal logical timing aggregate introduced by the Step-3 configuration slice.
 *
 * <p>Operational lifecycle and timing behaviour are added only when later SIP activities require
 * them. For A02 the real domain responsibility is stable identity.</p>
 */
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
