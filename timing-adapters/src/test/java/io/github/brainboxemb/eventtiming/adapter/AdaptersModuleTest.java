package io.github.brainboxemb.eventtiming.adapter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AdaptersModuleTest {
    @Test
    public void dependsOnApi() {
        assertEquals("timing-api -> timing-adapters", AdaptersModule.composition());
    }
}
