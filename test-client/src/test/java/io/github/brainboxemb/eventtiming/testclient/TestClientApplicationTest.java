package io.github.brainboxemb.eventtiming.testclient;

import javafx.application.Application;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class TestClientApplicationTest {
    @Test
    void publicEntryPointIsNotAJavaFxApplicationSubclass() {
        assertFalse(Application.class.isAssignableFrom(TestClientApplication.class));
    }
}
