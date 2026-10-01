package io.github.brainboxemb.eventtiming.timingdata;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class RegistrationIdTest {

    @Test
    public void preservesCanonicalValueWithoutNormalizingIt() {
        RegistrationId registrationId = new RegistrationId(" registration-0042 ");

        assertEquals(" registration-0042 ", registrationId.value());
        assertEquals(registrationId, new RegistrationId(" registration-0042 "));
        assertNotEquals(registrationId, new RegistrationId("registration-0042"));
        assertEquals(" registration-0042 ", registrationId.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankValue() {
        new RegistrationId("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingValue() {
        new RegistrationId(null);
    }
}
