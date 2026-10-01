package io.github.brainboxemb.eventtiming.timingdata.defaultprofile;

import io.github.brainboxemb.eventtiming.timingdata.AutomaticRegistrationTimingData;
import io.github.brainboxemb.eventtiming.timingdata.ManualRegistrationTimeSource;
import io.github.brainboxemb.eventtiming.timingdata.ManualRegistrationTimingData;
import io.github.brainboxemb.eventtiming.timingdata.RegistrationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataContext;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataFactory;
import io.github.brainboxemb.eventtiming.timingdata.TimingTimestamp;

/**
 * Default/reference profile TimingData factory.
 *
 * <p>The concrete default values are implementation details of this factory.
 * Consumers depend on the public semantic TimingData interfaces instead.</p>
 */
public final class DefaultTimingDataFactory implements TimingDataFactory {

    @Override
    public AutomaticRegistrationTimingData createAutomaticRegistration(
            TimingDataContext context,
            RegistrationId registrationId) {
        return new AutomaticRegistration(context, registrationId);
    }

    @Override
    public ManualRegistrationTimingData createManualRegistration(
            TimingDataContext context,
            RegistrationId registrationId,
            ManualRegistrationTimeSource timeSource) {
        return new ManualRegistration(context, registrationId, timeSource);
    }

    private static final class AutomaticRegistration
            implements AutomaticRegistrationTimingData {

        private final TimingDataContext context;
        private final RegistrationId registrationId;

        private AutomaticRegistration(
                TimingDataContext context,
                RegistrationId registrationId) {
            this.context = requireContext(context);
            this.registrationId = requireRegistrationId(registrationId);
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

    private static final class ManualRegistration
            implements ManualRegistrationTimingData {

        private final TimingDataContext context;
        private final RegistrationId registrationId;
        private final ManualRegistrationTimeSource timeSource;

        private ManualRegistration(
                TimingDataContext context,
                RegistrationId registrationId,
                ManualRegistrationTimeSource timeSource) {
            this.context = requireContext(context);
            this.registrationId = requireRegistrationId(registrationId);
            if (timeSource == null) {
                throw new IllegalArgumentException("timeSource must not be null");
            }
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

    private static TimingDataContext requireContext(TimingDataContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        return context;
    }

    private static RegistrationId requireRegistrationId(RegistrationId registrationId) {
        if (registrationId == null) {
            throw new IllegalArgumentException("registrationId must not be null");
        }
        return registrationId;
    }
}
