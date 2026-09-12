package io.github.brainboxemb.eventtiming.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimingApplicationTest {
    @Test
    public void consumesFrameworkLibrary() {
        assertEquals("core", TimingApplication.frameworkComponent());
    }
}
