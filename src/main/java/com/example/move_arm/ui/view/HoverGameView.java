package com.example.move_arm.ui.view;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer; // временно, пока переносим код

import com.example.move_arm.service.AnimationService;
import com.example.move_arm.util.AppLogger;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class HoverGameView implements GameView {

    @FXML private Pane gameRoot;
    @FXML private HBox topPanel;

    private Label scoreLabel;
    private Label timeLabel;
    private Label userLabel;

    // События
    private Consumer<TargetHitEvent> onTargetHitHandler;
    private Runnable onToMenuHandler;
    private Runnable onRestartHandler;
    private Runnable onViewReadyHandler;

    private final List<Circle> targets = new ArrayList<>();

    @FXML
    public void initialize() {
        // Создаём лейблы
        scoreLabel = new Label("Очки: 0");
        scoreLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        timeLabel = new Label("Время: 0");
        timeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        userLabel = new Label();
        userLabel.setStyle("-fx-text-fill: #ddd; -fx-font-size: 14px;");

        topPanel.setAlignment(Pos.CENTER_LEFT);
        topPanel.setSpacing(18);
        topPanel.getChildren().addAll(scoreLabel, timeLabel, userLabel);

        // Слушаем готовность сцены и размеров
        setupViewReadyListener();
    }

    private void setupViewReadyListener() {
        gameRoot.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                // Ждём, пока появятся реальные размеры
                gameRoot.widthProperty().addListener((wObs, oldW, newW) -> checkIfViewReady());
                gameRoot.heightProperty().addListener((hObs, oldH, newH) -> checkIfViewReady());
            }
        });
    }

    private void checkIfViewReady() {
        if (gameRoot.getWidth() > 100 && gameRoot.getHeight() > 100 && onViewReadyHandler != null) {
            AppLogger.info("HoverGameView: View готов (размеры получены)");
            onViewReadyHandler.run();
        }
    }

    // ==================== Реализация интерфейса GameView ====================

    @Override
    public void start() {
        // Подготовка перед запуском игры (очистка, сброс UI)
        clearField();
        scoreLabel.setText("Очки: 0");
        timeLabel.setText("Время: 0");
    }

    @Override
    public void clearField() {
        targets.clear();
        gameRoot.getChildren().clear();
    }

    @Override
    public void addTarget(double x, double y, int radius, Color color) {
        Circle circle = new Circle(radius);
        circle.setCenterX(x);
        circle.setCenterY(y);
        circle.setFill(color);
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(2);

        // Обработка попадания
        circle.setOnMouseEntered(event -> {
            try {
            AnimationService.playDestructionAnimation(gameRoot, circle, null);
            } catch (Exception ignored) {}

            targets.remove(circle);
            gameRoot.getChildren().remove(circle);
            if (onTargetHitHandler != null) {
                TargetHitEvent hitEvent = new TargetHitEvent(
                    event.getX(), event.getY(),
                    x, y,
                    radius,
                    System.nanoTime() // относительное время будет считать Presenter
                );
                onTargetHitHandler.accept(hitEvent);
            }

            
            // Анимация разрушения можно вызвать здесь или через Presenter
        });

        targets.add(circle);
        gameRoot.getChildren().add(circle);
    }

    @Override
    public void removeTarget(Object targetId) {
        if (targetId instanceof Circle circle) {
            targets.remove(circle);
            gameRoot.getChildren().remove(circle);
        }
    }

    @Override
    public void setScore(int score) {
        scoreLabel.setText("Очки: " + score);
    }

    @Override
    public void setTime(int seconds) {
        timeLabel.setText("Время: " + seconds);
    }

    @Override
    public void setUserName(String username) {
        userLabel.setText("Пользователь: " + username);
    }

    @Override
    public void setOnTargetHit(Consumer<TargetHitEvent> handler) {
        this.onTargetHitHandler = handler;
    }

    @Override
    public void setOnToMenu(Runnable handler) {
        this.onToMenuHandler = handler;
    }

    @Override
    public void setOnRestart(Runnable handler) {
        this.onRestartHandler = handler;
    }

    @Override
    public void setOnViewReady(Runnable onReady) {
        this.onViewReadyHandler = onReady;
    }

    // FXML методы для кнопок
    @FXML
    private void handleToMenu() {
        if (onToMenuHandler != null) onToMenuHandler.run();
    }

    @FXML
    private void handleRestart() {
        if (onRestartHandler != null) onRestartHandler.run();
    }
    @Override
    public double getWidth() {
        return gameRoot.getWidth();
    }

    @Override
    public double getHeight() {
        return gameRoot.getHeight();
    }
    @Override
    public List<double[]> getActiveTargetPositions() {
        List<double[]> positions = new ArrayList<>();

        for (Circle circle : targets) {

            positions.add(
                new double[]{
                    circle.getCenterX(),
                    circle.getCenterY()
                }
            );

        }

        return positions;
    }
}