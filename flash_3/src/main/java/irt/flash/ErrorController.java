package irt.flash;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ErrorController {

	@FXML private Label errorMessage ;

    @FXML private void close() {
        errorMessage.getScene().getWindow().hide();
    }

    public void setErrorText(String text) {
        errorMessage.setText(text);
    }
}
