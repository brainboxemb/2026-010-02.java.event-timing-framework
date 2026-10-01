package io.github.brainboxemb.eventtiming.timingdata;

/**
 * Stateless construction boundary for the configured concrete TimingData profile.
 */
public interface TimingDataFactory {

    AutomaticRegistrationTimingData createAutomaticRegistration(
            TimingDataContext context,
            RegistrationId registrationId);

    ManualRegistrationTimingData createManualRegistration(
            TimingDataContext context,
            RegistrationId registrationId,
            ManualRegistrationTimeSource timeSource);
}
