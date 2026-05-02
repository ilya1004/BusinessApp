package oll.businessdesktop;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private TextField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void onLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Fill in all fields");
            return;
        }

        try {
            ApiService.login(username, password);
            // Successful login - open main window
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/oll/businessdesktop/main-layout.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 800);
            stage.setScene(scene);
            stage.setTitle("Business Desktop");
            stage.show();
        } catch (IOException | InterruptedException e) {
            showError("Server connection error");
            e.printStackTrace();
        } catch (RuntimeException e) {
            showError("Invalid username or password");
        }
    }

    @FXML
    private void onDevLogin() {
        ApiService.setAuthToken("dev-token");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/oll/businessdesktop/main-layout.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 800);
            stage.setScene(scene);
            stage.setTitle("Business Desktop (DEV)");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
