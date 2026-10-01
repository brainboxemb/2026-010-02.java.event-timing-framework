package io.github.brainboxemb.eventtiming.testclient;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimingViewModelTest {
    @Test
    void derivesControlsFromAuthoritativeState() {
        TimingViewModel model = new TimingViewModel();
        model.applyStatus(status(
                node("node-01", null, "CLOSED")));
        model.applyCapabilities(capabilities(true));

        assertEquals("node-01", model.selectedNodeId());
        assertFalse(model.controls().setLocation());

        model.viewState(TimingViewModel.ViewState.LIVE);
        assertTrue(model.controls().setLocation());
        assertFalse(model.controls().open());
        assertFalse(model.controls().close());
        assertFalse(model.controls().autoReg());

        model.applyStatus(status(
                node("node-01", 24, "CLOSED")));
        assertTrue(model.controls().setLocation());
        assertTrue(model.controls().open());
        assertFalse(model.controls().close());

        model.applyStatus(status(
                node("node-01", 24, "OPEN")));
        assertFalse(model.controls().setLocation());
        assertFalse(model.controls().open());
        assertTrue(model.controls().close());
        assertTrue(model.controls().autoReg());

        model.viewState(TimingViewModel.ViewState.STALE);
        assertFalse(model.controls().setLocation());
        assertFalse(model.controls().open());
        assertFalse(model.controls().close());
        assertFalse(model.controls().autoReg());
    }

    @Test
    void keepsSelectedNodeWhenStatusRefreshesAndClearsLogBookOnSelectionChange() {
        TimingViewModel model = new TimingViewModel();
        model.applyStatus(status(
                node("node-01", 24, "OPEN"),
                node("node-02", 25, "CLOSED")));
        model.selectNode("node-02");
        model.mergeCommitted(record("node-02", 1L, "N001"));

        model.applyStatus(status(
                node("node-01", 24, "OPEN"),
                node("node-02", 25, "OPEN")));

        assertEquals("node-02", model.selectedNodeId());
        assertEquals(1, model.records().size());

        model.selectNode("node-01");
        assertEquals("node-01", model.selectedNodeId());
        assertTrue(model.records().isEmpty());
        assertEquals(0L, model.logBookCount());
    }

    @Test
    void deduplicatesCommittedRecordsByStableKey() {
        TimingViewModel model = new TimingViewModel();
        model.applyStatus(status(node("node-01", 24, "OPEN")));
        model.applyLogBookInfo(new ApiClient.LogBookInfo(2L, 1L, 2L, "{}"));

        ApiClient.TimingDataInfo first = record("node-01", 1L, "N001");
        ApiClient.TimingDataInfo second = record("node-01", 2L, "N002");
        model.mergeLogBookPage(new ApiClient.LogBookPage(
                2L,
                null,
                List.of(first, second),
                "{}"));
        model.mergeCommitted(second);

        assertEquals(2, model.records().size());
        assertEquals(2L, model.logBookCount());
        assertEquals(2L, model.latestSequence());
    }

    @Test
    void ignoresCommittedEventForNonSelectedNode() {
        TimingViewModel model = new TimingViewModel();
        model.applyStatus(status(
                node("node-01", 24, "OPEN"),
                node("node-02", 25, "OPEN")));

        model.mergeCommitted(record("node-02", 1L, "N001"));

        assertTrue(model.records().isEmpty());
        assertNull(model.latestSequence());
    }

    @Test
    void formatsCanonicalNineDigitUtcTime() {
        assertEquals(
                "2026-10-01T12:00:00.123000000Z",
                TimingViewModel.canonicalTime(
                        Instant.parse("2026-10-01T12:00:00.123Z")));
    }

    private static ApiClient.StatusResult status(ApiClient.TimingNodeInfo... nodes) {
        return new ApiClient.StatusResult(List.of(nodes), List.of(), "{}");
    }

    private static ApiClient.TimingNodeInfo node(
            String id,
            Integer locationId,
            String state) {
        return new ApiClient.TimingNodeInfo(id, locationId, state);
    }

    private static ApiClient.CapabilitiesResult capabilities(boolean enabled) {
        return new ApiClient.CapabilitiesResult(
                List.of(new ApiClient.CapabilityInfo(
                        "DIRECT_REGISTRATION_SIMULATION",
                        true,
                        enabled)),
                "{}");
    }

    private static ApiClient.TimingDataInfo record(
            String nodeId,
            long sequence,
            String registrationId) {
        return new ApiClient.TimingDataInfo(
                nodeId,
                sequence,
                24,
                "REGISTRATION",
                "2026-10-01T12:00:00.000000000Z",
                "2026-10-01T12:00:00.125000000Z",
                registrationId,
                "AUTOMATIC",
                "OBSERVED");
    }
}
