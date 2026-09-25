package io.github.brainboxemb.eventtiming.presentation.console;

import io.github.brainboxemb.eventtiming.application.ApplicationStatus;
import io.github.brainboxemb.eventtiming.application.CommandHandler;
import io.github.brainboxemb.eventtiming.domain.timing.TimingNodeId;
import io.github.brainboxemb.eventtiming.infra.BuildIdentity;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocalConsoleTest {
    @Test
    public void helpVersionStatusAndQuitUseSharedApplicationBoundary() {
        AtomicBoolean stopped = new AtomicBoolean(false);
        StringWriter output = new StringWriter();
        LocalConsole console = new LocalConsole(
                commandHandler(),
                () -> stopped.set(true),
                new StringReader("help\nversion\nstatus\nquit\n"),
                output);

        console.run();

        String text = output.toString();
        assertTrue(text.contains("help     Show available commands"));
        assertTrue(text.contains("version  Show application version"));
        assertTrue(text.contains("status   Show application status"));
        assertTrue(text.contains("quit     Stop the application"));
        assertTrue(text.contains("exit     Alias for quit"));
        assertTrue(text.contains("event-timing-app"));
        assertTrue(text.contains("Version      : test-version"));
        assertTrue(text.contains("Revision     : abc123def456"));
        assertTrue(text.contains("Source ref   : feature/test"));
        assertTrue(text.contains("Build origin : local"));
        assertTrue(text.contains("Source state : clean"));
        assertTrue(text.contains("State       : RUNNING"));
        assertTrue(text.contains("Timing node : timing-node-01"));
        assertTrue(stopped.get());
    }

    @Test
    public void exitAlsoStopsApplication() {
        AtomicBoolean stopped = new AtomicBoolean(false);
        LocalConsole console = new LocalConsole(
                commandHandler(),
                () -> stopped.set(true),
                new StringReader("exit\n"),
                new StringWriter());

        console.run();

        assertTrue(stopped.get());
    }

    @Test
    public void unknownCommandDoesNotStopApplication() {
        AtomicBoolean stopped = new AtomicBoolean(false);
        StringWriter output = new StringWriter();
        LocalConsole console = new LocalConsole(
                commandHandler(),
                () -> stopped.set(true),
                new StringReader("wat\n"),
                output);

        console.run();

        assertFalse(stopped.get());
        assertTrue(output.toString().contains("Unknown command: wat"));
    }

    private static CommandHandler commandHandler() {
        BuildIdentity identity = BuildIdentity.firstApiVersion(
                "event-timing-app",
                "test-version",
                "abc123def456",
                "feature/test",
                "local",
                false);
        return new CommandHandler(
                identity,
                () -> new ApplicationStatus("RUNNING", new TimingNodeId("timing-node-01")));
    }
}
