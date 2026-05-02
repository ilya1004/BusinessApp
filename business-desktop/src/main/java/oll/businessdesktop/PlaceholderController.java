package oll.businessdesktop;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class PlaceholderController {

    @FXML private Label titleLabel;

    @FXML
    public void initialize() {
    }

    public void setTabName(String name) {
        if (titleLabel != null) {
            titleLabel.setText(name);
        }
    }
}
