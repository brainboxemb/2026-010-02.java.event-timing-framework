package io.github.brainboxemb.eventtiming.timingdata;

/**
 * Type-safe semantic contract for one automatic/tag registration.
 *
 * <p>The effective time is the accepted observed time by definition, so no
 * origin or observed-time flag is required on this interface.</p>
 */
public interface AutomaticRegistrationTimingData extends TimingData {

    RegistrationId registrationId();
}
