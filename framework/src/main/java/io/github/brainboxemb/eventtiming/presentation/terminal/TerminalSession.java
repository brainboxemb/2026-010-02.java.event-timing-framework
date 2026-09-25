package io.github.brainboxemb.eventtiming.presentation.terminal;

import io.github.brainboxemb.eventtiming.application.ApplicationStatus;
import io.github.brainboxemb.eventtiming.application.CommandHandler;
import io.github.brainboxemb.eventtiming.infra.BuildIdentity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Locale;

/** Shared line-oriented command session used by local and remote terminal transports. */
public final class TerminalSession {
    private static final String PROMPT = "event-timing> ";

    private final CommandHandler commandHandler;
    private final Runnable shutdown;

    public TerminalSession(CommandHandler commandHandler, Runnable shutdown) {
        if (commandHandler == null) {
            throw new IllegalArgumentException("commandHandler must not be null");
        }
        if (shutdown == null) {
            throw new IllegalArgumentException("shutdown must not be null");
        }
        this.commandHandler = commandHandler;
        this.shutdown = shutdown;
    }

    public void run(Reader input, Writer output, String readyMessage) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        if (output == null) {
            throw new IllegalArgumentException("output must not be null");
        }
        if (readyMessage == null || readyMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("readyMessage must not be blank");
        }

        BufferedReader reader = new BufferedReader(input);
        PrintWriter writer = new PrintWriter(output, true);
        writer.println(readyMessage);

        String line;
        while (true) {
            writer.print(PROMPT);
            writer.flush();
            line = reader.readLine();
            if (line == null) {
                return;
            }
            if (execute(line, writer)) {
                return;
            }
        }
    }

    private boolean execute(String line, PrintWriter output) {
        String command = line.trim().toLowerCase(Locale.ROOT);
        if (command.isEmpty()) {
            return false;
        }

        switch (command) {
            case "help":
                showHelp(output);
                return false;
            case "version":
                showVersion(output);
                return false;
            case "status":
                showStatus(output);
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

    private void showHelp(PrintWriter output) {
        output.println("Commands:");
        output.println("  help     Show available commands");
        output.println("  version  Show application version");
        output.println("  status   Show application status");
        output.println("  quit     Stop the application");
        output.println("  exit     Alias for quit");
    }

    private void showVersion(PrintWriter output) {
        BuildIdentity identity = commandHandler.version();
        output.println(identity.application());
        output.println("  Version      : " + identity.version());
        output.println("  Revision     : " + identity.revision());
        output.println("  Source ref   : " + identity.sourceRef());
        output.println("  Build origin : " + identity.buildOrigin());
        output.println("  Source state : " + (identity.dirty() ? "modified" : "clean"));
    }

    private void showStatus(PrintWriter output) {
        ApplicationStatus status = commandHandler.status();
        output.println(commandHandler.version().application());
        output.println("  State       : " + status.applicationState());
        output.println("  Timing node : " + status.timingNodeId().value());
    }
}
