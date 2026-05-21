package oll.businessdesktop;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import oll.businessdesktop.model.KpiUserStats;
import oll.businessdesktop.model.User;

import java.io.IOException;

public class ProfileController {

    @FXML private Circle avatarCircle;
    @FXML private Label avatarLabel;
    @FXML private Label nameLabel;
    @FXML private Label roleLabel;
    @FXML private Label usernameLabel;
    @FXML private Label deptLabel;
    @FXML private Label ratingValLabel;
    @FXML private Label weeklyValLabel;
    @FXML private VBox statsContainer;
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
            stage.setTitle("Business Desktop — Вход");
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadUserProfile() {
        try {
            User user = ApiService.getCurrentUser();
            String initials = (user.firstName() != null && !user.firstName().isEmpty()
                    ? user.firstName().substring(0, 1).toUpperCase() : "")
                    + (user.lastName() != null && !user.lastName().isEmpty()
                    ? user.lastName().substring(0, 1).toUpperCase() : "");
            if (initials.isEmpty()) {
                initials = user.username() != null ? user.username().substring(0, 1).toUpperCase() : "?";
            }
            avatarLabel.setText(initials);
            nameLabel.setText(user.firstName() + " " + user.lastName());
            roleLabel.setText(user.role());
            usernameLabel.setText("@" + user.username());
            deptLabel.setText(user.departmentName());

            if ("EXECUTOR".equals(user.role())) {
                loadUserKpi();
            } else {
                statsContainer.setVisible(false);
                chartContainer.setVisible(false);
            }
        } catch (Exception e) {
            showAlert("Ошибка загрузки профиля: " + e.getMessage());
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
        ratingValLabel.setText(String.valueOf(stats.rating() != null ? (int) Math.round(stats.rating() * 100) : 0));
        weeklyValLabel.setText(String.valueOf(stats.weeklyCompleted() != null ? stats.weeklyCompleted() : 0));

        statsContainer.setVisible(true);

        if (stats.ratingHistory() != null && !stats.ratingHistory().isEmpty()) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            for (KpiUserStats.RatingHistoryPoint point : stats.ratingHistory()) {
                series.getData().add(new XYChart.Data<>(point.date(), point.rating() != null ? point.rating() * 100.0 : 0.0));
            }
            ratingChart.getData().clear();
            ratingChart.getData().add(series);
            chartContainer.setVisible(true);
        } else {
            chartContainer.setVisible(false);
        }
    }

    private void showAlert(String message) {
        javafx.application.Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, message).show());
    }
}
