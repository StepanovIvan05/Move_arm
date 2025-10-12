package com.example.move_arm;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) {
        AppLogger.log("Инициализация JavaFX Stage...");
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/move_arm/start-window.fxml"));
            Parent root = fxmlLoader.load();

            Scene scene = new Scene(root, 800, 600);
            stage.setTitle("Move Arm - Управление рукой");
            stage.setScene(scene);
            stage.show();

            AppLogger.log("Окно успешно отображено.");

        } catch (IOException e) {
            AppLogger.log("Ошибка при загрузке FXML: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
