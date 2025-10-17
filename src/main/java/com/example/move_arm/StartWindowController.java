package com.example.move_arm;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

public class StartWindowController {

    @FXML
    private Button startButton;

    @FXML
    private StackPane rootPane;

    @FXML
    private void handleStartButton() {
        AppLogger.log("Кнопка 'Начать' нажата.");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Move Arm");
        alert.setHeaderText(null);
        alert.setContentText("Приложение запущено! Добро пожаловать!");
        alert.showAndWait();
        rootPane.getChildren().clear();
        rootPane.setOnMouseMoved(this::onMouseMove);
    }

    private void onMouseMove(MouseEvent event) {
        double x = event.getSceneX();
        double y = event.getSceneY();
        System.out.printf("Курсор: (%.1f, %.1f)%n", x, y);
    }
}
