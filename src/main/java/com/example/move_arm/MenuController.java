package com.example.move_arm;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

/**
 * Контроллер окна №3 — меню выбранной игры.
 */
public class MenuController {

    @FXML private Button settingsButton;
    @FXML private Button statsButton;
    @FXML private Button startButton;
    @FXML private Button selectGameButton;
    @FXML private Button exitButton;

    private SceneManager sceneManager;

    public void setSceneManager(SceneManager sm) {
        this.sceneManager = sm;
    }

    @FXML
    public void initialize() {

        // Настройки игры
        settingsButton.setOnAction(e -> {
            sceneManager.showSettings();
        });

        // Статистика
        statsButton.setOnAction(e -> {
            sceneManager.showStatistics();
        });

        // Начало игры
        startButton.setOnAction(e -> {
            sceneManager.startNewGame();
        });

        // Вернуться к выбору игры
        selectGameButton.setOnAction(e -> {
            sceneManager.showSelection();
        });

        // Выйти из приложения
        exitButton.setOnAction(e -> Platform.exit());
    }
}
