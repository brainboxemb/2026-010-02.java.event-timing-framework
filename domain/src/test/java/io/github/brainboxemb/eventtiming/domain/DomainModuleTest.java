package io.github.brainboxemb.eventtiming.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DomainModuleTest {
    @Test
    public void exposesModuleName() {
        assertEquals("domain", DomainModule.name());
    }
}
