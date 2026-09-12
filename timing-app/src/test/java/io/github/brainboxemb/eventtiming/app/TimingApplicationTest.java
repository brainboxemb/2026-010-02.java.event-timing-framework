package io.github.brainboxemb.eventtiming.app;

import io.github.brainboxemb.eventtiming.testkit.ModuleAssertions;
import org.junit.Test;

public class TimingApplicationTest {
    @Test
    public void composesRuntimeAndAdaptersWithoutDomainBehaviour() {
        ModuleAssertions.requireEquals(
                "timing-api -> timing-core -> timing-runtime + timing-api -> timing-adapters",
                TimingApplication.composition());
    }
}
