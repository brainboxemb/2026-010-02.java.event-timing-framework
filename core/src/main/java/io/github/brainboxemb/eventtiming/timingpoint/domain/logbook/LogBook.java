package io.github.brainboxemb.eventtiming.timingpoint.domain.logbook;

import io.github.brainboxemb.eventtiming.timingdata.TimingData;
import io.github.brainboxemb.eventtiming.timingdata.TimingNodeId;

import java.util.ArrayList;

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
        if (!timingNodeId.equals(data.timingNodeId())) {
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
     * Visits at most {@code limit} committed records starting at the inclusive
     * source sequence without creating an intermediate List.
     */
    public <R> R visitRange(
            long fromSequence,
            int limit,
            LogBookVisitor<R> visitor) {
        if (fromSequence < 1L) {
            throw new IllegalArgumentException("fromSequence must be >= 1");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be >= 1");
        }
        if (visitor == null) {
            throw new IllegalArgumentException("visitor must not be null");
        }

        long startLong = fromSequence - 1L;
        int totalCount = records.size();
        if (startLong >= totalCount) {
            visitor.begin(totalCount, null);
            return visitor.finish();
        }

        int start = (int) startLong;
        int end = (int) Math.min((long) totalCount, startLong + limit);
        Long nextSequence =
                end < totalCount ? Long.valueOf((long) end + 1L) : null;

        visitor.begin(totalCount, nextSequence);
        for (int index = start; index < end; index++) {
            visitor.visit(records.get(index));
        }
        return visitor.finish();
    }

    /**
     * Visits at most {@code limit} newest committed records in source order
     * without creating an intermediate List.
     */
    public <R> R visitLatest(int limit, LogBookVisitor<R> visitor) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be >= 1");
        }
        if (visitor == null) {
            throw new IllegalArgumentException("visitor must not be null");
        }

        int totalCount = records.size();
        int start = Math.max(0, totalCount - limit);
        visitor.begin(totalCount, null);
        for (int index = start; index < totalCount; index++) {
            visitor.visit(records.get(index));
        }
        return visitor.finish();
    }

    /**
     * Visits the complete committed LogBook without creating an intermediate List.
     */
    public <R> R visitAll(LogBookVisitor<R> visitor) {
        if (visitor == null) {
            throw new IllegalArgumentException("visitor must not be null");
        }

        int totalCount = records.size();
        visitor.begin(totalCount, null);
        for (TimingData data : records) {
            visitor.visit(data);
        }
        return visitor.finish();
    }
}
