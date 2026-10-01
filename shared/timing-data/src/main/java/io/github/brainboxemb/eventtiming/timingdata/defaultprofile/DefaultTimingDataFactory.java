package io.github.brainboxemb.eventtiming.timingdata.defaultprofile;

import io.github.brainboxemb.eventtiming.timingdata.AutomaticRegistrationTimingData;
import io.github.brainboxemb.eventtiming.timingdata.ManualRegistrationTimeSource;
import io.github.brainboxemb.eventtiming.timingdata.ManualRegistrationTimingData;
import io.github.brainboxemb.eventtiming.timingdata.RegistrationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataContext;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataFactory;

/** Default/reference profile TimingData factory. */
public final class DefaultTimingDataFactory implements TimingDataFactory {

    @Override
    public AutomaticRegistrationTimingData createAutomaticRegistration(
            TimingDataContext context,
            RegistrationId registrationId) {
        return new DefaultAutomaticRegistrationTimingData(context, registrationId);
    }

    @Override
    public ManualRegistrationTimingData createManualRegistration(
            TimingDataContext context,
            RegistrationId registrationId,
            ManualRegistrationTimeSource timeSource) {
        return new DefaultManualRegistrationTimingData(context, registrationId, timeSource);
    }
}
