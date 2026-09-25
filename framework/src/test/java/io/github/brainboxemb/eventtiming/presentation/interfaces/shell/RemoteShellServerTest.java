package io.github.brainboxemb.eventtiming.presentation.interfaces.shell;

import io.github.brainboxemb.eventtiming.application.ApplicationStatus;
import io.github.brainboxemb.eventtiming.application.CommandHandler;
import io.github.brainboxemb.eventtiming.domain.timing.TimingNode;
import io.github.brainboxemb.eventtiming.domain.timing.TimingNodeId;
import io.github.brainboxemb.eventtiming.infra.BuildIdentity;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RemoteShellServerTest {

    @Test
    public void acceptsFirstCommandBeforeClientReadsBanner() throws Exception {
        RemoteShellServer server =
                new RemoteShellServer("127.0.0.1", 0, commandHandler(), () -> { });
        server.start();

        try (Socket client = connect(server.boundPort())) {
            Writer writer =
                    new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8);
            writer.write("help\n");
            writer.flush();

            String response = readUntil(client.getInputStream(), "event-timing> ");
            response += readUntil(client.getInputStream(), "event-timing> ");

            assertTrue(response.contains("Remote terminal ready."));
            assertTrue(response.contains("Commands:"));
            assertTrue(response.contains("help     Show available commands"));
            assertFalse(response.contains("Unknown command"));
        } finally {
            server.close();
        }
    }

    @Test
    public void reconnectsAfterDisconnectAndUsesSharedShutdownCommand() throws Exception {
        AtomicBoolean shutdown = new AtomicBoolean(false);
        RemoteShellServer server =
                new RemoteShellServer("127.0.0.1", 0, commandHandler(), () -> shutdown.set(true));
        server.start();

        try {
            try (Socket first = connect(server.boundPort())) {
                String banner = readUntil(first.getInputStream(), "event-timing> ");
                assertTrue(banner.contains("Remote terminal ready."));
            }

            try (Socket second = connect(server.boundPort())) {
                String banner = readUntil(second.getInputStream(), "event-timing> ");
                assertTrue(banner.contains("Remote terminal ready."));
                assertFalse(shutdown.get());

                Writer writer =
                        new OutputStreamWriter(second.getOutputStream(), StandardCharsets.UTF_8);
                writer.write("version\nstatus\nquit\n");
                writer.flush();

                String response = readToEnd(second.getInputStream());
                assertTrue(response.contains("Version      : test-version"));
                assertTrue(response.contains("Timing node"));
                assertTrue(response.contains("Id        : timing-node-01"));
                assertTrue(response.contains("Lifecycle : CLOSED"));
                assertTrue(response.contains("Stopping application."));
            }

            assertTrue(shutdown.get());
        } finally {
            server.close();
        }
    }

    private static Socket connect(int port) throws Exception {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", port), 1000);
        socket.setSoTimeout(2000);
        return socket;
    }

    private static String readUntil(InputStream input, String marker) throws Exception {
        StringBuilder value = new StringBuilder();
        while (value.indexOf(marker) < 0) {
            int next = input.read();
            if (next < 0) {
                break;
            }
            value.append((char) next);
        }
        return value.toString();
    }

    private static String readToEnd(InputStream input) throws Exception {
        StringBuilder value = new StringBuilder();
        int next;
        while ((next = input.read()) >= 0) {
            value.append((char) next);
        }
        return value.toString();
    }

    private static CommandHandler commandHandler() {
        BuildIdentity identity = BuildIdentity.firstApiVersion(
                "event-timing-app",
                "test-version",
                "abc123def456",
                "feature/test",
                "local",
                false);
        return new CommandHandler(
                identity,
                () -> new ApplicationStatus(
                        new TimingNodeId("timing-node-01"),
                        TimingNode.Lifecycle.CLOSED));
    }
}
