package com.example.move_arm;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;

import java.util.logging.Logger;

public class StartWindowController {
    private static final Logger logger = AppLogger.getLogger();

    @FXML
    private Button startButton;

    @FXML
    private void handleStartButton() {
        logger.info("Нажата кнопка 'Начать'");
        System.out.println("Кнопка 'Начать' нажата!");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Move Arm");
        alert.setHeaderText(null);
        alert.setContentText("Приложение запущено! Добро пожаловать в систему управления рукой.");
        alert.showAndWait();
    }
}
