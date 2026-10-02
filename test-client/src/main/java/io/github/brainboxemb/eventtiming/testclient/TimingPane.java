package io.github.brainboxemb.eventtiming.testclient;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/** Step-4 Timing tab presentation backed by the public IF-03 contract. */
final class TimingPane extends VBox {
    private static final int INITIAL_LOGBOOK_ROWS = 100;

    private final Supplier<ApiClient> clientSupplier;
    private final ExecutorService requests;
    private final Runnable connectLive;
    private final Runnable disconnectLive;
    private final TimingViewModel model = new TimingViewModel();

    private final Label connection = new Label("DISCONNECTED");
    private final Button connect = new Button("Connect live");
    private final Button disconnect = new Button("Disconnect");
    private final Button rebuild = new Button("Rebuild");

    private final ComboBox<String> node = new ComboBox<>();
    private final Label state = new Label("-");
    private final Label location = new Label("-");
    private final TextField locationInput = new TextField();
    private final Button setLocation = new Button("Set location");
    private final Button open = new Button("Open");
    private final Button close = new Button("Close");

    private final Label lastOperation = new Label("-");
    private final TextField registrationId = new TextField("N001");
    private final TextField registrationTime =
            new TextField(TimingViewModel.canonicalTime(Instant.now()));
    private final Button now = new Button("Now");
    private final Button autoReg = new Button("Auto-reg");

    private final Label logBookCount = new Label("0");
    private final TableView<ApiClient.TimingDataInfo> logBook = new TableView<>();

    private boolean updatingNodeSelection;
    private final List<ApiEventClient.ApiEvent> bufferedEvents = new ArrayList<>();

    TimingPane(
            Supplier<ApiClient> clientSupplier,
            ExecutorService requests,
            Runnable connectLive,
            Runnable disconnectLive) {
        if (clientSupplier == null || requests == null
                || connectLive == null || disconnectLive == null) {
            throw new IllegalArgumentException("TimingPane dependencies must not be null");
        }
        this.clientSupplier = clientSupplier;
        this.requests = requests;
        this.connectLive = connectLive;
        this.disconnectLive = disconnectLive;

        setSpacing(10);
        setPadding(new Insets(12));

        disconnect.setDisable(true);
        HBox connectionRow = new HBox(
                8,
                new Label("View"),
                connection,
                connect,
                disconnect,
                rebuild);

        node.setPrefWidth(230);
        locationInput.setPrefColumnCount(8);
        GridPane nodeGrid = new GridPane();
        nodeGrid.setHgap(12);
        nodeGrid.setVgap(8);
        nodeGrid.setPadding(new Insets(10));
        add(nodeGrid, 0, "TimingNode", node);
        add(nodeGrid, 1, "State", state);
        add(nodeGrid, 2, "LocationId", location);

        HBox locationRow = new HBox(
                8,
                new Label("LocationId"),
                locationInput,
                setLocation,
                open,
                close);
        nodeGrid.add(locationRow, 0, 3, 2, 1);

        TitledPane nodePane = new TitledPane("TimingNode", nodeGrid);
        nodePane.setCollapsible(false);

        registrationId.setPrefColumnCount(16);
        registrationTime.setPrefColumnCount(32);
        HBox registrationRow = new HBox(
                8,
                new Label("ID"),
                registrationId,
                new Label("Time"),
                registrationTime,
                now,
                autoReg);
        HBox.setHgrow(registrationTime, Priority.ALWAYS);

        VBox registrationBox = new VBox(
                8,
                registrationRow,
                new HBox(8, new Label("Last operation"), lastOperation));
        registrationBox.setPadding(new Insets(10));
        TitledPane registrationPane = new TitledPane("Auto-reg", registrationBox);
        registrationPane.setCollapsible(false);

        configureLogBook();
        VBox historyBox = new VBox(
                6,
                new HBox(8, new Label("Count"), logBookCount),
                logBook);
        VBox.setVgrow(logBook, Priority.ALWAYS);
        TitledPane historyPane = new TitledPane("LogBook", historyBox);
        historyPane.setCollapsible(false);
        VBox.setVgrow(historyPane, Priority.ALWAYS);

        getChildren().addAll(connectionRow, nodePane, registrationPane, historyPane);
        VBox.setVgrow(historyPane, Priority.ALWAYS);

        connect.setOnAction(event -> {
            model.viewState(TimingViewModel.ViewState.RECONNECTING);
            refresh();
            connectLive.run();
        });
        disconnect.setOnAction(event -> disconnectLive.run());
        rebuild.setOnAction(event -> rebuild());

        node.setOnAction(event -> {
            if (updatingNodeSelection) {
                return;
            }
            String selected = node.getValue();
            if (selected != null && !selected.equals(model.selectedNodeId())) {
                model.selectNode(selected);
                loadSelectedLogBook();
            }
        });

        setLocation.setOnAction(event -> setLocation());
        open.setOnAction(event -> runStateCommand(
                api -> api.open(requireSelectedNode())));
        close.setOnAction(event -> runStateCommand(
                api -> api.close(requireSelectedNode())));
        now.setOnAction(event ->
                registrationTime.setText(TimingViewModel.canonicalTime(Instant.now())));
        autoReg.setOnAction(event -> autoReg());

        refresh();
    }

