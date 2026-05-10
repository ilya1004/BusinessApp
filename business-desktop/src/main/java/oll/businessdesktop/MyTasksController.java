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

import java.util.Comparator;
import java.util.List;

public class MyTasksController {

    @FXML private TableView<MyTaskRow> tasksTable;
    @FXML private TableColumn<MyTaskRow, String> colTaskName;
    @FXML private TableColumn<MyTaskRow, String> colProcessName;
    @FXML private TableColumn<MyTaskRow, String> colDuration;
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
        colDuration.setCellValueFactory(new PropertyValueFactory<>("durationInfo"));

        colStatus.setCellFactory(col -> new StatusCell(this));
        colActions.setCellFactory(col -> new ActionCell(this));

        loadMyTasks();
        loadUserStats();
    }

    private void loadMyTasks() {
        showLoading(true);
        statusLabel.setText("Загрузка задач...");

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
                List<MyTaskRow> rows = new java.util.ArrayList<>(tasks.stream().map(this::toRow).toList());
                rows.sort(Comparator.comparing(MyTaskRow::getId));
                tasksTable.setItems(FXCollections.observableArrayList(rows));
                statusLabel.setText(rows.size() + " задач(а)");
            }
        });

        fetchTask.setOnFailed(e -> {
            showLoading(false);
            showError("Ошибка загрузки задач: " + fetchTask.getException().getMessage());
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
                    weeklyLabel.setText("Недоступно");
                    loadLabel.setText("");
                });
            }
        }).start();
    }

    private void updateStats(KpiUserStats stats) {
        if (stats == null) return;

        double rating = stats.rating() != null ? stats.rating() * 100.0 : 0.0;
        ratingLabel.setText(String.format("%.0f", rating));

        int weekly = stats.weeklyCompleted() != null ? stats.weeklyCompleted() : 0;
        weeklyLabel.setText(weekly + " завершено на этой неделе");

        double load = stats.loadPercent() != null ? stats.loadPercent() : 0.0;
        loadLabel.setText(String.format("Загрузка: %.0f%%", load));

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
        int planned = t.plannedDuration() != null ? t.plannedDuration() : 0;
        int actual = t.actualDuration() != null ? t.actualDuration() : 0;
        String durationInfo = (actual / 60) + " / " + (planned / 60) + " h";
        return new MyTaskRow(t.id(), t.getTaskName(), processName, durationInfo, t.status(), t);
    }

    void setTaskStatus(MyTaskRow row, String status) {
        statusLabel.setText("Обновление статуса...");
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
                statusLabel.setText("Статус изменён на " + status);
            }
        });

        updateTask.setOnFailed(e -> {
            showLoading(false);
            showError("Ошибка обновления статуса: " + updateTask.getException().getMessage());
        });

        new Thread(updateTask).start();
    }

    void onCompleteTask(MyTaskRow row) {
        if (row == null || "COMPLETED".equals(row.getStatus())) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Завершить задачу");
        confirm.setHeaderText("Завершить: " + row.getTaskName());
        confirm.setContentText("Это действие нельзя отменить. Статус задачи будет заблокирован.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                completeTaskConfirmed(row);
            }
        });
    }

    private void completeTaskConfirmed(MyTaskRow row) {
        statusLabel.setText("Завершение задачи...");
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
                statusLabel.setText("Задача завершена и заблокирована");
                loadUserStats();
            }
        });

        completeTask.setOnFailed(e -> {
            showLoading(false);
            showError("Ошибка завершения: " + completeTask.getException().getMessage());
        });

        new Thread(completeTask).start();
    }

    void onLogTime(MyTaskRow row) {
        if (row == null) return;

        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Учёт времени");
        dialog.setHeaderText("Учёт времени для: " + row.getTaskName());

        ButtonType saveBtn = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextField hoursField = new TextField();
        hoursField.setPromptText("Отработано часов");

        int planned = row.getTask().plannedDuration() != null ? row.getTask().plannedDuration() / 60 : 0;
        hoursField.setText(String.valueOf(planned));

        hoursField.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d*(\\.\\d?)?" ) ? c : null));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Время (ч):"), 0, 0);
        grid.add(hoursField, 1, 0);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    double hours = Double.parseDouble(hoursField.getText().trim());
                    return (int) Math.round(hours * 60);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(minutes -> {
            if (minutes == null) return;
            statusLabel.setText("Сохранение времени...");
            showLoading(true);

            javafx.concurrent.Task<Task> logTask = new javafx.concurrent.Task<>() {
                @Override
                protected Task call() throws Exception {
                    return ApiService.logTime(row.getTask().id(), minutes);
                }
            };

            logTask.setOnSucceeded(e -> {
                showLoading(false);
                Task updated = logTask.getValue();
                if (updated != null) {
                    int idx = tasksTable.getItems().indexOf(row);
                    if (idx >= 0) {
                        tasksTable.getItems().set(idx, toRow(updated));
                    }
                    statusLabel.setText("Время сохранено (" + (minutes / 60) + " ч)");
                }
            });

            logTask.setOnFailed(e -> {
                showLoading(false);
                showError("Ошибка сохранения времени: " + logTask.getException().getMessage());
            });

            new Thread(logTask).start();
        });
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
            alert.setTitle("Ошибка");
            alert.showAndWait();
        });
    }

    public static class MyTaskRow {
        private final Long id;
        private final String taskName;
        private final String processName;
        private final String durationInfo;
        private final String status;
        private final Task task;

        public MyTaskRow(Long id, String taskName, String processName, String durationInfo, String status, Task task) {
            this.id = id;
            this.taskName = taskName;
            this.processName = processName;
            this.durationInfo = durationInfo;
            this.status = status;
            this.task = task;
        }

        public Long getId() { return id; }
        public String getTaskName() { return taskName; }
        public String getProcessName() { return processName; }
        public String getDurationInfo() { return durationInfo; }
        public String getStatus() { return status; }
        public Task getTask() { return task; }
    }

    private static class StatusCell extends TableCell<MyTaskRow, Void> {
        private final ComboBox<String> combo = new ComboBox<>();
        private final MyTasksController controller;
        private MyTaskRow currentRow;

        StatusCell(MyTasksController controller) {
            this.controller = controller;
            combo.getItems().addAll("ASSIGNED", "IN_PROGRESS");
            combo.setOnAction(e -> {
                if (currentRow == null) return;
                String newStatus = combo.getValue();
                if (newStatus != null && !newStatus.equals(currentRow.getStatus())) {
                    controller.setTaskStatus(currentRow, newStatus);
                }
            });
            combo.setStyle("-fx-background-radius: 4; -fx-padding: 2 4;");
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTableRow().getItem() == null) {
                setGraphic(null);
                currentRow = null;
                return;
            }
            MyTaskRow row = getTableRow().getItem();
            currentRow = row;
            String status = row.getStatus();

            if ("COMPLETED".equals(status) || "CANCELLED".equals(status)) {
                Label badge = new Label(status);
                badge.setStyle("-fx-background-color: " + getStatusColor(status) + "; -fx-background-radius: 4; -fx-padding: 3 8;");
                setGraphic(badge);
            } else {
                combo.setValue(status);
                setGraphic(combo);
            }
        }

        private String getStatusColor(String status) {
            return switch (status) {
                case "PENDING" -> "#f0f0f0";
                case "IN_PROGRESS" -> "#fef3c7";
                case "COMPLETED" -> "#d1fae5";
                case "OVERDUE", "FAILED" -> "#fee2e2";
                default -> "#f0f0f0";
            };
        }
    }

    private static class ActionCell extends TableCell<MyTaskRow, Void> {
        private final HBox box = new HBox(6);
        private final Button logTimeBtn = new Button("Учёт времени");
        private final Button completeBtn = new Button("Завершить");
        private final MyTasksController controller;

        ActionCell(MyTasksController controller) {
            this.controller = controller;
            logTimeBtn.getStyleClass().add("btn-primary");
            logTimeBtn.setOnAction(e -> controller.onLogTime(getTableRow().getItem()));
            completeBtn.getStyleClass().add("btn-primary");
            completeBtn.setOnAction(e -> controller.onCompleteTask(getTableRow().getItem()));
            box.getChildren().addAll(logTimeBtn, completeBtn);
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTableRow().getItem() == null) {
                setGraphic(null);
                return;
            }
            String status = getTableRow().getItem().getStatus();
            boolean completed = "COMPLETED".equals(status);
            completeBtn.setDisable(completed);
            setGraphic(box);
        }
    }
}
