package com.example.move_arm;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

/**
 * Окно выбора игры (окно №2).
 * Только выбор игры и системные действия.
 */
public class SelectionController {

    @FXML private Button hoverButton;
    @FXML private Button holdButton;
    @FXML private Button logoutButton;
    @FXML private Button exitButton;

    private SceneManager sceneManager;
    private String selectedGame = "hover";

    public void setSceneManager(SceneManager sm) {
        this.sceneManager = sm;
    }

    @FXML
    public void initialize() {

        hoverButton.setOnAction(e -> {
            selectedGame = "hover";
            sceneManager.showMenu();
        });

        holdButton.setOnAction(e -> {
            selectedGame = "hold";
            sceneManager.showHoldGame(); // ✅ Запускаем новый режим
        });

        logoutButton.setOnAction(e -> {
            sceneManager.showStart();
        });

        exitButton.setOnAction(e -> Platform.exit());
    }
}
