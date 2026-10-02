package io.github.brainboxemb.eventtiming.timingpoint.application;

import io.github.brainboxemb.eventtiming.timingdata.LocationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingNodeId;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeTypes.Lifecycle;

/** Transport-independent current TimingNode status used by presentation adapters. */
public final class ApplicationStatus {
    private final TimingNodeId timingNodeId;
    private final Lifecycle timingNodeLifecycle;
    private final LocationId locationId;

    public ApplicationStatus(
            TimingNodeId timingNodeId,
            Lifecycle timingNodeLifecycle) {
        this(timingNodeId, timingNodeLifecycle, null);
    }

    public ApplicationStatus(
            TimingNodeId timingNodeId,
            Lifecycle timingNodeLifecycle,
            LocationId locationId) {
        if (timingNodeId == null) {
            throw new IllegalArgumentException("timingNodeId must not be null");
        }
        if (timingNodeLifecycle == null) {
            throw new IllegalArgumentException("timingNodeLifecycle must not be null");
        }
        this.timingNodeId = timingNodeId;
        this.timingNodeLifecycle = timingNodeLifecycle;
        this.locationId = locationId;
    }

    public TimingNodeId timingNodeId() {
        return timingNodeId;
    }

    public Lifecycle timingNodeLifecycle() {
        return timingNodeLifecycle;
    }

    public boolean hasLocation() {
        return locationId != null;
    }

    public LocationId locationId() {
        return locationId;
    }
}
