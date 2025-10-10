package com.example.move_arm;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Logger;

public class HelloApplication extends Application {
    private static final Logger logger = AppLogger.getLogger();

    @Override
    public void start(Stage stage) {
        try {
            logger.info("Загрузка FXML: start-window.fxml");

            FXMLLoader fxmlLoader = new FXMLLoader(
                    HelloApplication.class.getResource("start-window.fxml")
            );
            Parent root = fxmlLoader.load();

            Scene scene = new Scene(root, 800, 600);
            stage.setTitle("Move Arm - Управление рукой");
            stage.setScene(scene);
            stage.show();

            logger.info("Окно успешно отображено.");

        } catch (IOException e) {
            logger.severe("Ошибка при загрузке интерфейса: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception ex) {
            logger.severe("Неожиданная ошибка: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        logger.info("Запуск приложения...");
        launch();
    }
}
