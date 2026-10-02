package io.github.brainboxemb.eventtiming.timingpoint.domain.logbook;

import io.github.brainboxemb.eventtiming.timingdata.TimingData;

/**
 * Visits one bounded LogBook selection without first copying it to a List.
 *
 * <p>The visitor runs synchronously on the owning TimingNode serial lane.
 * Implementations must therefore stay bounded and non-blocking. Building a
 * representation in memory is suitable; network or file I/O is not.</p>
 */
public interface LogBookVisitor<R> {
    /**
     * Called once before records are visited.
     *
     * @param totalCount complete committed LogBook size
     * @param nextSequence next sequence after this selection, or null when none
     */
    void begin(int totalCount, Long nextSequence);

    /** Called once for each selected committed record in source order. */
    void visit(TimingData data);

    /** Called once after the last record and returns the query result. */
    R finish();
}
