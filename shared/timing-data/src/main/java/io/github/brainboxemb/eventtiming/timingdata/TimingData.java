package io.github.brainboxemb.eventtiming.timingdata;

/**
 * Common immutable TimingData contract shared by all concrete profiles.
 *
 * <p>The interface exposes only values that SI-01 and other consumers need
 * independently of a concrete TimingData implementation.</p>
 */
public interface TimingData {

    String timingNodeId();

    long sequenceNumber();

    int locationId();

    TimingTimestamp effectiveTime();

    TimingTimestamp recordedAt();
}
