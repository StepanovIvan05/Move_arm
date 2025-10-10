package com.example.move_arm;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class HelloApplication extends Application {

    private static void log(String message) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("MoveArm.log", true))) {
            writer.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage stage) {
        try {
            log("=== Приложение запускается ===");
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("start_window.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 600, 400);
            stage.setTitle("Move Arm — Управление рукой");
            stage.setScene(scene);
            stage.show();
            log("Окно успешно показано");
        } catch (Exception e) {
            log("Ошибка запуска: " + e.getMessage());
            for (StackTraceElement el : e.getStackTrace()) {
                log("    at " + el);
            }
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        log("main() запущен");
        launch();
    }
}
