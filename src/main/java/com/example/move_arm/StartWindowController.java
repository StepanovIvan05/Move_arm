// src/main/java/com/example/move_arm/StartWindowController.java
package com.example.move_arm;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

public class StartWindowController {

    @FXML private Button startButton;
    @FXML private StackPane rootPane;

    private SceneManager sceneManager;

    public void setSceneManager(SceneManager manager) {
        this.sceneManager = manager;
    }

    @FXML
    public void initialize() {
        AppLogger.info("StartWindowController: Контроллер инициализирован");
    }

    @FXML
    private void handleStartButton(ActionEvent event) {
        AppLogger.info("StartWindowController: Нажата кнопка 'Начать'");

        try {
            sceneManager.showGame();
            AppLogger.info("StartWindowController: Успешный переход к игровой сцене");

        } catch (Exception e) {
            AppLogger.error("StartWindowController: Неожиданная ошибка при переходе к игре", e);
            throw e;
        }
    }
}