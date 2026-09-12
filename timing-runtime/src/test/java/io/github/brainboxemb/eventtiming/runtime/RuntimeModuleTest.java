package io.github.brainboxemb.eventtiming.runtime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RuntimeModuleTest {
    @Test
    public void composesOnCore() {
        assertEquals("timing-api -> timing-core -> timing-runtime", RuntimeModule.composition());
    }
}
