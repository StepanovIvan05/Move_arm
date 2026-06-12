package com.example.move_arm.comtroller;

import java.util.Objects;

import com.example.move_arm.service.GameService;
import com.example.move_arm.ui.SceneManager;

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
    private final GameService gameService = GameService.getInstance();
    private String gameType;

    public void setSceneManager(SceneManager sm) {
        this.sceneManager = sm;
    }

    @FXML
    public void initialize() {

        // Настройки игры
        settingsButton.setOnAction(e -> {
            gameType = gameService.getCurrentGameTypeString();
            if(Objects.equals(gameType, "hover")){
                sceneManager.showHoverSettings();
            }
            else if(Objects.equals(gameType, "hold")){
                sceneManager.showHoldSettings();
            }
            else if(Objects.equals(gameType, "neural")){
                sceneManager.showNeuralSettings();
            }
        });

        // Статистика
        statsButton.setOnAction(e -> {
            sceneManager.showStatistics();
        });

        // Начало игры
        startButton.setOnAction(e -> {
            gameType = gameService.getCurrentGameTypeString();
            if(Objects.equals(gameType, "hover")){
                sceneManager.startNewGame();
            }
            else if(Objects.equals(gameType, "hold")){
                sceneManager.showHoldGame();
            }
            else if(Objects.equals(gameType, "neural")){
                sceneManager.startNeuralGame();
            }
        });

        // Вернуться к выбору игры
        selectGameButton.setOnAction(e -> {
            sceneManager.showSelection();
        });

        // Выйти из приложения
        exitButton.setOnAction(e -> Platform.exit());
    }
}
