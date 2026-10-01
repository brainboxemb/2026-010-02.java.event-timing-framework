package io.github.brainboxemb.eventtiming.timingpoint.domain.timing;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LocationIdTest {
    @Test
    public void keepsPositiveConfiguredValue() {
        LocationId id = new LocationId(24);

        assertEquals(24, id.value());
        assertEquals(new LocationId(24), id);
        assertEquals("24", id.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsZero() {
        new LocationId(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeValue() {
        new LocationId(-1);
    }
}
