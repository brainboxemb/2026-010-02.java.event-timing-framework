package io.github.brainboxemb.eventtiming.testclient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientLogTest {
    @TempDir
    Path temp;

    @Test
    void retainsAndPublishesLocalClientRecords() throws Exception {
        AtomicReference<String> observed = new AtomicReference<>("");
        try (ClientLog log = ClientLog.open(temp, "INFO")) {
            log.subscribe(observed::set);
            log.debug("hidden");
            writeInfo(log);

            assertTrue(log.snapshot().contains("[INFO] - client started"));
            assertTrue(log.snapshot().contains(
                    "[testclient.ClientLogTest.writeInfo]"));
            assertEquals(log.snapshot(), observed.get());
        }

        try (var files = Files.list(temp)) {
            Path file = files.findFirst().orElseThrow();
            assertTrue(Files.readString(file).contains("client started"));
        }
    }

    private static void writeInfo(ClientLog log) {
        log.info("client started");
    }

}
