package io.github.brainboxemb.eventtiming.comm;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CommModuleTest {
    @Test
    public void composesCoreAndPlatform() {
        assertEquals("domain -> core + platform -> comm", CommModule.composition());
    }
}
