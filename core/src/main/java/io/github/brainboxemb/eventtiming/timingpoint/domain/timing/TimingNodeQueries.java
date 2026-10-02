package io.github.brainboxemb.eventtiming.timingpoint.domain.timing;

import io.github.brainboxemb.eventtiming.timingdata.TimingData;

import java.util.List;

/**
 * Standard read operations supported by TimingNode.
 *
 * <p>Commands remain explicit methods on TimingNode because they express
 * state-changing domain intent. Queries are represented as typed values so a
 * new read does not require another forwarding method on the TimingNode
 * component boundary.</p>
 */
public final class TimingNodeQueries {
    private static final TimingNodeQuery<TimingNode.Status> STATUS =
            new TimingNodeQuery<>("status", false, TimingNodeLogic::status);
    private static final TimingNodeQuery<List<TimingData>> TIMING_DATA_SNAPSHOT =
            new TimingNodeQuery<>(
                    "timingDataSnapshot",
                    true,
                    TimingNodeLogic::timingDataSnapshot);
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

    public static TimingNodeQuery<List<TimingData>> timingDataSnapshot() {
        return TIMING_DATA_SNAPSHOT;
    }

    public static TimingNodeQuery<Integer> timingDataCount() {
        return TIMING_DATA_COUNT;
    }

    public static TimingNodeQuery<List<TimingData>> timingDataRange(
            long fromSequence,
            int limit) {
        return new TimingNodeQuery<>(
                "timingDataRange",
                true,
                logic -> logic.timingDataRange(fromSequence, limit));
    }

    public static TimingNodeQuery<List<TimingData>> latestTimingData(int limit) {
        return new TimingNodeQuery<>(
                "latestTimingData",
                true,
                logic -> logic.latestTimingData(limit));
    }
}