    void connected() {
        bufferedEvents.clear();
        model.viewState(TimingViewModel.ViewState.RECONNECTING);
        connection.setText("CONNECTED / syncing");
        connect.setDisable(true);
        disconnect.setDisable(false);
        refresh();
    }

    void disconnected(boolean stale) {
        bufferedEvents.clear();
        model.viewState(stale
                ? TimingViewModel.ViewState.STALE
                : TimingViewModel.ViewState.DISCONNECTED);
        connection.setText(stale ? "STALE" : "DISCONNECTED");
        connect.setDisable(false);
        disconnect.setDisable(true);
        refresh();
    }

    void applyStatus(ApiClient.StatusResult status) {
        model.applyStatus(status);
        syncNodeChoice();
        refresh();
    }

    void applyStatusEvent(ApiEventClient.StatusEvent event) {
        if ("STATUS_SNAPSHOT".equals(event.eventType())) {
            applyStatus(event.status());
            rebuild();
            return;
        }

        if (model.viewState() == TimingViewModel.ViewState.RECONNECTING) {
            bufferedEvents.add(event);
            return;
        }
        if (model.viewState() != TimingViewModel.ViewState.LIVE) {
            return;
        }

        applyStatus(event.status());
    }

    void applyTimingDataEvent(ApiEventClient.TimingDataEvent event) {
        if (model.viewState() == TimingViewModel.ViewState.RECONNECTING) {
            bufferedEvents.add(event);
            return;
        }
        if (model.viewState() != TimingViewModel.ViewState.LIVE) {
            return;
        }

        model.mergeCommitted(event.timingData());
        refreshLogBook();
    }

