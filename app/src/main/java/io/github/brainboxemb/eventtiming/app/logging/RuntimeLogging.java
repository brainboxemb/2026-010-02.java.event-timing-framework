package io.github.brainboxemb.eventtiming.app.logging;

import io.github.brainboxemb.eventtiming.infra.bootstrap.config.LoggingConfig;
import io.github.brainboxemb.eventtiming.infra.bootstrap.config.LoggingFileConfig;
import io.github.brainboxemb.eventtiming.infra.bootstrap.config.LoggingLiveConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Logger;

/** Concrete JUL logging composition selected by the default executable application. */
public final class RuntimeLogging implements AutoCloseable {
    private final Logger rootLogger;
    private final java.util.logging.Level previousRootLevel;
    private final Map<Handler, java.util.logging.Level> previousHandlerLevels;
    private final Map<Handler, Formatter> previousHandlerFormatters;
    private final TimestampedFileLogHandler fileHandler;
    private final DiagnosticLogServer liveServer;
    private final LiveLogHandler liveHandler;
    private final LoggingControl control;

    private RuntimeLogging(
            Logger rootLogger,
            java.util.logging.Level previousRootLevel,
            Map<Handler, java.util.logging.Level> previousHandlerLevels,
            Map<Handler, Formatter> previousHandlerFormatters,
            TimestampedFileLogHandler fileHandler,
            DiagnosticLogServer liveServer,
            LiveLogHandler liveHandler,
            LoggingControl control) {
        this.rootLogger = rootLogger;
        this.previousRootLevel = previousRootLevel;
        this.previousHandlerLevels = previousHandlerLevels;
        this.previousHandlerFormatters = previousHandlerFormatters;
        this.fileHandler = fileHandler;
        this.liveServer = liveServer;
        this.liveHandler = liveHandler;
        this.control = control;
    }

    public static RuntimeLogging start(LoggingConfig config) throws IOException {
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
        DiagnosticLogServer liveServer = null;
        LiveLogHandler liveHandler = null;
        try {
            LoggingControl control = new LoggingControl(root, config.level());

            LoggingFileConfig file = config.file();
            Path directory = Paths.get(file.path()).toAbsolutePath().normalize();
            fileHandler = new TimestampedFileLogHandler(
                    directory,
                    file.rotateBytes(),
                    file.retainedFiles());
            root.addHandler(fileHandler);

            LoggingLiveConfig live = config.live();
            if (live != null) {
                liveServer = new DiagnosticLogServer(
                        live.bindAddress(),
                        live.port(),
                        control);
                liveServer.start();
                liveHandler = new LiveLogHandler(liveServer);
                root.addHandler(liveHandler);
            }

            return new RuntimeLogging(
                    root,
                    previousRoot,
                    previousHandlers,
                    previousFormatters,
                    fileHandler,
                    liveServer,
                    liveHandler,
                    control);
        } catch (IOException | RuntimeException ex) {
            if (liveHandler != null) {
                root.removeHandler(liveHandler);
            }
            if (liveServer != null) {
                liveServer.close();
            }
            if (fileHandler != null) {
                root.removeHandler(fileHandler);
                fileHandler.close();
            }
            restore(root, previousRoot, previousHandlers, previousFormatters);
            throw ex;
        }
    }

    public int livePort() {
        if (liveServer == null) {
            throw new IllegalStateException("live logging is not configured");
        }
        return liveServer.boundPort();
    }

    LoggingConfig.Level level() {
        return control.level();
    }

    @Override
    public void close() {
        if (liveHandler != null) {
            rootLogger.removeHandler(liveHandler);
        }
        if (liveServer != null) {
            liveServer.close();
        }
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
