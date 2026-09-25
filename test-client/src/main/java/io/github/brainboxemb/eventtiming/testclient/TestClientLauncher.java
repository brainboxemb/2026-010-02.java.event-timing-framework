package io.github.brainboxemb.eventtiming.testclient;

import javafx.application.Application;

/**
 * Plain Java entry point for IDEs and classpath-based launchers.
 *
 * <p>Launching a class that directly extends {@link Application} can make the JVM
 * expect JavaFX runtime modules on its launcher module path. Keeping the process
 * entry point separate lets Maven and IDEs provide JavaFX through the normal
 * project dependencies.</p>
 */
public final class TestClientLauncher {
    private TestClientLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(TestClientApplication.class, args);
    }
}
