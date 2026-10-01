package io.github.brainboxemb.eventtiming.timingpoint.domain.logbook;

import io.github.brainboxemb.eventtiming.timingdata.TimingData;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Passive committed TimingData history owned by one TimingNode serial lane.
 *
 * <p>The LogBook has no worker or independent synchronization. Its owner decides
 * when mutations and snapshots occur.</p>
 */
public final class LogBook {
    private final TimingNodeId timingNodeId;
    private final List<TimingData> records = new ArrayList<>();

    public LogBook(TimingNodeId timingNodeId) {
        if (timingNodeId == null) {
            throw new IllegalArgumentException("timingNodeId must not be null");
        }
        this.timingNodeId = timingNodeId;
    }

    /**
     * Returns the next sequence implied by committed state without consuming it.
     */
    public long nextSequence() {
        if (records.isEmpty()) {
            return 1L;
        }
        long last = records.get(records.size() - 1).sequenceNumber();
        if (last >= TimingData.MAX_SEQUENCE_NUMBER) {
            throw new IllegalStateException("TimingData sequence space is exhausted");
        }
        return last + 1L;
    }

    /**
     * Adds one already committed TimingData value.
     */
    public void add(TimingData data) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        if (!timingNodeId.value().equals(data.timingNodeId())) {
            throw new IllegalArgumentException(
                    "TimingData belongs to a different TimingNode: " + data.timingNodeId());
        }

        long expectedSequence = nextSequence();
        if (data.sequenceNumber() != expectedSequence) {
            throw new IllegalArgumentException(
                    "TimingData sequence must be " + expectedSequence
                            + " but was " + data.sequenceNumber());
        }
        records.add(data);
    }

    public int size() {
        return records.size();
    }

    /**
     * Returns a stable shallow immutable view for calculation outside the owner lane.
     */
    public List<TimingData> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(records));
    }
}
