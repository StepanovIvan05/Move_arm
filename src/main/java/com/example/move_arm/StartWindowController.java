package com.example.move_arm;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class StartWindowController {

    @FXML
    private Button startButton;

    @FXML
    private StackPane rootPane;

    @FXML
    private void handleStartButton(ActionEvent event) {
        AppLogger.log("Кнопка 'Начать' нажата.");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Move Arm");
        alert.setHeaderText(null);
        alert.setContentText("Приложение запущено! Добро пожаловать!");
        alert.showAndWait();
        try {
            // Получаем текущую сцену и её Stage
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Загружаем игровую сцену
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/move_arm/game-view.fxml"));
            Scene gameScene = new Scene(loader.load(), 800, 600);

            // Меняем сцену на игровую
            stage.setScene(gameScene);
            stage.setTitle("Move Arm — Игра");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
