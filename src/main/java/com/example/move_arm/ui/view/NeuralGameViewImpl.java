package com.example.move_arm.ui.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.example.move_arm.service.AnimationService;
import com.example.move_arm.util.AppLogger;
import com.example.move_arm.util.GridUtils;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class NeuralGameViewImpl implements NeuralGameView {
    
    @FXML private Pane gameRoot;
    @FXML private HBox topPanel;
    
    private Label scoreLabel;
    private Label timeLabel;
    private Label userLabel;
    
    // Обработчики
    private Consumer<NeuralHitEvent> onNeuralHitHandler;
    private Runnable onToMenuHandler;
    private Runnable onRestartHandler;
    private Runnable onViewReadyHandler;
    
    // Состояние целей
    private final Map<Integer, TargetCellData> activeTargets = new HashMap<>();
    private int targetCounter = 0;
    private long gameStartTimeNs = 0;
    
    // DTO для внутреннего хранения
    private static class TargetCellData {
        Circle circle;
        int cellIndex;
        long spawnTimeNs;
        int targetIndex;
        double x, y;
        int radius;
    }
    
    // ==================== FXML инициализация ====================
    
    @FXML
    public void initialize() {
        scoreLabel = new Label("Очки: 0");
        scoreLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        timeLabel = new Label("Время: 0");
        timeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        userLabel = new Label();
        userLabel.setStyle("-fx-text-fill: #ddd; -fx-font-size: 14px;");
        
        topPanel.setAlignment(Pos.CENTER_LEFT);
        topPanel.setSpacing(18);
        topPanel.getChildren().addAll(scoreLabel, timeLabel, userLabel);
        
        setupViewReadyListener();
    }
    
    private void setupViewReadyListener() {
        gameRoot.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                gameRoot.widthProperty().addListener((wObs, oldW, newW) -> checkIfViewReady());
                gameRoot.heightProperty().addListener((hObs, oldH, newH) -> checkIfViewReady());
            }
        });
    }
    
    private void checkIfViewReady() {
        if (gameRoot.getWidth() > 100 && gameRoot.getHeight() > 100 && onViewReadyHandler != null) {
            AppLogger.info("NeuralGameViewImpl: View готов");
            onViewReadyHandler.run();
        }
    }
    
    // ==================== NeuralGameView ====================
    
    @Override
    public void addTargetWithCell(double x, double y, int radius, Color color, int cellIndex) {
        Circle circle = new Circle(radius);
        circle.setCenterX(x);
        circle.setCenterY(y);
        circle.setFill(color);
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(2);
        
        TargetCellData data = new TargetCellData();
        data.circle = circle;
        data.cellIndex = cellIndex;
        data.spawnTimeNs = System.nanoTime();
        data.targetIndex = targetCounter++;
        data.x = x;
        data.y = y;
        data.radius = radius;
        
        activeTargets.put(cellIndex, data);
        
        circle.setOnMouseEntered(event -> {
            long hitTimeNs = System.nanoTime();
            long lifetimeNs = hitTimeNs - data.spawnTimeNs;
            
            try {
                AnimationService.playDestructionAnimation(gameRoot, circle, null);
            } catch (Exception ignored) {}
            
            gameRoot.getChildren().remove(circle);
            activeTargets.remove(cellIndex);
            
            if (onNeuralHitHandler != null) {
                NeuralHitEvent hitEvent = new NeuralHitEvent(
                    event.getX(), event.getY(),
                    x, y, radius,
                    hitTimeNs - gameStartTimeNs,
                    data.spawnTimeNs,
                    lifetimeNs,
                    data.targetIndex,
                    cellIndex
                );
                onNeuralHitHandler.accept(hitEvent);
            }
        });
        
        gameRoot.getChildren().add(circle);
    }
    
    @Override
    public List<TargetCell> getActiveTargetsWithCells() {
        List<TargetCell> result = new ArrayList<>();
        for (TargetCellData data : activeTargets.values()) {
            result.add(new TargetCell(data.x, data.y, data.cellIndex, data.spawnTimeNs));
        }
        return result;
    }
    
    @Override
    public void setOnNeuralTargetHit(Consumer<NeuralHitEvent> handler) {
        this.onNeuralHitHandler = handler;
    }
    
    @Override
    public void recordTargetTimeout(int cellIndex, long timeoutNs) {
        TargetCellData data = activeTargets.remove(cellIndex);
        if (data != null) {
            gameRoot.getChildren().remove(data.circle);
            // Можно логировать: цель исчезла без попадания
        }
    }
    
    // ==================== GameView (базовые методы) ====================
    
    @Override
    public void start() {
        clearField();
        scoreLabel.setText("Очки: 0");
        timeLabel.setText("Время: 0");
        gameStartTimeNs = System.nanoTime();
    }
    
    @Override
    public void clearField() {
        gameRoot.getChildren().clear();
        activeTargets.clear();
        targetCounter = 0;
    }
    
    @Override
    public void addTarget(double x, double y, int radius, Color color) {
        // Делегируем в addTargetWithCell с вычислением cellIndex
        int cell = GridUtils.xyToCell(x, y, getWidth(), getHeight());
        addTargetWithCell(x, y, radius, color, cell);
    }
    
    @Override
    public void removeTarget(Object targetId) {
        if (targetId instanceof Circle circle) {
            gameRoot.getChildren().remove(circle);
            // Удаляем из activeTargets по поиску
            activeTargets.values().removeIf(data -> data.circle == circle);
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
        // Не используется в RL-режиме, но интерфейс требует
        // Можно игнорировать или обернуть NeuralHitEvent
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
        for (TargetCellData data : activeTargets.values()) {
            positions.add(new double[]{data.x, data.y});
        }
        return positions;
    }
    
    // ==================== FXML кнопки ====================
    
    @FXML
    private void handleToMenu() {
        if (onToMenuHandler != null) onToMenuHandler.run();
    }
    
    @FXML
    private void handleRestart() {
        if (onRestartHandler != null) onRestartHandler.run();
    }
}