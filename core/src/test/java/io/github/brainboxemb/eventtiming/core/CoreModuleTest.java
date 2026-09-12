package io.github.brainboxemb.eventtiming.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CoreModuleTest {
    @Test
    public void composesDomain() {
        assertEquals("domain -> core", CoreModule.composition());
    }
}
