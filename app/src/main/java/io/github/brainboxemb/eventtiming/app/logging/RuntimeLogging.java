package io.github.brainboxemb.eventtiming.app.logging;

import io.github.brainboxemb.eventtiming.infra.bootstrap.config.LoggingConfig;
import io.github.brainboxemb.eventtiming.infra.bootstrap.config.LoggingFileConfig;
import io.github.brainboxemb.eventtiming.infra.bootstrap.config.LoggingLiveConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/** Concrete JUL logging composition selected by the default executable application. */
public final class RuntimeLogging implements AutoCloseable {
    private final Logger rootLogger;
    private final java.util.logging.Level previousRootLevel;
    private final Map<Handler, java.util.logging.Level> previousHandlerLevels;
    private final FileHandler fileHandler;
    private final DiagnosticLogServer liveServer;
    private final LiveLogHandler liveHandler;
    private final LoggingControl control;

    private RuntimeLogging(
            Logger rootLogger,
            java.util.logging.Level previousRootLevel,
            Map<Handler, java.util.logging.Level> previousHandlerLevels,
            FileHandler fileHandler,
            DiagnosticLogServer liveServer,
            LiveLogHandler liveHandler,
            LoggingControl control) {
        this.rootLogger = rootLogger;
        this.previousRootLevel = previousRootLevel;
        this.previousHandlerLevels = previousHandlerLevels;
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
        for (Handler handler : root.getHandlers()) {
            previousHandlers.put(handler, handler.getLevel());
            handler.setLevel(java.util.logging.Level.ALL);
        }

        FileHandler fileHandler = null;
        DiagnosticLogServer liveServer = null;
        LiveLogHandler liveHandler = null;
        try {
            LoggingControl control = new LoggingControl(root, config.level());

            LoggingFileConfig file = config.file();
            Path path = Paths.get(file.path()).toAbsolutePath().normalize();
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            fileHandler = new FileHandler(
                    path.toString(),
                    file.rotateBytes(),
                    file.retainedFiles(),
                    true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(java.util.logging.Level.ALL);
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
            restore(root, previousRoot, previousHandlers);
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
        restore(rootLogger, previousRootLevel, previousHandlerLevels);
    }

    private static void restore(
            Logger root,
            java.util.logging.Level rootLevel,
            Map<Handler, java.util.logging.Level> handlerLevels) {
        root.setLevel(rootLevel);
        for (Map.Entry<Handler, java.util.logging.Level> entry : handlerLevels.entrySet()) {
            entry.getKey().setLevel(entry.getValue());
        }
    }
}
