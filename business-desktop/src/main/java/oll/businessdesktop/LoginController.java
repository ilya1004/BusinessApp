package oll.businessdesktop;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    private static final String EYE_PATH = "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z";
    private static final String EYE_OFF_PATH = "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z";

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private Button togglePasswordBtn;
    @FXML private Label errorLabel;

    private SVGPath eyeIcon;
    private SVGPath eyeOffIcon;

    @FXML
    public void initialize() {
        eyeIcon = new SVGPath();
        eyeIcon.setContent(EYE_PATH);
        eyeIcon.setFill(Color.web("#666"));
        eyeIcon.setScaleX(0.75);
        eyeIcon.setScaleY(0.75);

        eyeOffIcon = new SVGPath();
        eyeOffIcon.setContent(EYE_OFF_PATH);
        eyeOffIcon.setFill(Color.web("#666"));
        eyeOffIcon.setScaleX(0.75);
        eyeOffIcon.setScaleY(0.75);

        togglePasswordBtn.setGraphic(eyeIcon);
    }

    @FXML
    private void togglePasswordVisibility() {
        if (passwordField.isVisible()) {
            visiblePasswordField.setText(passwordField.getText());
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            visiblePasswordField.setVisible(true);
            visiblePasswordField.setManaged(true);
            visiblePasswordField.requestFocus();
            togglePasswordBtn.setGraphic(eyeOffIcon);
        } else {
            passwordField.setText(visiblePasswordField.getText());
            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordField.requestFocus();
            togglePasswordBtn.setGraphic(eyeIcon);
        }
    }

    @FXML
    private void onLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.isVisible() ? passwordField.getText() : visiblePasswordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Заполните все поля");
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
            stage.setTitle("Business Desktop — Панель управления");
            stage.show();
        } catch (IOException | InterruptedException e) {
            showError("Ошибка подключения к серверу");
            e.printStackTrace();
        } catch (RuntimeException e) {
            showError("Неверное имя пользователя или пароль");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
