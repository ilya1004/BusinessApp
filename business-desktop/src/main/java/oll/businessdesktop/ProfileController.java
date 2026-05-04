package oll.businessdesktop;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import oll.businessdesktop.model.KpiUserStats;
import oll.businessdesktop.model.User;

import java.io.IOException;

public class ProfileController {

    @FXML private Label usernameLabel;
    @FXML private Label nameLabel;
    @FXML private Label roleLabel;
    @FXML private VBox statsContainer;
    @FXML private HBox statsRows;
    @FXML private VBox chartContainer;
    @FXML private LineChart<String, Number> ratingChart;

    @FXML
    public void initialize() {
        loadUserProfile();
    }

    @FXML
    private void onRefresh() {
        loadUserProfile();
    }

    @FXML
    private void onLogout() {
        ApiService.setAuthToken(null);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/oll/businessdesktop/login-view.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) usernameLabel.getScene().getWindow();
            Scene scene = new Scene(root, 400, 500);
            stage.setScene(scene);
            stage.setTitle("Business Desktop - Login");
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadUserProfile() {
        try {
            User user = ApiService.getCurrentUser();
            usernameLabel.setText("Username: " + user.username());
            nameLabel.setText("Name: " + user.firstName() + " " + user.lastName());
            roleLabel.setText("Role: " + user.role());

            loadUserKpi();
        } catch (Exception e) {
            showAlert("Profile load error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadUserKpi() {
        new Thread(() -> {
            try {
                KpiUserStats stats = ApiService.getCurrentUserStats();
                javafx.application.Platform.runLater(() -> updateUserKpi(stats));
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    statsContainer.setVisible(false);
                    chartContainer.setVisible(false);
                });
            }
        }).start();
    }

    private void updateUserKpi(KpiUserStats stats) {
        statsRows.getChildren().clear();

        addStatCard("Rating", String.format("%.3f", stats.rating() != null ? stats.rating() : 0.0));
        addStatCard("Weekly Completed", String.valueOf(stats.weeklyCompleted() != null ? stats.weeklyCompleted() : 0));
        addStatCard("Load", String.format("%.1f%%", stats.loadPercent() != null ? stats.loadPercent() : 0.0));

        statsContainer.setVisible(true);

        if (stats.ratingHistory() != null && !stats.ratingHistory().isEmpty()) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            for (KpiUserStats.RatingHistoryPoint point : stats.ratingHistory()) {
                series.getData().add(new XYChart.Data<>(point.date(), point.rating() != null ? point.rating() : 0.0));
            }
            ratingChart.getData().clear();
            ratingChart.getData().add(series);
            chartContainer.setVisible(true);
        } else {
            chartContainer.setVisible(false);
        }
    }

    private void addStatCard(String label, String value) {
        VBox card = new VBox();
        card.setSpacing(4);
        card.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 8; -fx-padding: 12; -fx-min-width: 140;");

        Label keyLabel = new Label(label);
        keyLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-fg-muted;");

        Label valLabel = new Label(value);
        valLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        card.getChildren().addAll(keyLabel, valLabel);
        statsRows.getChildren().add(card);
    }

    private void showAlert(String message) {
        javafx.application.Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, message).show());
    }
}
