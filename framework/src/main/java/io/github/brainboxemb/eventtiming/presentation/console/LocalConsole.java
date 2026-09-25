package io.github.brainboxemb.eventtiming.presentation.console;

import io.github.brainboxemb.eventtiming.application.ApplicationStatus;
import io.github.brainboxemb.eventtiming.application.CommandHandler;
import io.github.brainboxemb.eventtiming.infra.BuildIdentity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Locale;

/** Local text console for development and service use. */
public final class LocalConsole implements Runnable {
    private static final String PROMPT = "event-timing> ";

    private final CommandHandler commandHandler;
    private final Runnable shutdown;
    private final BufferedReader input;
    private final PrintWriter output;

    public LocalConsole(
            CommandHandler commandHandler,
            Runnable shutdown,
            Reader input,
            Writer output) {
        if (commandHandler == null) {
            throw new IllegalArgumentException("commandHandler must not be null");
        }
        if (shutdown == null) {
            throw new IllegalArgumentException("shutdown must not be null");
        }
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        if (output == null) {
            throw new IllegalArgumentException("output must not be null");
        }
        this.commandHandler = commandHandler;
        this.shutdown = shutdown;
        this.input = new BufferedReader(input);
        this.output = new PrintWriter(output, true);
    }

    @Override
    public void run() {
        output.println("Local console ready. Type 'help' for commands.");

        try {
            String line;
            while (true) {
                output.print(PROMPT);
                output.flush();
                line = input.readLine();
                if (line == null) {
                    return;
                }
                if (execute(line)) {
                    return;
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Local console input failed", ex);
        }
    }

    private boolean execute(String line) {
        String command = line.trim().toLowerCase(Locale.ROOT);
        if (command.isEmpty()) {
            return false;
        }

        switch (command) {
            case "help":
                showHelp();
                return false;
            case "version":
                showVersion();
                return false;
            case "status":
                showStatus();
                return false;
            case "quit":
            case "exit":
                output.println("Stopping application.");
                shutdown.run();
                return true;
            default:
                output.println("Unknown command: " + line.trim() + ". Type 'help' for commands.");
                return false;
        }
    }

    private void showHelp() {
        output.println("Commands:");
        output.println("  help     Show available commands");
        output.println("  version  Show application version");
        output.println("  status   Show application status");
        output.println("  quit     Stop the application");
        output.println("  exit     Alias for quit");
    }

    private void showVersion() {
        BuildIdentity identity = commandHandler.version();
        output.println(
                "application=" + identity.application()
                        + " version=" + identity.version()
                        + " revision=" + identity.revision()
                        + " sourceRef=" + identity.sourceRef()
                        + " buildOrigin=" + identity.buildOrigin()
                        + " dirty=" + identity.dirty());
    }

    private void showStatus() {
        ApplicationStatus status = commandHandler.status();
        output.println(
                "applicationState=" + status.applicationState()
                        + " timingNodeId=" + status.timingNodeId().value());
    }
}
