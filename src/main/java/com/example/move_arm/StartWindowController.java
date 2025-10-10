package com.example.move_arm;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;

public class StartWindowController {

    @FXML
    private Button startButton;

    @FXML
    private void handleStartButton() {
        AppLogger.log("Нажата кнопка 'Начать'");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Move Arm");
        alert.setHeaderText(null);
        alert.setContentText("Приложение запущено! Добро пожаловать в систему управления рукой.");
        alert.showAndWait();

        AppLogger.log("Показано приветственное сообщение пользователю.");
    }
}
