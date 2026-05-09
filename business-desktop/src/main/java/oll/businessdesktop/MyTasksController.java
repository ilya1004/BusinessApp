package oll.businessdesktop;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import oll.businessdesktop.model.KpiUserStats;
import oll.businessdesktop.model.Task;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class MyTasksController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM, HH:mm");

    @FXML private TableView<MyTaskRow> tasksTable;
    @FXML private TableColumn<MyTaskRow, String> colTaskName;
    @FXML private TableColumn<MyTaskRow, String> colProcessName;
    @FXML private TableColumn<MyTaskRow, String> colDeadline;
    @FXML private TableColumn<MyTaskRow, Void> colStatus;
    @FXML private TableColumn<MyTaskRow, Void> colActions;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label statusLabel;
    @FXML private VBox ratingCard;
    @FXML private Label ratingLabel;
    @FXML private Label weeklyLabel;
    @FXML private Label loadLabel;
    @FXML private VBox chartCard;
    @FXML private LineChart<String, Number> activityChart;
    @FXML private CategoryAxis chartXAxis;
    @FXML private NumberAxis chartYAxis;

    @FXML
    public void initialize() {
        colTaskName.setCellValueFactory(new PropertyValueFactory<>("taskName"));
        colProcessName.setCellValueFactory(new PropertyValueFactory<>("processName"));
        colDeadline.setCellValueFactory(new PropertyValueFactory<>("deadline"));

        colStatus.setCellFactory(col -> new StatusCell(this));
        colActions.setCellFactory(col -> new ActionCell(this));

        loadMyTasks();
        loadUserStats();
    }

    private void loadMyTasks() {
        showLoading(true);
        statusLabel.setText("Loading tasks...");

        javafx.concurrent.Task<List<Task>> fetchTask = new javafx.concurrent.Task<>() {
            @Override
            protected List<Task> call() throws Exception {
                return ApiService.getMyTasks();
            }
        };

        fetchTask.setOnSucceeded(e -> {
            showLoading(false);
            List<Task> tasks = fetchTask.getValue();
            if (tasks != null) {
                List<MyTaskRow> rows = tasks.stream().map(this::toRow).toList();
                tasksTable.setItems(FXCollections.observableArrayList(rows));
                statusLabel.setText(rows.size() + " task(s)");
            }
        });

        fetchTask.setOnFailed(e -> {
            showLoading(false);
            showError("Failed to load tasks: " + fetchTask.getException().getMessage());
        });

        new Thread(fetchTask).start();
    }

    private void loadUserStats() {
        new Thread(() -> {
            try {
                KpiUserStats stats = ApiService.getCurrentUserStats();
                Platform.runLater(() -> updateStats(stats));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    ratingLabel.setText("-");
                    weeklyLabel.setText("Unavailable");
                    loadLabel.setText("");
                });
            }
        }).start();
    }

    private void updateStats(KpiUserStats stats) {
        if (stats == null) return;

        double rating = stats.rating() != null ? stats.rating() * 100.0 : 0.0;
        ratingLabel.setText(String.format("%.0f%%", rating));

        int weekly = stats.weeklyCompleted() != null ? stats.weeklyCompleted() : 0;
        weeklyLabel.setText(weekly + " completed this week");

        double load = stats.loadPercent() != null ? stats.loadPercent() : 0.0;
        loadLabel.setText(String.format("Load: %.0f%%", load));

        if (stats.ratingHistory() != null && !stats.ratingHistory().isEmpty()) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            for (KpiUserStats.RatingHistoryPoint point : stats.ratingHistory()) {
                String dayLabel = point.date() != null && point.date().length() > 5 ?
                        point.date().substring(5) : point.date();
                series.getData().add(new XYChart.Data<>(dayLabel, point.rating() != null ? point.rating() * 100.0 : 0.0));
            }
            activityChart.getData().clear();
            activityChart.getData().add(series);
            chartCard.setVisible(true);
        } else {
            chartCard.setVisible(false);
        }
    }

    private MyTaskRow toRow(Task t) {
        String processName = t.instance() != null && t.instance().model() != null ?
                t.instance().model().name() : "-";
        String deadline = t.dueDate() != null ? t.dueDate().format(DATE_FMT) : "-";
        return new MyTaskRow(t.id(), t.getTaskName(), processName, deadline, t.status(), t);
    }

    void onChangeStatus(MyTaskRow row) {
        if (row == null || "COMPLETED".equals(row.getStatus())) return;

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Change Status");
        dialog.setHeaderText("Task: " + row.getTaskName());

        ButtonType applyBtn = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(applyBtn, ButtonType.CANCEL);

        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList(
                "PENDING", "IN_PROGRESS"
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

        dialog.showAndWait().ifPresent(status -> setTaskStatus(row, status));
    }

    private void setTaskStatus(MyTaskRow row, String status) {
        statusLabel.setText("Updating status...");
        showLoading(true);

        javafx.concurrent.Task<Task> updateTask = new javafx.concurrent.Task<>() {
            @Override
            protected Task call() throws Exception {
                return ApiService.updateTaskStatus(row.getTask().id(), status);
            }
        };

        updateTask.setOnSucceeded(e -> {
            showLoading(false);
            Task updated = updateTask.getValue();
            if (updated != null) {
                int idx = tasksTable.getItems().indexOf(row);
                if (idx >= 0) {
                    tasksTable.getItems().set(idx, toRow(updated));
                }
                statusLabel.setText("Status set to " + status);
            }
        });

        updateTask.setOnFailed(e -> {
            showLoading(false);
            showError("Failed to update status: " + updateTask.getException().getMessage());
        });

        new Thread(updateTask).start();
    }

    void onCompleteTask(MyTaskRow row) {
        if (row == null || "COMPLETED".equals(row.getStatus())) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Complete Task");
        confirm.setHeaderText("Complete: " + row.getTaskName());
        confirm.setContentText("This action cannot be undone. The task status will be locked.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                completeTaskConfirmed(row);
            }
        });
    }

    private void completeTaskConfirmed(MyTaskRow row) {
        statusLabel.setText("Completing task...");
        showLoading(true);

        javafx.concurrent.Task<Task> completeTask = new javafx.concurrent.Task<>() {
            @Override
            protected Task call() throws Exception {
                return ApiService.completeTask(row.getTask().id());
            }
        };

        completeTask.setOnSucceeded(e -> {
            showLoading(false);
            Task updated = completeTask.getValue();
            if (updated != null) {
                int idx = tasksTable.getItems().indexOf(row);
                if (idx >= 0) {
                    tasksTable.getItems().set(idx, toRow(updated));
                }
                statusLabel.setText("Task completed and locked");
                loadUserStats();
            }
        });

        completeTask.setOnFailed(e -> {
            showLoading(false);
            showError("Failed to complete: " + completeTask.getException().getMessage());
        });

        new Thread(completeTask).start();
    }

    @FXML
    private void onRefresh() {
        loadMyTasks();
        loadUserStats();
    }

    private void showLoading(boolean loading) {
        Platform.runLater(() -> progressIndicator.setVisible(loading));
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message);
            alert.setTitle("Error");
            alert.showAndWait();
        });
    }

    public static class MyTaskRow {
        private final Long id;
        private final String taskName;
        private final String processName;
        private final String deadline;
        private final String status;
        private final Task task;

        public MyTaskRow(Long id, String taskName, String processName, String deadline, String status, Task task) {
            this.id = id;
            this.taskName = taskName;
            this.processName = processName;
            this.deadline = deadline;
            this.status = status;
            this.task = task;
        }

        public Long getId() { return id; }
        public String getTaskName() { return taskName; }
        public String getProcessName() { return processName; }
        public String getDeadline() { return deadline; }
        public String getStatus() { return status; }
        public Task getTask() { return task; }
    }

    private static class StatusCell extends TableCell<MyTaskRow, Void> {
        private final HBox box = new HBox(6);
        private final Label badge = new Label();
        private final Button changeBtn = new Button("Change");
        private final MyTasksController controller;

        StatusCell(MyTasksController controller) {
            this.controller = controller;
            badge.setStyle("-fx-padding: 3 8; -fx-background-radius: 4;");
            changeBtn.getStyleClass().add("btn-primary");
            changeBtn.setOnAction(e -> controller.onChangeStatus(getTableRow().getItem()));
            box.getChildren().addAll(badge, changeBtn);
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
            badge.setStyle("-fx-background-color: " + getStatusColor(status) + "; -fx-background-radius: 4; -fx-padding: 3 8;");
            changeBtn.setVisible(!"COMPLETED".equals(status));
            changeBtn.setDisable("COMPLETED".equals(status));
            setGraphic(box);
        }

        private String getStatusColor(String status) {
            switch (status) {
                case "PENDING": return "-color-bg-subtle";
                case "IN_PROGRESS": return "-color-attention-muted";
                case "COMPLETED": return "-color-success-muted";
                case "OVERDUE": case "FAILED": return "-color-danger-muted";
                default: return "-color-bg-subtle";
            }
        }
    }

    private static class ActionCell extends TableCell<MyTaskRow, Void> {
        private final Button completeBtn = new Button("Complete");
        private final MyTasksController controller;

        ActionCell(MyTasksController controller) {
            this.controller = controller;
            completeBtn.getStyleClass().add("btn-primary");
            completeBtn.setOnAction(e -> controller.onCompleteTask(getTableRow().getItem()));
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTableRow().getItem() == null) {
                setGraphic(null);
                return;
            }
            String status = getTableRow().getItem().getStatus();
            completeBtn.setDisable("COMPLETED".equals(status));
            setGraphic(completeBtn);
        }
    }
}
