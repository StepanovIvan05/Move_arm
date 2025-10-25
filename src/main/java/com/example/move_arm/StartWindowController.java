package com.example.move_arm;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class StartWindowController {

    @FXML
    private Button startButton;

    @FXML
    private StackPane rootPane;

    @FXML
    public void initialize() {
        AppLogger.info("StartWindowController: Контроллер инициализирован");
    }

    @FXML
    private void handleStartButton(ActionEvent event) {
        AppLogger.info("StartWindowController: Нажата кнопка 'Начать'");

        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/move_arm/game-view.fxml"));
            Scene gameScene = new Scene(loader.load(), 800, 600);

            stage.setScene(gameScene);
            stage.setTitle("Move Arm — Игра");

            AppLogger.info("StartWindowController: Успешный переход к игровой сцене");

        } catch (IOException e) {
            AppLogger.error("StartWindowController: Ошибка при загрузке игровой сцены", e);
            e.printStackTrace();
        } catch (Exception e) {
            AppLogger.error("StartWindowController: Неожиданная ошибка при переходе к игре", e);
            throw e;
        }
    }
}