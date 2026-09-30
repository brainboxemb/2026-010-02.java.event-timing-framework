package io.github.brainboxemb.eventtiming.timingpoint.infra.logging;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Logger;

/** Reusable runtime logging infrastructure; the executable selects the SLF4J provider. */
public final class Logging implements AutoCloseable {
    private final Logger rootLogger;
    private final java.util.logging.Level previousRootLevel;
    private final Map<Handler, java.util.logging.Level> previousHandlerLevels;
    private final Map<Handler, Formatter> previousHandlerFormatters;
    private final TimestampedFileLogHandler fileHandler;
    private final LoggingControl control;

    private Logging(
            Logger rootLogger,
            java.util.logging.Level previousRootLevel,
            Map<Handler, java.util.logging.Level> previousHandlerLevels,
            Map<Handler, Formatter> previousHandlerFormatters,
            TimestampedFileLogHandler fileHandler,
            LoggingControl control) {
        this.rootLogger = rootLogger;
        this.previousRootLevel = previousRootLevel;
        this.previousHandlerLevels = previousHandlerLevels;
        this.previousHandlerFormatters = previousHandlerFormatters;
        this.fileHandler = fileHandler;
        this.control = control;
    }

    public static Logging start(LoggingConfig config) throws IOException {
        if (config == null) {
            throw new IllegalArgumentException("logging config must not be null");
        }

        Logger root = Logger.getLogger("");
        java.util.logging.Level previousRoot = root.getLevel();
        Map<Handler, java.util.logging.Level> previousHandlers =
                new IdentityHashMap<Handler, java.util.logging.Level>();
        Map<Handler, Formatter> previousFormatters =
                new IdentityHashMap<Handler, Formatter>();
        CompactLogFormatter consoleFormatter = new CompactLogFormatter();
        for (Handler handler : root.getHandlers()) {
            previousHandlers.put(handler, handler.getLevel());
            handler.setLevel(java.util.logging.Level.ALL);
            if (handler instanceof ConsoleHandler) {
                previousFormatters.put(handler, handler.getFormatter());
                handler.setFormatter(consoleFormatter);
            }
        }

        TimestampedFileLogHandler fileHandler = null;
        try {
            LoggingControl control = new LoggingControl(root, config.level());

            LoggingFileConfig file = config.file();
            Path directory = Paths.get(file.path()).toAbsolutePath().normalize();
            fileHandler = new TimestampedFileLogHandler(
                    directory,
                    file.rotateBytes(),
                    file.retainedFiles());
            root.addHandler(fileHandler);

            return new Logging(
                    root,
                    previousRoot,
                    previousHandlers,
                    previousFormatters,
                    fileHandler,
                    control);
        } catch (IOException | RuntimeException ex) {
            if (fileHandler != null) {
                root.removeHandler(fileHandler);
                fileHandler.close();
            }
            restore(root, previousRoot, previousHandlers, previousFormatters);
            throw ex;
        }
    }

    /** Runtime control shared with optional infrastructure components such as LoggingServer. */
    public LoggingControl control() {
        return control;
    }

    @Override
    public void close() {
        rootLogger.removeHandler(fileHandler);
        fileHandler.close();
        restore(
                rootLogger,
                previousRootLevel,
                previousHandlerLevels,
                previousHandlerFormatters);
    }

    private static void restore(
            Logger root,
            java.util.logging.Level rootLevel,
            Map<Handler, java.util.logging.Level> handlerLevels,
            Map<Handler, Formatter> handlerFormatters) {
        root.setLevel(rootLevel);
        for (Map.Entry<Handler, java.util.logging.Level> entry : handlerLevels.entrySet()) {
            entry.getKey().setLevel(entry.getValue());
        }
        for (Map.Entry<Handler, Formatter> entry : handlerFormatters.entrySet()) {
            entry.getKey().setFormatter(entry.getValue());
        }
    }
}
