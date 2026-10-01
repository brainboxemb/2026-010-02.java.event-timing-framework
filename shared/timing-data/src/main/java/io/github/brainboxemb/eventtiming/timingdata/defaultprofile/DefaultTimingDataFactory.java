package io.github.brainboxemb.eventtiming.timingdata.defaultprofile;

import io.github.brainboxemb.eventtiming.timingdata.LocationId;
import io.github.brainboxemb.eventtiming.timingdata.RegistrationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingData;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataFactory;
import io.github.brainboxemb.eventtiming.timingdata.TimingTimestamp;

/**
 * Default/reference profile TimingData factory.
 *
 * <p>The concrete default values are private implementation details. Consumers
 * depend on the grouped public semantic contracts under {@link TimingData}.</p>
 */
public final class DefaultTimingDataFactory implements TimingDataFactory {

    @Override
    public TimingData.AutomaticRegistration createAutomaticRegistration(
            Context context,
            RegistrationId registrationId) {
        return new AutomaticRegistration(context, registrationId);
    }

    @Override
    public TimingData.ManualRegistration createManualRegistration(
            Context context,
            RegistrationId registrationId,
            TimingData.ManualTimeSource timeSource) {
        return new ManualRegistration(context, registrationId, timeSource);
    }

    private static final class AutomaticRegistration
            implements TimingData.AutomaticRegistration {

        private final Context context;
        private final RegistrationId registrationId;

        private AutomaticRegistration(
                Context context,
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
        public LocationId locationId() {
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
            implements TimingData.ManualRegistration {

        private final Context context;
        private final RegistrationId registrationId;
        private final TimingData.ManualTimeSource timeSource;

        private ManualRegistration(
                Context context,
                RegistrationId registrationId,
                TimingData.ManualTimeSource timeSource) {
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
        public LocationId locationId() {
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
        public TimingData.ManualTimeSource timeSource() {
            return timeSource;
        }
    }

    private static Context requireContext(Context context) {
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
