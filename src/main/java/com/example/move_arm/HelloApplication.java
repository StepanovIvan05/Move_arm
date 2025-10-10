package com.example.move_arm;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Загружаем наш FXML файл
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("start-window.fxml"));
        Parent root = fxmlLoader.load();
        
        Scene scene = new Scene(root, 800, 600);
        
        stage.setTitle("Move Arm - Управление рукой");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}