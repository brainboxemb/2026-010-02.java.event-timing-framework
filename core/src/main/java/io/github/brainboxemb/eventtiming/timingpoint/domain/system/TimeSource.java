package io.github.brainboxemb.eventtiming.timingpoint.domain.system;

import io.github.brainboxemb.eventtiming.timingdata.TimingTimestamp;

/**
 * Project-owned absolute time source shared by one TimingSystem composition.
 */
@FunctionalInterface
public interface TimeSource {

    TimingTimestamp now();
}
