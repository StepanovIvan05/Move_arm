// src/main/java/com/example/move_arm/HelloApplication.java
package com.example.move_arm;

import javafx.application.Application;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    @Override
    public void init() {
        AppLogger.info("HelloApplication: init() - приложение инициализируется");
    }

    @Override
    public void start(Stage stage) {
        AppLogger.info("HelloApplication: start() - запуск JavaFX приложения");

        try {
            // Инициализируем SceneManager
            SceneManager.init(stage);

            // Показываем стартовую сцену
            SceneManager.get().showStart();

            stage.setTitle("Move Arm - Управление рукой");
            stage.setResizable(false);

            // Логирование событий окна
            stage.setOnShowing(event -> AppLogger.info("HelloApplication: Окно показывается"));
            stage.setOnShown(event -> AppLogger.info("HelloApplication: Окно показано"));
            stage.setOnCloseRequest(event -> {
                AppLogger.info("HelloApplication: Запрос на закрытие окна");
                // Можно сохранить статистику
            });

            stage.show();
            AppLogger.info("HelloApplication: Приложение запущено и стартовое окно отображено");

        } catch (Exception e) {
            AppLogger.error("HelloApplication: Критическая ошибка при запуске", e);
            throw e;
        }
    }

    @Override
    public void stop() {
        AppLogger.info("HelloApplication: stop() - приложение завершает работу");
    }

    public static void main(String[] args) {
        launch(args);
    }
}