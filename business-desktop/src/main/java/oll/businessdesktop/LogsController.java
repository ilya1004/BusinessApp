package oll.businessdesktop;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import oll.businessdesktop.model.AppLog;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LogsController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML private ComboBox<String> levelFilter;
    @FXML private Label statusLabel;
    @FXML private Label pageLabel;
    @FXML private Label totalLabel;
    @FXML private TableView<LogRow> logsTable;
    @FXML private TableColumn<LogRow, String> colTimestamp;
    @FXML private TableColumn<LogRow, String> colLevel;
    @FXML private TableColumn<LogRow, String> colSource;
    @FXML private TableColumn<LogRow, String> colAction;
    @FXML private TableColumn<LogRow, String> colMessage;

    private int currentPage = 0;
    private int pageSize = 50;
    private String currentLevel = "ALL";
    private int totalElements = 0;

    @FXML
    public void initialize() {
        colTimestamp.setCellValueFactory(p -> p.getValue().timestamp);
        colLevel.setCellValueFactory(p -> p.getValue().level);
        colSource.setCellValueFactory(p -> p.getValue().source);
        colAction.setCellValueFactory(p -> p.getValue().action);
        colMessage.setCellValueFactory(p -> p.getValue().message);

        colLevel.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle(getLevelStyle(item));
                }
            }

            private String getLevelStyle(String level) {
                return switch (level) {
                    case "ERROR" -> "-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;";
                    case "WARN" -> "-fx-background-color: #fef3c7; -fx-text-fill: #d97706;";
                    case "INFO" -> "-fx-background-color: #dbeafe; -fx-text-fill: #2563eb;";
                    default -> "";
                };
            }
        });

        levelFilter.setValue("ALL");
        levelFilter.setOnAction(e -> onFilterChanged());
        ObservableList<String> levels = FXCollections.observableArrayList("ALL", "INFO", "WARN", "ERROR");
        levelFilter.setItems(levels);
        loadLogs();
    }

    @FXML
    private void onFilterChanged() {
        currentLevel = levelFilter.getValue();
        currentPage = 0;
        loadLogs();
    }

    @FXML
    private void onRefresh() {
        loadLogs();
    }

    @FXML
    private void onPreviousPage() {
        if (currentPage > 0) {
            currentPage--;
            loadLogs();
        }
    }

    @FXML
    private void onNextPage() {
        int maxPage = totalElements > 0 ? (totalElements + pageSize - 1) / pageSize - 1 : 0;
        if (currentPage < maxPage) {
            currentPage++;
            loadLogs();
        }
    }

    private void loadLogs() {
        statusLabel.setText("Loading logs...");
        new Thread(() -> {
            try {
                List<AppLog> logs = ApiService.getLogs(currentPage, pageSize, currentLevel);
                List<LogRow> rows = new ArrayList<>();
                for (AppLog log : logs) {
                    rows.add(new LogRow(
                            log.getTimestamp() != null ? log.getTimestamp().format(DATE_FMT) : "-",
                            log.getLevel() != null ? log.getLevel() : "-",
                            log.getSource() != null ? log.getSource() : "-",
                            log.getAction() != null ? log.getAction() : "-",
                            log.getMessage() != null ? log.getMessage() : "-"
                    ));
                }
                Platform.runLater(() -> {
                    logsTable.setItems(FXCollections.observableArrayList(rows));
                    pageLabel.setText("Page " + (currentPage + 1));
                    totalLabel.setText("Total: " + totalElements);
                    statusLabel.setText(rows.size() + " logs loaded");
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Failed to load logs: " + e.getMessage()));
            }
        }).start();
    }

    public static class LogRow {
        public final SimpleStringProperty timestamp;
        public final SimpleStringProperty level;
        public final SimpleStringProperty source;
        public final SimpleStringProperty action;
        public final SimpleStringProperty message;

        public LogRow(String timestamp, String level, String source, String action, String message) {
            this.timestamp = new SimpleStringProperty(timestamp);
            this.level = new SimpleStringProperty(level);
            this.source = new SimpleStringProperty(source);
            this.action = new SimpleStringProperty(action);
            this.message = new SimpleStringProperty(message);
        }

        public String getTimestamp() { return timestamp.get(); }
        public String getLevel() { return level.get(); }
        public String getSource() { return source.get(); }
        public String getAction() { return action.get(); }
        public String getMessage() { return message.get(); }
    }
}