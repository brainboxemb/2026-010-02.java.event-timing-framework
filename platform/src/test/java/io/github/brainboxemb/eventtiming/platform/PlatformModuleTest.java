package io.github.brainboxemb.eventtiming.platform;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PlatformModuleTest {
    @Test
    public void exposesModuleName() {
        assertEquals("platform", PlatformModule.name());
    }
}
