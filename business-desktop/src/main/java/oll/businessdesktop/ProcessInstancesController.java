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
import oll.businessdesktop.model.KpiInstanceData;

import java.util.ArrayList;
import java.util.List;

public class ProcessInstancesController {

    @FXML private ComboBox<String> instanceSelector;
    @FXML private Label statusLabel;
    @FXML private VBox infoContainer;
    @FXML private HBox processInfoRows;
    @FXML private TableView<TaskRow> taskTable;
    @FXML private TableColumn<TaskRow, Long> colId;
    @FXML private TableColumn<TaskRow, String> colElementId;
    @FXML private TableColumn<TaskRow, String> colName;
    @FXML private TableColumn<TaskRow, String> colStatus;
    @FXML private TableColumn<TaskRow, String> colDuration;
    @FXML private TableColumn<TaskRow, Double> colDeviation;
    @FXML private TableColumn<TaskRow, String> colKpiWeight;
    @FXML private TableColumn<TaskRow, Void> colActions;

    private List<ProcessInstance> allInstances;
    private ProcessInstance currentInstance;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colElementId.setCellValueFactory(new PropertyValueFactory<>("elementId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDuration.setCellValueFactory(new PropertyValueFactory<>("durationInfo"));
        colDeviation.setCellValueFactory(new PropertyValueFactory<>("deviationPercent"));
        colKpiWeight.setCellValueFactory(new PropertyValueFactory<>("kpiWeight"));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button changeStatusBtn = new Button("Change Status");

            {
                changeStatusBtn.setOnAction(e -> openStatusDialog(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(changeStatusBtn);
            }
        });

        loadInstanceList();
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
        addInfoItem("Started", inst.startedAt() != null ? inst.startedAt().toString() : "N/A");
        addInfoItem("Finished", inst.finishedAt() != null ? inst.finishedAt().toString() : "-");
        addInfoItem("State", inst.currentState() != null ? inst.currentState() : "-");
    }

    private void updateInstanceKpiInfo(KpiInstanceData kpi) {
        processInfoRows.getChildren().clear();
        addInfoItem("ID", String.valueOf(kpi.instanceId()));
        addInfoItem("Model", kpi.modelId());
        addInfoItem("Status", kpi.status());
        addInfoItem("Started", kpi.startedAt() != null ? kpi.startedAt().toString() : "N/A");
        addInfoItem("Finished", kpi.finishedAt() != null ? kpi.finishedAt().toString() : "-");
        addInfoItem("Planned", kpi.plannedDuration() + " min");
        addInfoItem("Actual", kpi.actualDuration() + " min");
        addInfoItem("Deviation", String.format("%.1f%%", kpi.deviationPercent()));
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
                        rows.add(new TaskRow(t.id(),
                                t.taskDefinition() != null ? t.taskDefinition().bpmnElementId() : "-",
                                t.getTaskName(),
                                t.status(),
                                planned + " / " + actual + " min",
                                dev,
                                kpiW,
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

    public static class TaskRow {
        private final Long id;
        private final String elementId;
        private final String name;
        private final String status;
        private final String durationInfo;
        private final Double deviationPercent;
        private final String kpiWeight;
        private final Task task;

        public TaskRow(Long id, String elementId, String name, String status,
                       String durationInfo, Double deviationPercent, String kpiWeight, Task task) {
            this.id = id;
            this.elementId = elementId;
            this.name = name;
            this.status = status;
            this.durationInfo = durationInfo;
            this.deviationPercent = deviationPercent;
            this.kpiWeight = kpiWeight;
            this.task = task;
        }

        public Long getId() { return id; }
        public String getElementId() { return elementId; }
        public String getName() { return name; }
        public String getStatus() { return status; }
        public String getDurationInfo() { return durationInfo; }
        public Double getDeviationPercent() { return deviationPercent; }
        public String getKpiWeight() { return kpiWeight; }
        public Task getTask() { return task; }
    }
}
