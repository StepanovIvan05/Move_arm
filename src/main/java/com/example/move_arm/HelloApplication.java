package com.example.move_arm;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class HelloApplication extends Application {

    @Override
    public void init() {
        AppLogger.info("HelloApplication: init() - приложение инициализируется");
    }

    @Override
    public void start(Stage stage) {
        AppLogger.info("HelloApplication: start() - запуск JavaFX приложения");

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/move_arm/start-window.fxml"));
            Parent root = fxmlLoader.load();

            Scene scene = new Scene(root, 800, 600);
            stage.setTitle("Move Arm - Управление рукой");
            stage.setScene(scene);

            // Логирование событий окна
            stage.setOnShowing(event -> AppLogger.info("HelloApplication: Окно показывается"));
            stage.setOnShown(event -> AppLogger.info("HelloApplication: Окно показано"));
            stage.setOnCloseRequest(event -> AppLogger.info("HelloApplication: Запрос на закрытие окна"));

            stage.show();

            AppLogger.info("HelloApplication: Стартовое окно успешно отображено");

        } catch (IOException e) {
            AppLogger.error("HelloApplication: Критическая ошибка при загрузке FXML", e);
            e.printStackTrace();
        } catch (Exception e) {
            AppLogger.error("HelloApplication: Неожиданная ошибка при запуске", e);
            throw e;
        }
    }

    @Override
    public void stop() {
        AppLogger.info("HelloApplication: stop() - приложение завершает работу");
    }
}