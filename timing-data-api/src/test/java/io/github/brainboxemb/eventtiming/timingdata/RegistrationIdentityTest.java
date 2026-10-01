package io.github.brainboxemb.eventtiming.timingdata;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RegistrationIdentityTest {

    @Test
    public void appliesLocationCompatibility() {
        RegistrationIdentity standard =
                new RegistrationIdentity(RegistrationIdentity.Type.STANDARD, 42);
        RegistrationIdentity women =
                new RegistrationIdentity(RegistrationIdentity.Type.WOMEN, 42);
        RegistrationIdentity men =
                new RegistrationIdentity(RegistrationIdentity.Type.MEN, 42);

        assertTrue(standard.supportsLocationId(1));
        assertTrue(standard.supportsLocationId(23));
        assertFalse(standard.supportsLocationId(24));
        assertTrue(women.supportsLocationId(24));
        assertFalse(women.supportsLocationId(25));
        assertTrue(men.supportsLocationId(25));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsZeroNumber() {
        new RegistrationIdentity(RegistrationIdentity.Type.STANDARD, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNumberAboveContractRange() {
        new RegistrationIdentity(
                RegistrationIdentity.Type.STANDARD,
                RegistrationIdentity.MAX_NUMBER + 1);
    }
}
