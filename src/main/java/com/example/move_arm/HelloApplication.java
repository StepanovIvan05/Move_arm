package com.example.move_arm;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) {
        try {
            AppLogger.log("Загрузка интерфейса...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("start-window.fxml"));
            Parent root = loader.load();

            AppLogger.log("FXML успешно загружен.");

            Scene scene = new Scene(root, 800, 600);
            stage.setTitle("Move Arm - Управление рукой");
            stage.setScene(scene);
            stage.show();

            AppLogger.log("Главное окно показано.");
        } catch (Exception e) {
            AppLogger.logError("Ошибка при запуске приложения", e);
        }
    }

    public static void main(String[] args) {
        AppLogger.log("Инициализация приложения...");
        launch();
    }
}
