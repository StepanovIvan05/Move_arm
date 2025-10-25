package com.example.move_arm;

import java.util.Random;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class GameController {

    @FXML
    private Pane gameRoot;

    @FXML
    private HBox topPanel;

    private Label scoreLabel;
    private int score = 0;
    private int activeCircles = 0;
    private static final int MAX_CIRCLES = 3;
    private final Random random = new Random();
    private boolean sceneReady = false;

    @FXML
    public void initialize() {
        AppLogger.info("GameController: initialize() начал работу");

        try {
            scoreLabel = new Label("Очки: 0");
            scoreLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px;");
            topPanel.getChildren().add(scoreLabel);

            gameRoot.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    AppLogger.debug("GameController: Сцена установлена для gameRoot");
                    gameRoot.widthProperty().addListener((wObs, oldW, newW) -> {
                        AppLogger.debug("GameController: Изменена ширина gameRoot: " + newW.doubleValue());
                        checkAndGenerate();
                    });
                    gameRoot.heightProperty().addListener((hObs, oldH, newH) -> {
                        AppLogger.debug("GameController: Изменена высота gameRoot: " + newH.doubleValue());
                        checkAndGenerate();
                    });
                }
            });

            AppLogger.info("GameController: initialize() завершил работу успешно");
        } catch (Exception e) {
            AppLogger.error("GameController: Ошибка в initialize()", e);
            throw e;
        }
    }

    private void checkAndGenerate() {
        if (sceneReady) return;

        double w = gameRoot.getWidth();
        double h = gameRoot.getHeight();

        AppLogger.debug(String.format("GameController: checkAndGenerate() - размеры: %.2fx%.2f", w, h));

        if (w > 100 && h > 100) {
            sceneReady = true;
            AppLogger.info("GameController: Размеры панели готовы, начинаем генерацию целей");
            while (activeCircles < MAX_CIRCLES) {
                spawnRandomTarget();
            }
        }
    }

    private void spawnRandomTarget() {
        double paneWidth = gameRoot.getWidth();
        double paneHeight = gameRoot.getHeight();

        if (paneWidth <= 0 || paneHeight <= 0) {
            AppLogger.warn("GameController: spawnRandomTarget вызван с нулевыми размерами панели");
            return;
        }

        double radius = 20 + random.nextDouble() * 30;
        double x = radius + random.nextDouble() * (paneWidth - 2 * radius);
        double y = radius + random.nextDouble() * (paneHeight - 2 * radius);

        AppLogger.debug(String.format("GameController: Создание цели в (%.2f, %.2f) радиус=%.2f", x, y, radius));

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

            AppLogger.info(String.format("GameController: Цель уничтожена. Счёт: %d, активных целей: %d",
                    score, activeCircles));

            if (activeCircles < MAX_CIRCLES) {
                spawnRandomTarget();
            }
        });

        gameRoot.getChildren().add(circle);
        activeCircles++;

        AppLogger.debug(String.format("GameController: Цель создана. Активных целей: %d", activeCircles));
    }
}