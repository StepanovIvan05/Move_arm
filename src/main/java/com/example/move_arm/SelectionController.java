package com.example.move_arm;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Промежуточный экран выбора игры.
 * После выбора — переходим в showGame() или showSettings() через SceneManager.
 */
public class SelectionController {

    @FXML private Button hoverButton;
    @FXML private Button otherButton;
    @FXML private Button toSettingsButton;
    @FXML private Button backButton;
    @FXML private Label infoLabel;

    private SceneManager sceneManager;
    private String selectedGame = "hover"; // default

    public void setSceneManager(SceneManager sm) {
        this.sceneManager = sm;
        updateInfo();
    }

    @FXML
    public void initialize() {
        hoverButton.setOnAction(e -> {
            selectedGame = "hover";
            updateInfo();
        });

        otherButton.setOnAction(e -> {
            selectedGame = "other";
            updateInfo();
        });

        toSettingsButton.setOnAction(e -> {
            // при выборе настроек — показываем настройки; контроллер настроек берет текущую gameType (если нужно)
            try {
                // при текущей архитектуре settings общие, поэтому просто открываем настройки
                sceneManager.showSettings();
            } catch (Exception ex) {
                AppLogger.error("SelectionController: Не удалось открыть настройки", ex);
            }
        });

        backButton.setOnAction(e -> {
            sceneManager.showStart();
        });
    }

    private void updateInfo() {
        infoLabel.setText("Выбрана игра: " + selectedGame);
    }
}
