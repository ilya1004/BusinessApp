package oll.businessdesktop;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import oll.businessdesktop.model.ProcessInstance;
import oll.businessdesktop.model.Task;
import oll.businessdesktop.model.User;
import oll.businessdesktop.model.KpiInstanceData;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ProcessInstancesController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @FXML private ComboBox<String> instanceSelector;
    @FXML private Label statusLabel;
    @FXML private VBox infoContainer;
    @FXML private HBox processInfoRows;
    @FXML private TableView<TaskRow> taskTable;
    @FXML private TableColumn<TaskRow, Long> colId;
    @FXML private TableColumn<TaskRow, String> colElementId;
    @FXML private TableColumn<TaskRow, String> colName;
    @FXML private TableColumn<TaskRow, String> colStatus;
    @FXML private TableColumn<TaskRow, String> colAssignee;
    @FXML private TableColumn<TaskRow, String> colDuration;
    @FXML private TableColumn<TaskRow, Double> colDeviation;
    @FXML private TableColumn<TaskRow, String> colKpiWeight;
    @FXML private TableColumn<TaskRow, String> colStarted;
    @FXML private TableColumn<TaskRow, String> colDueDate;
    @FXML private TableColumn<TaskRow, Void> colActions;

    private List<ProcessInstance> allInstances;
    private ProcessInstance currentInstance;
    private List<User> allUsers = new ArrayList<>();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colElementId.setCellValueFactory(new PropertyValueFactory<>("elementId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAssignee.setCellValueFactory(new PropertyValueFactory<>("assigneeName"));
        colDuration.setCellValueFactory(new PropertyValueFactory<>("durationInfo"));
        colStarted.setCellValueFactory(new PropertyValueFactory<>("startedAt"));
        colDueDate.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        colDeviation.setCellValueFactory(new PropertyValueFactory<>("deviationPercent"));
        colKpiWeight.setCellValueFactory(new PropertyValueFactory<>("kpiWeight"));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final HBox actionBox = new HBox(4);
            private final Button changeStatusBtn = new Button("Status");
            private final Button assignBtn = new Button("Assign");

            {
                changeStatusBtn.getStyleClass().add("table-action-button");
                assignBtn.getStyleClass().add("table-assign-button");
                changeStatusBtn.setOnAction(e -> openStatusDialog(getTableRow().getItem()));
                assignBtn.setOnAction(e -> openAssignDialog(getTableRow().getItem()));
                actionBox.getChildren().addAll(changeStatusBtn, assignBtn);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(actionBox);
            }
        });

        loadUsers();
        loadInstanceList();
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
                "PENDING", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "OVERDUE", "CANCELLED"
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
                instanceSelector.setValue(inst.model() != null ? inst.model().name() : "Unknown");
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
        loadTasks(currentInstance.id());
        loadInstanceKpi(currentInstance.id());
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

    private void updateInstanceKpiInfo(KpiInstanceData kpi) {
        processInfoRows.getChildren().clear();
        addInfoItem("ID", String.valueOf(kpi.instanceId()));
        addInfoItem("Model", kpi.modelId());
        addInfoItem("Status", kpi.status());
        addInfoItem("Started", formatDateTime(kpi.startedAt()));
        addInfoItem("Finished", formatDateTime(kpi.finishedAt()));
        addInfoItem("Planned", kpi.plannedDuration() + " min");
        addInfoItem("Actual", kpi.actualDuration() + " min");
        addInfoItem("Deviation", String.format("%.1f%%", kpi.deviationPercent()));
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
        new Thread(() -> {
            try {
                List<Task> tasks = ApiService.getTasksByInstance(instanceId);
                Platform.runLater(() -> {
                    List<TaskRow> rows = new ArrayList<>();
                    for (Task t : tasks) {
                        int planned = t.plannedDuration() != null ? t.plannedDuration() : 0;
                        int actual = t.actualDuration() != null ? t.actualDuration() : 0;
                        double dev = planned > 0 ? ((double) (actual - planned) / planned) * 100.0 : 0.0;
                        String kpiW = t.taskDefinition() != null && t.taskDefinition().getKpiWeight() != null ?
                                t.taskDefinition().getKpiWeight().toPlainString() : "-";
                        String assignee = t.getAssigneeName();
                        String due = formatDateTime(t.dueDate());
                        String started = formatDateTime(t.startedAt());
                        rows.add(new TaskRow(t.id(),
                                t.taskDefinition() != null ? t.taskDefinition().bpmnElementId() : "-",
                                t.getTaskName(),
                                t.status(),
                                assignee,
                                planned + " / " + actual + " min",
                                dev,
                                kpiW,
                                started,
                                due,
                                t));
                    }
                    taskTable.setItems(FXCollections.observableArrayList(rows));
                    statusLabel.setText("Loaded " + rows.size() + " tasks");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Failed to load tasks: " + e.getMessage());
                });
            }
        }).start();
    }

    private void loadInstanceKpi(Long instanceId) {
        new Thread(() -> {
            try {
                KpiInstanceData kpi = ApiService.getInstanceKpi(instanceId);
                Platform.runLater(() -> updateInstanceKpiInfo(kpi));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("KPI data not available for this instance");
                });
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
        private final Double deviationPercent;
        private final String kpiWeight;
        private final String startedAt;
        private final String dueDate;
        private final Task task;

        public TaskRow(Long id, String elementId, String name, String status,
                       String assigneeName, String durationInfo, Double deviationPercent, String kpiWeight,
                       String startedAt, String dueDate, Task task) {
            this.id = id;
            this.elementId = elementId;
            this.name = name;
            this.status = status;
            this.assigneeName = assigneeName;
            this.durationInfo = durationInfo;
            this.deviationPercent = deviationPercent;
            this.kpiWeight = kpiWeight;
            this.startedAt = startedAt;
            this.dueDate = dueDate;
            this.task = task;
        }

        public Long getId() { return id; }
        public String getElementId() { return elementId; }
        public String getName() { return name; }
        public String getStatus() { return status; }
        public String getAssigneeName() { return assigneeName; }
        public String getDurationInfo() { return durationInfo; }
        public Double getDeviationPercent() { return deviationPercent; }
        public String getKpiWeight() { return kpiWeight; }
        public String getStartedAt() { return startedAt; }
        public String getDueDate() { return dueDate; }
        public Task getTask() { return task; }
    }
}
