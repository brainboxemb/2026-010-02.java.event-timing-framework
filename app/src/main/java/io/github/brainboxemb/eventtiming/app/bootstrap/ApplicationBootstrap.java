package io.github.brainboxemb.eventtiming.app.bootstrap;

import io.github.brainboxemb.eventtiming.app.TimingApplication;
import io.github.brainboxemb.eventtiming.domain.timing.TimingNode;
import io.github.brainboxemb.eventtiming.infra.BuildIdentity;
import io.github.brainboxemb.eventtiming.presentation.interfaces.console.LocalConsole;
import io.github.brainboxemb.eventtiming.presentation.interfaces.remoteapi.RemoteApiHttpServer;
import io.github.brainboxemb.eventtiming.presentation.interfaces.remoteapi.RemoteApiWebSocketServer;
import io.github.brainboxemb.eventtiming.presentation.interfaces.shell.RemoteShellServer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;

/**
 * Cross-cutting executable bootstrap: configuration, composition and startup wiring.
 *
 * <p>Bootstrap constructs the runtime and concrete presentation endpoints. It is not a normal
 * runtime layer; presentation/application/domain interactions do not route through it after
 * composition.</p>
 */
public final class ApplicationBootstrap {
    private ApplicationBootstrap() {
    }

    public static void run(BuildIdentity buildIdentity, Path configPath) throws IOException {
        ApplicationConfig config = ApplicationConfigLoader.load(configPath);
        TimingApplication application = compose(buildIdentity, config);

        Runtime runtime = Runtime.getRuntime();
        Thread shutdownHook = new Thread(application::close, "event-timing-shutdown");
        runtime.addShutdownHook(shutdownHook);

        RemoteApiHttpServer http = null;
        RemoteApiWebSocketServer webSocket = null;
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

    static TimingApplication compose(BuildIdentity buildIdentity, ApplicationConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        return TimingApplication.builder(buildIdentity)
                .timingNode(new TimingNode(config.timingNodeId()))
                .build();
    }

    private static RemoteApiHttpServer startHttp(
            ApplicationConfig config,
            TimingApplication application) throws IOException {
        RemoteApiConfig remoteApi = config.presentation().remoteApi();
        RemoteApiHttpConfig endpoint = remoteApi == null ? null : remoteApi.http();
        if (endpoint == null) {
            return null;
        }

        RemoteApiHttpServer server = new RemoteApiHttpServer(
                endpoint.bindAddress(),
                endpoint.port(),
                application.commandHandler());
        server.start();
        return server;
    }

    private static RemoteApiWebSocketServer startWebSocket(
            ApplicationConfig config,
            TimingApplication application) throws IOException {
        RemoteApiConfig remoteApi = config.presentation().remoteApi();
        RemoteApiWebSocketConfig endpoint =
                remoteApi == null ? null : remoteApi.webSocket();
        if (endpoint == null) {
            return null;
        }

        RemoteApiWebSocketServer server = new RemoteApiWebSocketServer(
                endpoint.bindAddress(),
                endpoint.port(),
                application.commandHandler());
        server.start();
        return server;
    }

    private static RemoteShellServer startRemoteShell(
            ApplicationConfig config,
            TimingApplication application) throws IOException {
        RemoteShellConfig endpoint = config.presentation().remoteShell();
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

    private static void startLocalConsole(TimingApplication application) {
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
