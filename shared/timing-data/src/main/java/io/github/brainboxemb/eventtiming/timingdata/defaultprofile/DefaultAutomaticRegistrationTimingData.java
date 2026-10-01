package io.github.brainboxemb.eventtiming.timingdata.defaultprofile;

import io.github.brainboxemb.eventtiming.timingdata.AutomaticRegistrationTimingData;
import io.github.brainboxemb.eventtiming.timingdata.RegistrationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataContext;
import io.github.brainboxemb.eventtiming.timingdata.TimingTimestamp;

/** Simple immutable default-profile automatic registration value. */
final class DefaultAutomaticRegistrationTimingData
        implements AutomaticRegistrationTimingData {

    private final TimingDataContext context;
    private final RegistrationId registrationId;

    DefaultAutomaticRegistrationTimingData(
            TimingDataContext context,
            RegistrationId registrationId) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (registrationId == null) {
            throw new IllegalArgumentException("registrationId must not be null");
        }
        this.context = context;
        this.registrationId = registrationId;
    }

    @Override
    public String timingNodeId() {
        return context.timingNodeId();
    }

    @Override
    public long sequenceNumber() {
        return context.sequenceNumber();
    }

    @Override
    public int locationId() {
        return context.locationId();
    }

    @Override
    public TimingTimestamp effectiveTime() {
        return context.effectiveTime();
    }

    @Override
    public TimingTimestamp recordedAt() {
        return context.recordedAt();
    }

    @Override
    public RegistrationId registrationId() {
        return registrationId;
    }
}
