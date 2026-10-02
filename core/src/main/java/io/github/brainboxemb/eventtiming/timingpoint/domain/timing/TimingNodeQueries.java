package io.github.brainboxemb.eventtiming.timingpoint.domain.timing;

import io.github.brainboxemb.eventtiming.timingdata.TimingData;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeTypes.Status;

import java.util.List;

/**
 * Standard read operations supported by TimingNode.
 *
 * <p>Queries are represented as typed values so a new read does not require
 * another forwarding method on the TimingNode component boundary. Commands use
 * the matching TimingNodeCommand/TimingNodeCommands path.</p>
 */
public final class TimingNodeQueries {
    private static final TimingNodeQuery<Status> STATUS =
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

    public static TimingNodeQuery<Status> status() {
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
