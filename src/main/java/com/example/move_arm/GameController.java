package com.example.move_arm;

import java.util.Random;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class GameController {

    @FXML
    private Pane gameRoot;

    private Label scoreLabel;
    private int score = 0;
    private int activeCircles = 0;
    private static final int MAX_CIRCLES = 3; // ← лимит кругов
    private Random random = new Random();
    private boolean sceneReady = false;

    @FXML
    public void initialize() {
        scoreLabel = new Label("Очки: 0");
        scoreLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-padding: 10;");
        scoreLabel.setTranslateX(10);
        scoreLabel.setTranslateY(10);
        gameRoot.getChildren().add(scoreLabel);

        // Слушаем изменения сцены
        gameRoot.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                // Слушаем изменения размеров сцены — это сработает, когда окно отобразится
                newScene.widthProperty().addListener((wObs, oldW, newW) -> checkAndGenerate());
                newScene.heightProperty().addListener((hObs, oldH, newH) -> checkAndGenerate());
            }
        });
    }

    private void checkAndGenerate() {
        if (sceneReady) return;

        double w = gameRoot.getScene().getWidth();
        double h = gameRoot.getScene().getHeight();

        // Ждём, пока размеры станут разумными (> 100)
        if (w > 100 && h > 100) {
            sceneReady = true;
            while (activeCircles < MAX_CIRCLES) {
                spawnRandomTarget();
            }
        }
    }

    private void spawnRandomTarget() {
        double sceneWidth = 700.0;
        double sceneHeight = 500.0;

        // Дополнительная защита
        if (sceneWidth <= 0 || sceneHeight <= 0) return;

        double radius = 20 + random.nextDouble() * 30;
        double x = random.nextDouble(sceneWidth);
        double y = random.nextDouble(sceneHeight);

        Circle circle = new Circle(radius);
        circle.setCenterX(x);
        circle.setCenterY(y);
        circle.setFill(Color.rgb(
            random.nextInt(256),
            random.nextInt(256),
            random.nextInt(256),
            0.8
        ));
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(2);

        circle.setOnMouseEntered(event -> {
            Circle target = (Circle) event.getSource();
            gameRoot.getChildren().remove(target);
            activeCircles--;

            score++;
            scoreLabel.setText("Очки: " + score);

            // Добавляем новый, если меньше лимита
            if (activeCircles < MAX_CIRCLES) {
                spawnRandomTarget();
            }
        });

        gameRoot.getChildren().add(circle);
        activeCircles++;
    }
}