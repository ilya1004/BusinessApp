package oll.businessdesktop;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import oll.businessdesktop.model.User;

import java.io.IOException;

public class ProfileController {

    @FXML private Label usernameLabel;
    @FXML private Label nameLabel;
    @FXML private Label roleLabel;

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
        } catch (Exception e) {
            showAlert("Profile load error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(String message) {
        javafx.application.Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, message).show());
    }
}
