// src/main/java/com/example/move_arm/SceneManager.java
package com.example.move_arm;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class SceneManager {

    private static SceneManager instance;
    private final Stage primaryStage;
    private final Map<String, Scene> sceneCache = new HashMap<>();
    private final Map<String, Object> controllers = new HashMap<>();

    // Ключи для сцен
    public static final String START = "start";
    public static final String GAME = "game";
    public static final String RESULTS = "results";
    public static final String MORERESULTS = "moreresults";
    public static final String SETTINGS = "settings";
    public static final String SELECTION = "selection";
    public static final String MENU = "menu";
    public static final String STATISTICS = "statistics";


    private SceneManager(Stage stage) {
        this.primaryStage = stage;
    }

    public static void init(Stage stage) {
        if (instance == null) {
            instance = new SceneManager(stage);
            AppLogger.info("SceneManager: Инициализирован");
        }
    }

    public static SceneManager get() {
        if (instance == null) {
            throw new IllegalStateException("SceneManager не инициализирован. Вызовите init() в start().");
        }
        return instance;
    }

    // Универсальная загрузка сцены
    private <T> T loadScene(String key, String fxmlPath, Class<T> controllerType) {
        try {
            // Кэшируем, если ещё не загружено
            if (sceneCache.containsKey(key)) {
                primaryStage.setScene(sceneCache.get(key));
                AppLogger.info("SceneManager: Сцена '" + key + "' взята из кэша");
                return controllerType.cast(controllers.get(key));
            }

            AppLogger.info("SceneManager: Загрузка FXML: " + fxmlPath);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            T controller = loader.getController();

            Rectangle2D screen = Screen.getPrimary().getBounds();
            Scene scene = new Scene(root, screen.getWidth(), screen.getHeight());

            sceneCache.put(key, scene);
            controllers.put(key, controller);

            primaryStage.setScene(scene);
            AppLogger.info("SceneManager: Сцена '" + key + "' успешно загружена и установлена");

            return controller;

        } catch (IOException e) {
            AppLogger.error("SceneManager: Критическая ошибка при загрузке FXML: " + fxmlPath, e);
            e.printStackTrace();
            throw new RuntimeException("Не удалось загрузить сцену: " + key, e);
        } catch (Exception e) {
            AppLogger.error("SceneManager: Неожиданная ошибка при переключении сцены: " + key, e);
            throw e;
        }
    }

    // === Публичные методы ===
    public void showStart() {
        StartWindowController ctrl = loadScene(START, "/com/example/move_arm/start-window.fxml", StartWindowController.class);
        ctrl.setSceneManager(this);
    }

    public void showResults(){
        ResultsController ctrl = loadScene(RESULTS, "/com/example/move_arm/results-view.fxml", ResultsController.class);
        ctrl.setSceneManager(this);
    }

    public void showMoreResults(){
        MoreResultsController ctrl = loadScene(MORERESULTS, "/com/example/move_arm/more-results-view.fxml", MoreResultsController.class);
        ctrl.setSceneManager(this);
    }

    public void showSelection() {
        SelectionController ctrl = loadScene(SELECTION, "/com/example/move_arm/selection-view.fxml", SelectionController.class);
        ctrl.setSceneManager(this);
    }

    public void showMenu() {
        MenuController ctrl = loadScene(MENU, "/com/example/move_arm/menu-view.fxml", MenuController.class);
        ctrl.setSceneManager(this);
    }

    public void showStatistics() {
        StatisticsController ctrl = loadScene(STATISTICS, "/com/example/move_arm/hover-statistics-view.fxml", StatisticsController.class);
        ctrl.setSceneManager(this);
    }

    public void clearCache() {
        sceneCache.clear();
        controllers.clear();
        AppLogger.info("SceneManager: Кэш сцен очищен");
    }
    public void showSettings() {
        SettingsController ctrl = loadScene(SETTINGS, "/com/example/move_arm/settings-view.fxml", SettingsController.class);
        ctrl.setSceneManager(this);
    }
    public void startNewGame() {
        GameController controller = loadScene(GAME, "/com/example/move_arm/game-view.fxml", GameController.class);
        
        // Отложить запуск до следующего кадра, когда размеры будут известны
        Platform.runLater(() -> {
            controller.startGame();
            AppLogger.info("SceneManager: Игра запущена после инициализации сцены");
        });
    }


public void showHoldGame() {
    String sceneKey = "holdGame";
    
    try {
        // Если сцена уже в кэше - используем ее
        if (sceneCache.containsKey(sceneKey)) {
            primaryStage.setScene(sceneCache.get(sceneKey));
            AppLogger.info("SceneManager: HoldGame взята из кэша");
            
            // Запускаем игру, если контроллер существует
            HoldGameController controller = (HoldGameController) controllers.get(sceneKey);
            if (controller != null) {
                Platform.runLater(controller::startGame);
            }
            return;
        }

        AppLogger.info("SceneManager: Загрузка HoldGame FXML");
        
        // ✅ ПРОВЕРКА СУЩЕСТВОВАНИЯ РЕСУРСА
        var resource = getClass().getResource("/com/example/move_arm/hold-game-view.fxml");
        if (resource == null) {
            AppLogger.error("SceneManager: FXML файл для HoldGame не найден!");
            throw new IOException("FXML файл для HoldGame не найден по пути: /com/example/move_arm/hold-game-view.fxml");
        }
        
        FXMLLoader loader = new FXMLLoader(resource);
        Parent root = loader.load();
        
        HoldGameController controller = loader.getController();
        if (controller == null) {
            AppLogger.error("SceneManager: Не удалось получить HoldGameController из FXML");
            throw new IllegalStateException("HoldGameController не инициализирован");
        }
        
        controller.setSceneManager(this);

        Rectangle2D screen = Screen.getPrimary().getBounds();
        Scene scene = new Scene(root, screen.getWidth(), screen.getHeight());
        
        sceneCache.put(sceneKey, scene);
        controllers.put(sceneKey, controller);

        primaryStage.setScene(scene);
        AppLogger.info("SceneManager: HoldGame успешно загружена");
        
        // Запускаем игру после полной инициализации UI
        Platform.runLater(() -> {
            try {
                controller.startGame();
                AppLogger.info("SceneManager: HoldGame успешно запущена");
            } catch (Exception e) {
                AppLogger.error("SceneManager: Ошибка при запуске HoldGame", e);
            }
        });
        
    } catch (Exception e) {
        AppLogger.error("SceneManager: Критическая ошибка при загрузке HoldGame", e);
        e.printStackTrace();
        
        // Возвращаемся к выбору игр при ошибке
        Platform.runLater(() -> {
            try {
                showSelection();
            } catch (Exception fallbackError) {
                AppLogger.error("SceneManager: Не удалось вернуться к выбору игр", fallbackError);
                Platform.exit();
            }
        });
    }
}
}