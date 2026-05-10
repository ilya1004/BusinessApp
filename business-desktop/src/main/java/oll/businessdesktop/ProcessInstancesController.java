package oll.businessdesktop;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import oll.businessdesktop.model.ProcessInstance;
import oll.businessdesktop.model.KpiInstanceData;
import oll.businessdesktop.model.Task;
import oll.businessdesktop.model.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.Objects;

public class ProcessInstancesController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @FXML private ComboBox<String> instanceSelector;
    @FXML private Label statusLabel;
    @FXML private VBox infoContainer;
    @FXML private HBox processInfoRows;
    @FXML private WebView diagramViewer;
    @FXML private VBox kpiCardsContainer;
    @FXML private Label kpiPlannedLabel;
    @FXML private Label kpiActualLabel;
    @FXML private Label kpiOnTimeLabel;
    @FXML private Label kpiDelayLabel;
    @FXML private TableView<TaskRow> taskTable;
    @FXML private TableColumn<TaskRow, Long> colId;
    @FXML private TableColumn<TaskRow, String> colName;
    @FXML private TableColumn<TaskRow, Void> colStatus;
    @FXML private TableColumn<TaskRow, String> colAssignee;
    @FXML private TableColumn<TaskRow, String> colDuration;
    @FXML private TableColumn<TaskRow, Void> colActions;

    private List<ProcessInstance> allInstances;
    private ProcessInstance currentInstance;
    private List<User> allUsers = new ArrayList<>();
    private List<Task> currentTasks = new ArrayList<>();

    private WebEngine viewerEngine;
    private boolean viewerReady = false;
    private String currentBpmnXml;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            {
                badge.setStyle("-fx-padding: 3 8; -fx-background-radius: 4;");
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                String status = getTableRow().getItem().getStatus();
                badge.setText(status);
                badge.setStyle(getStatusStyle(status));
                setGraphic(badge);
            }
            private String getStatusStyle(String status) {
                return switch (status) {
                    case "PENDING" -> "-fx-background-color: #f0f0f0; -fx-background-radius: 4; -fx-padding: 3 8;";
                    case "ASSIGNED" -> "-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-background-radius: 4; -fx-padding: 3 8;";
                    case "IN_PROGRESS" -> "-fx-background-color: #fef3c7; -fx-background-radius: 4; -fx-padding: 3 8;";
                    case "COMPLETED" -> "-fx-background-color: #d1fae5; -fx-background-radius: 4; -fx-padding: 3 8;";
                    case "CANCELLED" -> "-fx-background-color: #e5e7eb; -fx-text-fill: #6b7280; -fx-background-radius: 4; -fx-padding: 3 8;";
                    case "OVERDUE" -> "-fx-background-color: #fee2e2; -fx-background-radius: 4; -fx-padding: 3 8;";
                    default -> "-fx-background-color: #f0f0f0; -fx-background-radius: 4; -fx-padding: 3 8;";
                };
            }
        });
        colAssignee.setCellValueFactory(new PropertyValueFactory<>("assigneeName"));
        colDuration.setCellValueFactory(new PropertyValueFactory<>("durationInfo"));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final HBox actionBox = new HBox(6);
            private final Button changeStatusBtn = new Button("Status");
            private final Button assignBtn = new Button("Assign");
            private final MenuButton moreBtn = new MenuButton("...");
            private final MenuItem completeItem = new MenuItem("Complete");
            private final MenuItem cancelItem = new MenuItem("Cancel");

            {
                changeStatusBtn.getStyleClass().add("table-action-button");
                assignBtn.getStyleClass().add("table-assign-button");
                moreBtn.getStyleClass().add("table-menu-button");

                completeItem.setOnAction(e -> onCompleteTask(getTableRow().getItem()));
                cancelItem.setOnAction(e -> onCancelTask(getTableRow().getItem()));
                moreBtn.getItems().addAll(completeItem, cancelItem);

                changeStatusBtn.setOnAction(e -> openStatusDialog(getTableRow().getItem()));
                assignBtn.setOnAction(e -> openAssignDialog(getTableRow().getItem()));

                actionBox.getChildren().addAll(changeStatusBtn, assignBtn, moreBtn);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                String status = getTableRow().getItem().getStatus();
                boolean isLocked = "COMPLETED".equals(status) || "CANCELLED".equals(status);
                moreBtn.setDisable(isLocked);
                setGraphic(actionBox);
            }
        });

        viewerEngine = diagramViewer.getEngine();
        viewerEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                viewerReady = true;
                if (currentBpmnXml != null) {
                    renderDiagram(currentBpmnXml);
                }
            }
        });

        String viewerPath = Objects.requireNonNull(getClass().getResource("/bpmn-viewer.html")).toExternalForm();
        viewerEngine.load(viewerPath);

        loadUsers();
        loadInstanceList();
    }

    @FXML
    private void onZoomToFit() {
        if (viewerReady) {
            try {
                viewerEngine.executeScript("zoomToFit()");
            } catch (Exception e) {
                System.err.println("zoomToFit error: " + e.getMessage());
            }
        }
    }

    private void renderDiagram(String xml) {
        if (!viewerReady || xml == null) return;
        try {
            String escapedXml = xml.replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n");
            viewerEngine.executeScript("renderDiagram('" + escapedXml + "')");
        } catch (Exception e) {
            System.err.println("renderDiagram error: " + e.getMessage());
        }
    }

    private void loadUsers() {
        new Thread(() -> {
            try {
                allUsers = ApiService.getAllUsers();
            } catch (Exception e) {
                allUsers = new ArrayList<>();
            }
        }).start();
    }

    private void changeTaskStatus(TaskRow row, String status) {
        if (row == null) return;

        statusLabel.setText("Updating task...");
        new Thread(() -> {
            try {
                ApiService.updateTaskStatus(row.getTask().id(), status);
                loadTasks(currentInstance.id());
                Platform.runLater(() -> statusLabel.setText("Task updated to " + status));
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Failed: " + e.getMessage()));
            }
        }).start();
    }

    private void assignTask(TaskRow row, Long assigneeId) {
        if (row == null) return;

        statusLabel.setText("Assigning task...");
        new Thread(() -> {
            try {
                ApiService.assignTask(row.getTask().id(), assigneeId);
                loadTasks(currentInstance.id());
                Platform.runLater(() -> statusLabel.setText("Task assigned"));
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Assign failed: " + e.getMessage()));
            }
        }).start();
    }

    private void openStatusDialog(TaskRow row) {
        if (row == null || currentInstance == null) return;

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Change Task Status");
        dialog.setHeaderText("Task: " + row.getName());

        ButtonType applyBtn = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(applyBtn, ButtonType.CANCEL);

        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList(
                "PENDING", "ASSIGNED", "IN_PROGRESS", "OVERDUE"
        ));
        statusCombo.setValue(row.getStatus());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Status:"), 0, 0);
        grid.add(statusCombo, 1, 0);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == applyBtn) {
                return statusCombo.getValue();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(status -> changeTaskStatus(row, status));
    }

    private void openAssignDialog(TaskRow row) {
        if (row == null || currentInstance == null) return;

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Assign Task");
        dialog.setHeaderText("Task: " + row.getName());

        ButtonType applyBtn = new ButtonType("Assign", ButtonBar.ButtonData.OK_DONE);
        ButtonType unassignBtn = new ButtonType("Unassign", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().setAll(applyBtn, unassignBtn, ButtonType.CANCEL);

        ComboBox<User> userCombo = new ComboBox<>();
        userCombo.getItems().addAll(allUsers);
        userCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(u == null || empty ? null : u.firstName() + " " + u.lastName() + " (" + u.username() + ")");
            }
        });
        userCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(u == null || empty ? "Select user..." : u.firstName() + " " + u.lastName());
            }
        });
        if (row.getTask().assignee() != null) {
            for (User u : allUsers) {
                if (u.id().equals(row.getTask().assignee().id())) {
                    userCombo.setValue(u);
                    break;
                }
            }
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Assignee:"), 0, 0);
        grid.add(userCombo, 1, 0);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == applyBtn) {
                if (userCombo.getValue() == null) {
                    showAlert("Select a user");
                    return null;
                }
                return "assign:" + userCombo.getValue().id();
            }
            if (btn == unassignBtn) {
                return "unassign";
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            if (result.startsWith("assign:")) {
                Long assigneeId = Long.parseLong(result.substring(7));
                assignTask(row, assigneeId);
            } else {
                unassignTask(row);
            }
        });
    }

    private void unassignTask(TaskRow row) {
        if (row == null) return;

        statusLabel.setText("Unassigning task...");
        new Thread(() -> {
            try {
                ApiService.unassignTask(row.getTask().id());
                loadTasks(currentInstance.id());
                Platform.runLater(() -> statusLabel.setText("Task unassigned"));
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Unassign failed: " + e.getMessage()));
            }
        }).start();
    }

    private void onCompleteTask(TaskRow row) {
        if (row == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Complete Task");
        confirm.setHeaderText("Complete: " + row.getName());
        confirm.setContentText("This action cannot be undone. The task will be locked.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                statusLabel.setText("Completing task...");
                new Thread(() -> {
                    try {
                        ApiService.completeTask(row.getTask().id());
                        loadTasks(currentInstance.id());
                        Platform.runLater(() -> statusLabel.setText("Task completed and locked"));
                    } catch (Exception e) {
                        Platform.runLater(() -> statusLabel.setText("Complete failed: " + e.getMessage()));
                    }
                }).start();
            }
        });
    }

    private void onCancelTask(TaskRow row) {
        if (row == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Task");
        confirm.setHeaderText("Cancel: " + row.getName());
        confirm.setContentText("This action cannot be undone. The task will be locked.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                statusLabel.setText("Cancelling task...");
                new Thread(() -> {
                    try {
                        ApiService.cancelTask(row.getTask().id());
                        loadTasks(currentInstance.id());
                        Platform.runLater(() -> statusLabel.setText("Task cancelled and locked"));
                    } catch (Exception e) {
                        Platform.runLater(() -> statusLabel.setText("Cancel failed: " + e.getMessage()));
                    }
                }).start();
            }
        });
    }

    public void selectInstanceById(Long instanceId) {
        if (allInstances == null) {
            loadInstanceList();
            Platform.runLater(() -> {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                if (allInstances != null) {
                    selectById(instanceId);
                }
            });
            return;
        }
        selectById(instanceId);
    }

    private void selectById(Long instanceId) {
        for (ProcessInstance inst : allInstances) {
            if (inst.id().equals(instanceId)) {
                instanceSelector.setValue(inst.model() != null ? inst.model().name() + " #" + inst.id() : "Instance #" + inst.id());
                onInstanceSelected();
                return;
            }
        }
    }

    private void loadInstanceList() {
        statusLabel.setText("Loading instances...");
        new Thread(() -> {
            try {
                allInstances = ApiService.getAllProcessInstances();
                Platform.runLater(() -> {
                    if (allInstances == null || allInstances.isEmpty()) {
                        instanceSelector.setItems(FXCollections.emptyObservableList());
                        statusLabel.setText("No process instances found");
                        return;
                    }
                    instanceSelector.setItems(FXCollections.observableArrayList(
                            allInstances.stream()
                                    .map(inst -> inst.model() != null ? inst.model().name() + " #" + inst.id() : "Instance #" + inst.id())
                                    .toList()));
                    statusLabel.setText(allInstances.size() + " instance(s) available");
                    instanceSelector.setOnAction(e -> onInstanceSelected());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Failed to load instances: " + e.getMessage());
                });
            }
        }).start();
    }

    private void onInstanceSelected() {
        String selected = instanceSelector.getValue();
        if (selected == null || allInstances == null) return;

        int idx = instanceSelector.getItems().indexOf(selected);
        if (idx < 0 || idx >= allInstances.size()) return;

        currentInstance = allInstances.get(idx);
        statusLabel.setText("Loading instance #" + currentInstance.id() + "...");

        showInstanceInfo(currentInstance);
        loadInstanceBpmn(currentInstance);
        loadTasks(currentInstance.id());
    }

    private void loadInstanceBpmn(ProcessInstance inst) {
        if (inst.model() == null || inst.model().bpmnXml() == null) return;

        String bpmnXml = inst.model().bpmnXml();
        currentBpmnXml = bpmnXml;
        renderDiagram(bpmnXml);
    }

    private void showInstanceInfo(ProcessInstance inst) {
        processInfoRows.getChildren().clear();
        addInfoItem("ID", String.valueOf(inst.id()));
        addInfoItem("Model", inst.model() != null ? inst.model().name() : "N/A");
        addInfoItem("Status", inst.status());
        addInfoItem("Started", formatDateTime(inst.startedAt()));
        addInfoItem("Finished", formatDateTime(inst.finishedAt()));
        addInfoItem("State", inst.currentState() != null ? inst.currentState() : "-");
    }

    private String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "-";
        return dt.format(DATE_FMT);
    }

    private void addInfoItem(String label, String value) {
        VBox item = new VBox();
        item.setSpacing(2);
        Label keyLabel = new Label(label);
        keyLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-fg-muted;");
        Label valLabel = new Label(value);
        valLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        item.getChildren().addAll(keyLabel, valLabel);
        processInfoRows.getChildren().add(item);
    }

    private void loadTasks(Long instanceId) {
        System.out.println("[ProcessInstancesController] loadTasks called for instanceId=" + instanceId);
        new Thread(() -> {
            try {
                List<Task> tasks = ApiService.getTasksByInstance(instanceId);
                System.out.println("[ProcessInstancesController] Fetched " + tasks.size() + " tasks from API");

                Platform.runLater(() -> {
                    List<TaskRow> rows = new ArrayList<>();
                    int totalPlanned = 0;
                    int totalActual = 0;
                    int completedCount = 0;
                    int delayedCount = 0;
                    int onTimeCount = 0;

                    for (Task t : tasks) {
                        int planned = t.plannedDuration() != null ? t.plannedDuration() : 0;
                        int actual = t.actualDuration() != null ? t.actualDuration() : 0;
                        String assignee = t.getAssigneeName();
                        System.out.println("[ProcessInstancesController] Task id=" + t.id() + " name=" + t.getTaskName()
                                + " status=" + t.status()
                                + " plannedDuration=" + t.plannedDuration()
                                + " actualDuration=" + t.actualDuration()
                                + " → display actual=" + actual + " planned=" + planned);

                        totalPlanned += planned;
                        totalActual += actual;



                        if ("COMPLETED".equals(t.status())) {
                            completedCount++;
                        }

                        String elementId = t.taskDefinition() != null ? t.taskDefinition().bpmnElementId() : null;

                        rows.add(new TaskRow(
                                t.id(),
                                elementId != null ? elementId : "-",
                                t.getTaskName(),
                                t.status(),
                                assignee,
                                (actual / 60) + " / " + (planned / 60) + " h",
                                t
                        ));
                    }

                    System.out.println("[ProcessInstancesController] totalPlanned=" + totalPlanned + " totalActual=" + totalActual
                            + " completedCount=" + completedCount + " delayedCount=" + delayedCount);

                    rows.sort(Comparator.comparing(TaskRow::getId));
                    taskTable.setItems(FXCollections.observableArrayList(rows));
                    currentTasks = tasks;
                    statusLabel.setText("Loaded " + rows.size() + " tasks");

                    // Update KPI cards from local task data
                    double delayRate = completedCount > 0 ? (double) delayedCount / completedCount : 0;
                    double onTimeRate = completedCount > 0 ? (double) onTimeCount / completedCount : 0;
                    double deviation = totalPlanned > 0 ? ((double) (totalActual - totalPlanned) / totalPlanned) * 100.0 : 0.0;

                    kpiPlannedLabel.setText((totalPlanned / 60) + " h");
                    kpiActualLabel.setText((totalActual / 60) + " h");
                    kpiOnTimeLabel.setText(String.format("%.0f%%", onTimeRate * 100));
                    kpiOnTimeLabel.setStyle(onTimeRate >= 0.8
                            ? "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -color-success-emphasis;"
                            : "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -color-warning-emphasis;");
                    kpiDelayLabel.setText(String.format("%.0f%%", delayRate * 100));
                    kpiDelayLabel.setStyle(delayRate > 0.2
                            ? "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -color-danger-emphasis;"
                            : "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -color-success-emphasis;");

                    kpiCardsContainer.setVisible(true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Failed to load tasks: " + e.getMessage());
                    kpiCardsContainer.setVisible(false);
                });
            }
        }).start();
    }

    private void loadInstanceKpi(Long instanceId) {
        new Thread(() -> {
            try {
                KpiInstanceData kpi = ApiService.getInstanceKpi(instanceId);
                Platform.runLater(() -> {
                    if (kpi != null) {
                        addInfoItem("Planned", (kpi.plannedDuration() / 60) + " h");
                        addInfoItem("Actual", (kpi.actualDuration() / 60) + " h");
                    }
                });
            } catch (Exception e) {
                // KPI data is best-effort; instance info still shows
            }
        }).start();
    }

    private void showAlert(String message) {
        Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, message).show());
    }

    public static class TaskRow {
        private final Long id;
        private final String elementId;
        private final String name;
        private final String status;
        private final String assigneeName;
        private final String durationInfo;
        private final Task task;

        public TaskRow(Long id, String elementId, String name, String status,
                       String assigneeName, String durationInfo, Task task) {
            this.id = id;
            this.elementId = elementId;
            this.name = name;
            this.status = status;
            this.assigneeName = assigneeName;
            this.durationInfo = durationInfo;
            this.task = task;
        }

        public Long getId() { return id; }
        public String getElementId() { return elementId; }
        public String getName() { return name; }
        public String getStatus() { return status; }
        public String getAssigneeName() { return assigneeName; }
        public String getDurationInfo() { return durationInfo; }
        public Task getTask() { return task; }
    }
}