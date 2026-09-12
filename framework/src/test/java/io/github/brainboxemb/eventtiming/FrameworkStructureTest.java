package io.github.brainboxemb.eventtiming;

import io.github.brainboxemb.eventtiming.comm.CommLayer;
import io.github.brainboxemb.eventtiming.core.CoreLayer;
import io.github.brainboxemb.eventtiming.domain.DomainLayer;
import io.github.brainboxemb.eventtiming.platform.PlatformLayer;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class FrameworkStructureTest {
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
