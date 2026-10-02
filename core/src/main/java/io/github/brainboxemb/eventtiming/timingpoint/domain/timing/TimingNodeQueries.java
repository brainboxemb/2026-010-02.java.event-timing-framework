package io.github.brainboxemb.eventtiming.timingpoint.domain.timing;

import io.github.brainboxemb.eventtiming.timingpoint.domain.logbook.LogBookVisitor;

/**
 * Standard read operations supported by TimingNode.
 *
 * <p>Queries describe read intent. Bulk LogBook queries accept a visitor so the
 * caller can build the required result directly while the selected records are
 * visited on the TimingNode serial lane; no intermediate record List is required.</p>
 */
public final class TimingNodeQueries {
    private static final TimingNodeQuery<TimingNode.Status> STATUS =
            new TimingNodeQuery<>("status", false, TimingNodeLogic::status);
    private static final TimingNodeQuery<Integer> TIMING_DATA_COUNT =
            new TimingNodeQuery<>(
                    "timingDataCount",
                    true,
                    TimingNodeLogic::timingDataCount);

    private TimingNodeQueries() {
    }

    public static TimingNodeQuery<TimingNode.Status> status() {
        return STATUS;
    }

    public static TimingNodeQuery<Integer> timingDataCount() {
        return TIMING_DATA_COUNT;
    }

    public static <R> TimingNodeQuery<R> timingData(
            LogBookVisitor<R> visitor) {
        requireVisitor(visitor);
        return new TimingNodeQuery<>(
                "timingData",
                true,
                logic -> logic.visitTimingData(visitor));
    }

    public static <R> TimingNodeQuery<R> timingDataRange(
            long fromSequence,
            int limit,
            LogBookVisitor<R> visitor) {
        requireVisitor(visitor);
        return new TimingNodeQuery<>(
                "timingDataRange",
                true,
                logic -> logic.visitTimingDataRange(
                        fromSequence,
                        limit,
                        visitor));
    }

    public static <R> TimingNodeQuery<R> latestTimingData(
            int limit,
            LogBookVisitor<R> visitor) {
        requireVisitor(visitor);
        return new TimingNodeQuery<>(
                "latestTimingData",
                true,
                logic -> logic.visitLatestTimingData(limit, visitor));
    }

    private static void requireVisitor(LogBookVisitor<?> visitor) {
        if (visitor == null) {
            throw new IllegalArgumentException("visitor must not be null");
        }
    }
}
