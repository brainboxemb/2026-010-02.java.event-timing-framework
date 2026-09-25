package io.github.brainboxemb.eventtiming.testclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TestClientBuildIdentityTest {
    @Test
    void loadsEmbeddedBuildIdentity() {
        TestClientBuildIdentity identity = TestClientBuildIdentity.embedded();

        assertFalse(identity.application().isBlank());
        assertFalse(identity.version().isBlank());
        assertFalse(identity.revision().isBlank());
        assertFalse(identity.sourceRef().isBlank());
        assertNotNull(identity.buildOrigin());
    }
}