    void rebuild() {
        bufferedEvents.clear();
        model.viewState(TimingViewModel.ViewState.RECONNECTING);
        connection.setText("RECONNECTING");
        refresh();

        final String preferredNode = model.selectedNodeId();
        final Long cachedLatest = model.latestSequence();

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        ApiClient api = clientSupplier.get();
                        ApiClient.StatusResult status = api.getStatus();
                        ApiClient.CapabilitiesResult capabilities = api.getCapabilities();
                        String target = chooseNode(status, preferredNode);
                        if (target == null) {
                            return new RebuildResult(
                                    status,
                                    capabilities,
                                    null,
                                    List.of(),
                                    true);
                        }

                        ApiClient.LogBookInfo info = api.getLogBookInfo(target);
                        boolean sameNode = target.equals(preferredNode);
                        boolean canAppendGap = sameNode
                                && cachedLatest != null
                                && info.last() != null
                                && cachedLatest.longValue() <= info.last().longValue();

                        List<ApiClient.LogBookPage> pages = new ArrayList<>();
                        boolean replace;
                        if (canAppendGap) {
                            replace = false;
                            long from = cachedLatest.longValue() + 1L;
                            while (info.last() != null && from <= info.last().longValue()) {
                                int limit = (int) Math.min(
                                        1000L,
                                        info.last().longValue() - from + 1L);
                                ApiClient.LogBookPage page =
                                        api.getLogBookFrom(target, from, limit);
                                pages.add(page);
                                if (page.next() == null) {
                                    break;
                                }
                                from = page.next().longValue();
                            }
                        } else {
                            replace = true;
                            if (info.count() > 0L) {
                                int last = (int) Math.min(
                                        INITIAL_LOGBOOK_ROWS,
                                        info.count());
                                pages.add(api.getLogBookLast(target, last));
                            }
                        }

                        return new RebuildResult(
                                status,
                                capabilities,
                                info,
                                List.copyOf(pages),
                                replace);
                    } catch (Exception ex) {
                        throw new CompletionException(ex);
                    }
                }, requests)
                .whenComplete((result, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        model.viewState(TimingViewModel.ViewState.STALE);
                        connection.setText("STALE");
                        lastOperation.setText("Rebuild failed: " + rootMessage(error));
                        refresh();
                        return;
                    }

                    model.applyStatus(result.status());
                    syncNodeChoice();
                    model.applyCapabilities(result.capabilities());
                    if (result.replace()) {
                        model.clearLogBook();
                    }
                    if (result.info() != null) {
                        model.applyLogBookInfo(result.info());
                    }
                    for (ApiClient.LogBookPage page : result.pages()) {
                        model.mergeLogBookPage(page);
                    }
                    applyBufferedEvents();
                    model.viewState(TimingViewModel.ViewState.LIVE);
                    connection.setText("LIVE");
                    refresh();
                }));
    }

    /**
     * Applies live events received after the rebuild baseline in delivery order.
     *
     * <p>This method runs on the JavaFX application thread. Events are buffered
     * only while the Timing view is RECONNECTING; raw Events-tab diagnostics are
     * still shown immediately by the outer application.</p>
     */
    private void applyBufferedEvents() {
        for (ApiEventClient.ApiEvent event : bufferedEvents) {
            if (event instanceof ApiEventClient.StatusEvent statusEvent) {
                model.applyStatus(statusEvent.status());
                syncNodeChoice();
            } else if (event instanceof ApiEventClient.TimingDataEvent timingDataEvent) {
                model.mergeCommitted(timingDataEvent.timingData());
            }
        }
        bufferedEvents.clear();
    }

    private void loadSelectedLogBook() {
        String selected = model.selectedNodeId();
        if (selected == null) {
            refresh();
            return;
        }

        bufferedEvents.clear();
        model.viewState(TimingViewModel.ViewState.RECONNECTING);
        connection.setText("RECONNECTING");
        refresh();

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        ApiClient api = clientSupplier.get();
                        ApiClient.LogBookInfo info = api.getLogBookInfo(selected);
                        ApiClient.LogBookPage page = info.count() == 0L
                                ? null
                                : api.getLogBookLast(
                                        selected,
                                        (int) Math.min(INITIAL_LOGBOOK_ROWS, info.count()));
                        return new NodeLogBook(info, page);
                    } catch (Exception ex) {
                        throw new CompletionException(ex);
                    }
                }, requests)
                .whenComplete((result, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        model.viewState(TimingViewModel.ViewState.STALE);
                        connection.setText("STALE");
                        lastOperation.setText("LogBook load failed: " + rootMessage(error));
                    } else {
                        model.clearLogBook();
                        model.applyLogBookInfo(result.info());
                        if (result.page() != null) {
                            model.mergeLogBookPage(result.page());
                        }
                        applyBufferedEvents();
                        model.viewState(TimingViewModel.ViewState.LIVE);
                        connection.setText("LIVE");
                    }
                    refresh();
                }));
    }

    private void setLocation() {
        final int value;
        try {
            value = Integer.parseInt(locationInput.getText().trim());
        } catch (NumberFormatException ex) {
            lastOperation.setText("LocationId must be an integer");
            return;
        }
        runStateCommand(api -> api.setLocation(requireSelectedNode(), value));
    }

    private void autoReg() {
        final String selected = requireSelectedNode();
        final String id = registrationId.getText().trim();
        final String time = registrationTime.getText().trim();

        setOperationBusy(true);
        lastOperation.setText("Submitting...");
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        ApiClient api = clientSupplier.get();
                        ApiClient.AutoRegResult result = api.autoReg(selected, id, time);
                        ApiClient.LogBookPage page =
                                api.getLogBookFrom(selected, result.seq(), 1);
                        ApiClient.StatusResult status = api.getStatus();
                        return new AutoRegCommand(result, page, status);
                    } catch (Exception ex) {
                        throw new CompletionException(ex);
                    }
                }, requests)
                .whenComplete((result, error) -> Platform.runLater(() -> {
                    setOperationBusy(false);
                    if (error != null) {
                        handleCommandError(error);
                        return;
                    }
                    lastOperation.setText("seq " + result.result().seq());
                    model.applyStatus(result.status());
                    model.mergeLogBookPage(result.page());
                    syncNodeChoice();
                    refresh();
                }));
    }

    private void runStateCommand(StateCommand command) {
        setOperationBusy(true);
        lastOperation.setText("Requesting...");
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        ApiClient api = clientSupplier.get();
                        ApiClient.OperationResult result = command.run(api);
                        ApiClient.StatusResult status = api.getStatus();
                        return new StateCommandResult(result, status);
                    } catch (Exception ex) {
                        throw new CompletionException(ex);
                    }
                }, requests)
                .whenComplete((result, error) -> Platform.runLater(() -> {
                    setOperationBusy(false);
                    if (error != null) {
                        handleCommandError(error);
                        return;
                    }
                    lastOperation.setText(result.result().result());
                    model.applyStatus(result.status());
                    syncNodeChoice();
                    refresh();
                }));
    }

    private void handleCommandError(Throwable error) {
        Throwable root = rootCause(error);
        if (root instanceof ApiClient.ApiException apiError) {
            lastOperation.setText(apiError.code() + ": " + apiError.getMessage());
            if ("OUTCOME_UNKNOWN".equals(apiError.code())) {
                model.viewState(TimingViewModel.ViewState.STALE);
                connection.setText("STALE");
                rebuild();
            }
        } else {
            lastOperation.setText("Error: " + rootMessage(error));
        }
        refresh();
    }

    private void setOperationBusy(boolean busy) {
        if (busy) {
            setLocation.setDisable(true);
            open.setDisable(true);
            close.setDisable(true);
            autoReg.setDisable(true);
        } else {
            refreshControls();
        }
    }

    private void refresh() {
        syncNodeChoice();
        ApiClient.TimingNodeInfo selected = model.selectedNode();
        state.setText(selected == null ? "-" : selected.state());
        location.setText(selected == null || selected.locationId() == null
                ? "-"
                : Integer.toString(selected.locationId()));
        refreshControls();
        refreshLogBook();
    }

    private void refreshControls() {
        TimingViewModel.Controls controls = model.controls();
        node.setDisable(model.viewState() != TimingViewModel.ViewState.LIVE
                || model.nodes().size() <= 1);
        locationInput.setDisable(!controls.setLocation());
        setLocation.setDisable(!controls.setLocation());
        open.setDisable(!controls.open());
        close.setDisable(!controls.close());
        registrationId.setDisable(!controls.autoReg());
        registrationTime.setDisable(!controls.autoReg());
        now.setDisable(!controls.autoReg());
        autoReg.setDisable(!controls.autoReg());
        rebuild.setDisable(model.viewState() == TimingViewModel.ViewState.RECONNECTING);
    }

    private void refreshLogBook() {
        logBookCount.setText(Long.toString(model.logBookCount()));
        logBook.setItems(FXCollections.observableArrayList(model.records()));
    }

    private void syncNodeChoice() {
        String selected = model.selectedNodeId();
        List<String> ids = model.nodes().stream()
                .map(ApiClient.TimingNodeInfo::id)
                .toList();
        updatingNodeSelection = true;
        try {
            node.setItems(FXCollections.observableArrayList(ids));
            node.setValue(selected);
        } finally {
            updatingNodeSelection = false;
        }
    }

    private void configureLogBook() {
        TableColumn<ApiClient.TimingDataInfo, String> seq = column(
                "Seq",
                value -> Long.toString(value.sequenceNumber()));
        TableColumn<ApiClient.TimingDataInfo, String> type = column(
                "Type",
                ApiClient.TimingDataInfo::origin);
        TableColumn<ApiClient.TimingDataInfo, String> loc = column(
                "Location",
                value -> Integer.toString(value.locationId()));
        TableColumn<ApiClient.TimingDataInfo, String> reg = column(
                "RegistrationId",
                ApiClient.TimingDataInfo::registrationId);
        TableColumn<ApiClient.TimingDataInfo, String> effective = column(
                "Effective",
                ApiClient.TimingDataInfo::effectiveTime);
        TableColumn<ApiClient.TimingDataInfo, String> recorded = column(
                "Recorded",
                ApiClient.TimingDataInfo::recordedAt);

        seq.setPrefWidth(65);
        type.setPrefWidth(90);
        loc.setPrefWidth(80);
        reg.setPrefWidth(130);
        effective.setPrefWidth(235);
        recorded.setPrefWidth(235);
        logBook.getColumns().setAll(seq, type, loc, reg, effective, recorded);
        logBook.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        logBook.setPlaceholder(new Label("No committed records"));
    }

    private static TableColumn<ApiClient.TimingDataInfo, String> column(
            String title,
            java.util.function.Function<ApiClient.TimingDataInfo, String> value) {
        TableColumn<ApiClient.TimingDataInfo, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return column;
    }

    private String requireSelectedNode() {
        String value = model.selectedNodeId();
        if (value == null) {
            throw new IllegalStateException("No TimingNode selected");
        }
        return value;
    }

    private static String chooseNode(
            ApiClient.StatusResult status,
            String preferredNode) {
        if (preferredNode != null) {
            for (ApiClient.TimingNodeInfo node : status.nodes()) {
                if (preferredNode.equals(node.id())) {
                    return preferredNode;
                }
            }
        }
        return status.nodes().isEmpty() ? null : status.nodes().get(0).id();
    }

    private static void add(GridPane grid, int row, String label, javafx.scene.Node value) {
        grid.add(new Label(label), 0, row);
        grid.add(value, 1, row);
    }

    private static Throwable rootCause(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static String rootMessage(Throwable error) {
        Throwable root = rootCause(error);
        String value = root.getMessage();
        return value == null || value.isBlank() ? root.toString() : value;
    }

    private record RebuildResult(
            ApiClient.StatusResult status,
            ApiClient.CapabilitiesResult capabilities,
            ApiClient.LogBookInfo info,
            List<ApiClient.LogBookPage> pages,
            boolean replace) {
    }

    private record NodeLogBook(
            ApiClient.LogBookInfo info,
            ApiClient.LogBookPage page) {
    }

    private record StateCommandResult(
            ApiClient.OperationResult result,
            ApiClient.StatusResult status) {
    }

    private record AutoRegCommand(
            ApiClient.AutoRegResult result,
            ApiClient.LogBookPage page,
            ApiClient.StatusResult status) {
    }

    @FunctionalInterface
    private interface StateCommand {
        ApiClient.OperationResult run(ApiClient api) throws Exception;
    }
}
