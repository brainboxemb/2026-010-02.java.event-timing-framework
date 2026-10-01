package io.github.brainboxemb.eventtiming.timingdata;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class RegistrationIdentityTest {

    @Test
    public void preservesProviderNeutralIdentityWithoutNormalizingIt() {
        RegistrationIdentity identity =
                new RegistrationIdentity(" participant-0042 ");

        assertEquals(" participant-0042 ", identity.value());
        assertEquals(identity, new RegistrationIdentity(" participant-0042 "));
        assertNotEquals(identity, new RegistrationIdentity("participant-0042"));
        assertEquals(" participant-0042 ", identity.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankIdentity() {
        new RegistrationIdentity("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingIdentity() {
        new RegistrationIdentity(null);
    }
}
