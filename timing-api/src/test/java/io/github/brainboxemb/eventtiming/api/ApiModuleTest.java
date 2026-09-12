package io.github.brainboxemb.eventtiming.api;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ApiModuleTest {
    @Test
    public void exposesModuleName() {
        assertEquals("timing-api", ApiModule.moduleName());
    }
}
