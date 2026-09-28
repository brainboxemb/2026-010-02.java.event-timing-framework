package io.github.brainboxemb.eventtiming.presentation.interfaces.console;

import io.github.brainboxemb.eventtiming.application.CommandHandler;
import io.github.brainboxemb.eventtiming.presentation.common.terminal.TerminalSession;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/** Local text console for development and service use. */
public final class LocalConsole implements Runnable {
    private static final String READY_MESSAGE = "Local console ready. Type 'help' for commands.";

    private final TerminalSession session;
    private final Reader input;
    private final Writer output;

    public LocalConsole(
            CommandHandler commandHandler,
            Runnable shutdown,
            Reader input,
            Writer output) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        if (output == null) {
            throw new IllegalArgumentException("output must not be null");
        }
        this.session = new TerminalSession(commandHandler, shutdown);
        this.input = input;
        this.output = output;
    }

    @Override
    public void run() {
        try {
            session.run(input, output, READY_MESSAGE);
        } catch (IOException ex) {
            throw new IllegalStateException("Local console input failed", ex);
        }
    }
}
