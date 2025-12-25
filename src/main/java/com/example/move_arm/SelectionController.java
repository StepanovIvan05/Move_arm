package com.example.move_arm;

import com.example.move_arm.database.GameTypeDao;
import com.example.move_arm.service.GameService;
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
    private final GameTypeDao gameTypeDao = new GameTypeDao();
    private final GameService gameService = GameService.getInstance();

    public void setSceneManager(SceneManager sm) {
        this.sceneManager = sm;
    }

    @FXML
    public void initialize() {

        hoverButton.setOnAction(e -> {
            gameService.setCurrentGameType(gameTypeDao.findByName("hover").get());
            sceneManager.showMenu();
        });

        holdButton.setOnAction(e -> {
            gameService.setCurrentGameType(gameTypeDao.findByName("hold").get());
            sceneManager.showMenu();
        });

        logoutButton.setOnAction(e -> {
            sceneManager.showStart();
        });

        exitButton.setOnAction(e -> Platform.exit());
    }
}
