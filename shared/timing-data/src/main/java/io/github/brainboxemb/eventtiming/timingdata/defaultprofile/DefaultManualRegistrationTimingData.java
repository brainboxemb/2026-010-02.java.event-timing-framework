package io.github.brainboxemb.eventtiming.timingdata.defaultprofile;

import io.github.brainboxemb.eventtiming.timingdata.ManualRegistrationTimeSource;
import io.github.brainboxemb.eventtiming.timingdata.ManualRegistrationTimingData;
import io.github.brainboxemb.eventtiming.timingdata.RegistrationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataContext;
import io.github.brainboxemb.eventtiming.timingdata.TimingTimestamp;

/** Simple immutable default-profile manual registration value. */
final class DefaultManualRegistrationTimingData implements ManualRegistrationTimingData {

    private final TimingDataContext context;
    private final RegistrationId registrationId;
    private final ManualRegistrationTimeSource timeSource;

    DefaultManualRegistrationTimingData(
            TimingDataContext context,
            RegistrationId registrationId,
            ManualRegistrationTimeSource timeSource) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (registrationId == null) {
            throw new IllegalArgumentException("registrationId must not be null");
        }
        if (timeSource == null) {
            throw new IllegalArgumentException("timeSource must not be null");
        }
        this.context = context;
        this.registrationId = registrationId;
        this.timeSource = timeSource;
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

    @Override
    public ManualRegistrationTimeSource timeSource() {
        return timeSource;
    }
}
