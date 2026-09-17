package io.github.brainboxemb.eventtiming;

import io.github.brainboxemb.eventtiming.comm.CommLayer;
import io.github.brainboxemb.eventtiming.core.CoreLayer;
import io.github.brainboxemb.eventtiming.domain.DomainLayer;
import io.github.brainboxemb.eventtiming.platform.PlatformLayer;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class FrameworkStructureTest {
    // Migration-006 canary: Java-only changes should select Windows smoke, not full Maven verification.
    @Test
    public void keepsInitialResponsibilitiesInsideOneFrameworkArtifact() {
        assertArrayEquals(
                new String[] {"domain", "core", "platform", "comm"},
                new String[] {
                        DomainLayer.name(),
                        CoreLayer.name(),
                        PlatformLayer.name(),
                        CommLayer.name()
                });
    }
}
