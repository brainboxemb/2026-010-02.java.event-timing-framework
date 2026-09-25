package io.github.brainboxemb.eventtiming.presentation.websocket;

import io.github.brainboxemb.eventtiming.application.CommandHandler;
import io.github.brainboxemb.eventtiming.presentation.control.ApplicationControlJson;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.extensions.IExtension;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** WebSocket adapter for IF-03 status snapshots and change events. */
public final class WebSocketStatusServer implements AutoCloseable {
    public static final String EVENTS_PATH = "/api/v1/events";

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketStatusServer.class);
    private static final int MAX_INBOUND_FRAME_BYTES = 64 * 1024;
    private static final long START_TIMEOUT_MILLIS = 3000L;

    private final String bindAddress;
    private final int port;
    private final CommandHandler commandHandler;
    private final Clock clock;

    private Server server;

    public WebSocketStatusServer(
            String bindAddress,
            int port,
            CommandHandler commandHandler) {
        this(bindAddress, port, commandHandler, Clock.systemUTC());
    }

    WebSocketStatusServer(
            String bindAddress,
            int port,
            CommandHandler commandHandler,
            Clock clock) {
        if (bindAddress == null || bindAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("bindAddress must not be blank");
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        if (commandHandler == null) {
            throw new IllegalArgumentException("commandHandler must not be null");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.bindAddress = bindAddress.trim();
        this.port = port;
        this.commandHandler = commandHandler;
        this.clock = clock;
    }

    public synchronized void start() throws IOException {
        if (server != null) {
            throw new IllegalStateException("WebSocket status server is already started");
        }

        InetSocketAddress address =
                new InetSocketAddress(InetAddress.getByName(bindAddress), port);
        Server candidate = new Server(address);
        candidate.start();

        try {
            if (!candidate.awaitStarted(START_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                stopCandidate(candidate);
                throw new IOException("Timed out starting WebSocket status server");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            stopCandidate(candidate);
            throw new IOException("Interrupted while starting WebSocket status server", ex);
        }

        Exception failure = candidate.startFailure();
        if (failure != null) {
            stopCandidate(candidate);
            throw new IOException("Unable to start WebSocket status server", failure);
        }

        server = candidate;
        LOG.info("WebSocket status listening on {}:{}{}", bindAddress, candidate.getPort(), EVENTS_PATH);
    }

    public synchronized int boundPort() {
        if (server == null) {
            throw new IllegalStateException("WebSocket status server is not started");
        }
        return server.getPort();
    }

    /** Broadcasts a complete current status after a real authoritative status change. */
    public void publishStatusChanged() {
        Server current;
        synchronized (this) {
            current = server;
        }
        if (current == null) {
            throw new IllegalStateException("WebSocket status server is not started");
        }
        current.broadcast(eventJson("STATUS_CHANGED"));
    }

    private String eventJson(String eventType) {
        return ApplicationControlJson.statusEvent(
                eventType,
                clock.instant(),
                commandHandler.version(),
                commandHandler.status());
    }

    @Override
    public synchronized void close() {
        Server current = server;
        server = null;
        if (current == null) {
            return;
        }
        try {
            current.stop(1000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static void stopCandidate(Server candidate) {
        try {
            candidate.stop(1000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private final class Server extends WebSocketServer {
        private final CountDownLatch started = new CountDownLatch(1);
        private final AtomicReference<Exception> startFailure = new AtomicReference<>();

        private Server(InetSocketAddress address) {
            super(
                    address,
                    1,
                    Collections.<Draft>singletonList(
                            new Draft_6455(
                                    Collections.<IExtension>emptyList(),
                                    MAX_INBOUND_FRAME_BYTES)));
        }

        private boolean awaitStarted(long timeout, TimeUnit unit) throws InterruptedException {
            return started.await(timeout, unit);
        }

        private Exception startFailure() {
            return startFailure.get();
        }

        @Override
        public void onOpen(WebSocket connection, ClientHandshake handshake) {
            if (!EVENTS_PATH.equals(handshake.getResourceDescriptor())) {
                connection.close(
                        CloseFrame.POLICY_VALIDATION,
                        "Unknown IF-03 WebSocket resource");
                return;
            }
            try {
                connection.send(eventJson("STATUS_SNAPSHOT"));
            } catch (RuntimeException ex) {
                LOG.warn("Unable to create initial IF-03 status snapshot", ex);
                connection.close(
                        CloseFrame.UNEXPECTED_CONDITION,
                        "Unable to create status snapshot");
            }
        }

        @Override
        public void onClose(WebSocket connection, int code, String reason, boolean remote) {
            LOG.debug(
                    "IF-03 WebSocket client closed code={} remote={} reason={}",
                    code,
                    remote,
                    reason);
        }

        @Override
        public void onMessage(WebSocket connection, String message) {
            rejectClientMessage(connection);
        }

        @Override
        public void onMessage(WebSocket connection, ByteBuffer message) {
            rejectClientMessage(connection);
        }

        private void rejectClientMessage(WebSocket connection) {
            connection.close(
                    CloseFrame.POLICY_VALIDATION,
                    "IF-03 event stream is server-to-client only");
        }

        @Override
        public void onError(WebSocket connection, Exception ex) {
            if (started.getCount() > 0) {
                startFailure.compareAndSet(null, ex);
                started.countDown();
            }
            if (connection == null) {
                LOG.warn("IF-03 WebSocket server failed", ex);
            } else {
                LOG.debug("IF-03 WebSocket connection failed", ex);
            }
        }

        @Override
        public void onStart() {
            started.countDown();
        }
    }
}
