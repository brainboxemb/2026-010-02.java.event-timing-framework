package io.github.brainboxemb.eventtiming.timingdata;

/** Type-safe semantic contract for one manual registration. */
public interface ManualRegistrationTimingData extends TimingData {

    RegistrationId registrationId();

    ManualRegistrationTimeSource timeSource();
}
