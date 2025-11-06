// src/main/java/com/example/move_arm/StartWindowController.java
package com.example.move_arm;

import javafx.fxml.FXML;

public class StartWindowController {

    private SceneManager sceneManager;

    public void setSceneManager(SceneManager manager) {
        this.sceneManager = manager;
    }

    @FXML
    public void initialize() {
        AppLogger.info("StartWindowController: Контроллер инициализирован");
    }

    @FXML
    private void handleStartButton() {
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