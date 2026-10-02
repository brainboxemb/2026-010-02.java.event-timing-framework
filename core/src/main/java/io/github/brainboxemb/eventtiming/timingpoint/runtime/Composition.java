package io.github.brainboxemb.eventtiming.timingpoint.runtime;

import io.github.brainboxemb.eventtiming.timingdata.TimingTimestamp;
import io.github.brainboxemb.eventtiming.timingdata.defaultprofile.DefaultTimingDataCodec;
import io.github.brainboxemb.eventtiming.timingdata.defaultprofile.DefaultTimingDataFactory;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNode;
import io.github.brainboxemb.eventtiming.timingpoint.infra.BuildIdentity;
import io.github.brainboxemb.eventtiming.timingpoint.io.storage.FileTimingDataStore;
import io.github.brainboxemb.eventtiming.timingpoint.presentation.interfaces.api.HttpEndpoint;
import io.github.brainboxemb.eventtiming.timingpoint.presentation.interfaces.api.WebSocketEndpoint;
import io.github.brainboxemb.eventtiming.timingpoint.presentation.interfaces.console.LocalConsole;
import io.github.brainboxemb.eventtiming.timingpoint.presentation.interfaces.shell.RemoteShellServer;
import io.github.brainboxemb.eventtiming.timingpoint.runtime.config.Api;
import io.github.brainboxemb.eventtiming.timingpoint.runtime.config.Config;
import io.github.brainboxemb.eventtiming.timingpoint.runtime.config.Presentation;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.time.Instant;

/**
 * Owns the concrete SI-01 runtime composition.
 *
 * <p>I/O, Platform and infrastructure types keep their own responsibilities.
 * This class only decides which current implementations form the running
 * application.</p>
 */
public final class Composition {
    private Composition() {
    }

    public static void run(BuildIdentity buildIdentity, Config config) throws IOException {
        Application application = create(buildIdentity, config);

        Runtime runtime = Runtime.getRuntime();
        Thread shutdownHook = new Thread(application::close, "event-timing-shutdown");
        runtime.addShutdownHook(shutdownHook);

        HttpEndpoint http = null;
        WebSocketEndpoint webSocket = null;
        RemoteShellServer remoteShell = null;
        try {
            application.start();
            http = startHttp(config, application);
            webSocket = startWebSocket(config, application);
            remoteShell = startRemoteShell(config, application);
            startLocalConsole(application);
            try {
                application.awaitStopped();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        } finally {
            if (webSocket != null) {
                webSocket.close();
            }
            if (http != null) {
                http.close();
            }
            if (remoteShell != null) {
                remoteShell.close();
            }
            application.close();
            removeShutdownHook(runtime, shutdownHook);
        }

        System.out.println(application.smokeOutput());
    }

    static Application create(BuildIdentity buildIdentity, Config config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        if (config.timingDataPath() == null) {
            throw new IllegalArgumentException(
                    "TimingData storage path must be configured before composition");
        }

        DefaultTimingDataFactory timingDataFactory = new DefaultTimingDataFactory();
        FileTimingDataStore timingDataStore = new FileTimingDataStore(
                config.timingDataPath(),
                config.timingNodeId(),
                new DefaultTimingDataCodec());

        TimingNode timingNode = new TimingNode(
                config.timingNodeId(),
                timingDataStore,
                timingDataFactory,
                () -> new TimingTimestamp(Instant.now()));

        return new Application(buildIdentity, timingNode);
    }

    private static HttpEndpoint startHttp(Config config, Application application)
            throws IOException {
        Api api = config.presentation().api();
        Api.Http endpoint = api == null ? null : api.http();
        if (endpoint == null) {
            return null;
        }

        HttpEndpoint server = new HttpEndpoint(
                endpoint.bindAddress(),
                endpoint.port(),
                application.commandHandler());
        server.start();
        return server;
    }

    private static WebSocketEndpoint startWebSocket(Config config, Application application)
            throws IOException {
        Api api = config.presentation().api();
        Api.WebSocket endpoint = api == null ? null : api.webSocket();
        if (endpoint == null) {
            return null;
        }

        WebSocketEndpoint server = new WebSocketEndpoint(
                endpoint.bindAddress(),
                endpoint.port(),
                application.commandHandler());
        server.start();
        return server;
    }

    private static RemoteShellServer startRemoteShell(Config config, Application application)
            throws IOException {
        Presentation.RemoteShell endpoint = config.presentation().remoteShell();
        if (endpoint == null) {
            return null;
        }

        RemoteShellServer server = new RemoteShellServer(
                endpoint.bindAddress(),
                endpoint.port(),
                application.commandHandler(),
                application::close);
        server.start();
        return server;
    }

    private static void startLocalConsole(Application application) {
        LocalConsole console = new LocalConsole(
                application.commandHandler(),
                application::close,
                new InputStreamReader(System.in),
                new OutputStreamWriter(System.out));
        Thread consoleThread = new Thread(console, "event-timing-console");
        consoleThread.setDaemon(true);
        consoleThread.start();
    }

    private static void removeShutdownHook(Runtime runtime, Thread shutdownHook) {
        try {
            runtime.removeShutdownHook(shutdownHook);
        } catch (IllegalStateException ignored) {
            // JVM shutdown is already in progress.
        }
    }
}
