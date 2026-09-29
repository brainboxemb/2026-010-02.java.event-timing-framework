package io.github.brainboxemb.eventtiming.infra.logging;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogRecord;

/**
 * Small best-effort engineering log listener.
 *
 * <p>The client initiates the connection. Log publishers never write to the socket directly:
 * they only offer records to a bounded queue, so a slow/disconnected client cannot block normal
 * application execution.</p>
 */
final class LoggingServer implements AutoCloseable {
    private static final int QUEUE_CAPACITY = 512;

    private final String bindAddress;
    private final int port;
    private final LoggingControl control;
    private final CompactLogFormatter formatter = new CompactLogFormatter();
    private final BlockingQueue<String> outbound =
            new ArrayBlockingQueue<String>(QUEUE_CAPACITY);

    private volatile boolean closed;
    private volatile ServerSocket serverSocket;
    private volatile Socket activeClient;
    private Thread acceptThread;

    LoggingServer(LoggingServerConfig config, LoggingControl control) {
        if (config == null) {
            throw new IllegalArgumentException("logging server config must not be null");
        }
        if (control == null) {
            throw new IllegalArgumentException("logging control must not be null");
        }
        this.bindAddress = config.bindAddress();
        this.port = config.port();
        this.control = control;
    }

    synchronized void start() throws IOException {
        if (serverSocket != null) {
            throw new IllegalStateException("logging server is already started");
        }
        ServerSocket socket = new ServerSocket();
        socket.bind(new InetSocketAddress(InetAddress.getByName(bindAddress), port), 1);
        serverSocket = socket;

        Thread thread = new Thread(this::acceptLoop, "event-timing-live-log");
        thread.setDaemon(true);
        acceptThread = thread;
        thread.start();
    }

    int boundPort() {
        ServerSocket socket = serverSocket;
        if (socket == null) {
            throw new IllegalStateException("logging server is not started");
        }
        return socket.getLocalPort();
    }

    void publish(LogRecord record) {
        if (activeClient == null || record == null) {
            return;
        }
        offer(logLine(record));
    }

    private void offer(String line) {
        if (!outbound.offer(line)) {
            outbound.poll();
            outbound.offer(line);
        }
    }

    private void acceptLoop() {
        while (!closed) {
            Socket client = null;
            try {
                client = serverSocket.accept();
                client.setTcpNoDelay(true);
                activeClient = client;
                outbound.clear();
                runSession(client);
            } catch (SocketException ex) {
                if (!closed) {
                    // Connection failure is deliberately isolated from normal logging.
                }
            } catch (IOException ex) {
                if (!closed) {
                    // Best-effort diagnostics: keep the listener alive when possible.
                }
            } finally {
                if (activeClient == client) {
                    activeClient = null;
                }
                closeSocket(client);
                outbound.clear();
            }
        }
    }

    private void runSession(Socket client) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8));

        Thread writerThread = new Thread(
                () -> writeLoop(client, writer),
                "event-timing-live-log-writer");
        writerThread.setDaemon(true);
        writerThread.start();

        offer(levelLine(control.level()));

        try {
            String command;
            while (!closed && activeClient == client && (command = reader.readLine()) != null) {
                handleCommand(command);
            }
        } finally {
            writerThread.interrupt();
        }
    }

    private void handleCommand(String command) {
        String value = command == null ? "" : command.trim();
        if ("GET_LEVEL".equalsIgnoreCase(value)) {
            offer(levelLine(control.level()));
            return;
        }
        if (value.regionMatches(true, 0, "SET_LEVEL ", 0, 10)) {
            String requested = value.substring(10).trim().toUpperCase();
            try {
                LoggingLevel level = LoggingLevel.valueOf(requested);
                control.setLevel(level);
                offer(levelLine(level));
            } catch (IllegalArgumentException ex) {
                offer(errorLine("Unsupported level: " + requested));
            }
            return;
        }
        offer(errorLine("Unsupported command"));
    }

    private void writeLoop(Socket client, BufferedWriter writer) {
        try {
            while (!closed && activeClient == client && !Thread.currentThread().isInterrupted()) {
                String line = outbound.poll(250, TimeUnit.MILLISECONDS);
                if (line != null) {
                    writer.write(line);
                    writer.newLine();
                    writer.flush();
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (IOException ex) {
            closeSocket(client);
        }
    }

    private String logLine(LogRecord record) {
        String thrown = record.getThrown() == null ? null : record.getThrown().toString();
        return "{\"type\":\"log\",\"occurredAt\":\""
                + escape(Instant.ofEpochMilli(record.getMillis()).toString())
                + "\",\"level\":\""
                + LoggingControl.semanticLevel(record.getLevel())
                + "\",\"logger\":\""
                + escape(record.getLoggerName() == null ? "" : record.getLoggerName())
                + "\",\"source\":\""
                + escape(CompactLogFormatter.source(record))
                + "\",\"message\":\""
                + escape(formatter.message(record))
                + "\",\"formatted\":\""
                + escape(formatter.format(record))
                + "\""
                + (thrown == null ? "" : ",\"thrown\":\"" + escape(thrown) + "\"")
                + "}";
    }

    private static String levelLine(LoggingLevel level) {
        return "{\"type\":\"level\",\"level\":\"" + level.name() + "\"}";
    }

    private static String errorLine(String message) {
        return "{\"type\":\"error\",\"message\":\"" + escape(message) + "\"}";
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
            }
        }
        return escaped.toString();
    }

    @Override
    public synchronized void close() {
        closed = true;
        closeSocket(activeClient);
        activeClient = null;

        ServerSocket socket = serverSocket;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // Best-effort diagnostics cleanup.
            }
            serverSocket = null;
        }

        Thread thread = acceptThread;
        if (thread != null && thread != Thread.currentThread()) {
            try {
                thread.join(1000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        acceptThread = null;
        outbound.clear();
    }

    private static void closeSocket(Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
            // Best-effort diagnostics cleanup.
        }
    }
}
