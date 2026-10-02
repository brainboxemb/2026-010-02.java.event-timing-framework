package io.github.brainboxemb.eventtiming.timingdata;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TimingDataTypes.RegistrationIdTest {

    @Test
    public void preservesCanonicalValueWithoutNormalizingIt() {
        TimingDataTypes.RegistrationId registrationId = new TimingDataTypes.RegistrationId(" registration-0042 ");

        assertEquals(" registration-0042 ", registrationId.value());
        assertEquals(registrationId, new TimingDataTypes.RegistrationId(" registration-0042 "));
        assertNotEquals(registrationId, new TimingDataTypes.RegistrationId("registration-0042"));
        assertEquals(" registration-0042 ", registrationId.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankValue() {
        new TimingDataTypes.RegistrationId("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingValue() {
        new TimingDataTypes.RegistrationId(null);
    }
}
