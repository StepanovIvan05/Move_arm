package com.example.move_arm;

import java.util.Random;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class GameController {

    @FXML
    private Pane gameRoot;

    @FXML
    private HBox topPanel; // Теперь используется

    private Label scoreLabel;
    private Label timeLabel;
    private int score = 0;
    private int remainingTime; // Оставшееся время в секундах
    private final int gameDuration = 30; // Настраиваемая длительность игры в секундах (можно изменить позже)
    private int activeCircles = 0;
    private static final int MAX_CIRCLES = 3;
    private Random random = new Random();
    private boolean sceneReady = false;
    private boolean gameActive = false;
    private Timeline timer;

    @FXML
    public void initialize() {
        scoreLabel = new Label("Очки: 0");
        scoreLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px;");
        
        timeLabel = new Label("Время: " + gameDuration);
        timeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px;");
        
        topPanel.getChildren().addAll(scoreLabel, timeLabel);
        topPanel.setSpacing(20); // Добавим расстояние между лейблами

        // Слушаем изменения сцены для gameRoot
        gameRoot.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                // --- ВАЖНО: Слушаем изменения размера ПАНЕЛИ gameRoot, а не сцены ---
                gameRoot.widthProperty().addListener((wObs, oldW, newW) -> checkAndGenerate());
                gameRoot.heightProperty().addListener((hObs, oldH, newH) -> checkAndGenerate());
            }
        });
    }

    private void checkAndGenerate() {
        // Проверяем, готовы ли размеры gameRoot (не сцены!)
        if (sceneReady) return;

        double w = gameRoot.getWidth(); // Получаем ширину ПАНЕЛИ gameRoot
        double h = gameRoot.getHeight(); // Получаем высоту ПАНЕЛИ gameRoot

        // Ждём, пока размеры ПАНЕЛИ станут разумными (> 100)
        if (w > 100 && h > 100) {
            sceneReady = true;
            startGame();
        }
    }

    private void startGame() {
        if (gameActive) return;
        
        gameActive = true;
        score = 0;
        activeCircles = 0;
        remainingTime = gameDuration;
        scoreLabel.setText("Очки: 0");
        timeLabel.setText("Время: " + remainingTime);
        
        // Очищаем панель от предыдущих элементов (если рестарт)
        gameRoot.getChildren().clear();
        
        // Спавним начальные круги
        while (activeCircles < MAX_CIRCLES) {
            spawnRandomTarget();
        }
        
        // Запускаем таймер
        startTimer();
    }

    private void startTimer() {
        if (timer != null) {
            timer.stop();
        }
        
        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            remainingTime--;
            timeLabel.setText("Время: " + remainingTime);
            
            if (remainingTime <= 0) {
                endGame();
            }
        }));
        timer.setCycleCount(gameDuration);
        timer.play();
    }

    private void endGame() {
        gameActive = false;
        if (timer != null) {
            timer.stop();
        }
        
        // Удаляем все круги
        gameRoot.getChildren().removeIf(node -> node instanceof Circle);
        activeCircles = 0;
        
        // Показываем экран результатов
        showResults();
    }

    private void showResults() {
        VBox resultsBox = new VBox(20);
        resultsBox.setAlignment(Pos.CENTER);
        resultsBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");
        resultsBox.prefWidthProperty().bind(gameRoot.widthProperty());
        resultsBox.prefHeightProperty().bind(gameRoot.heightProperty());
        
        Label resultLabel = new Label("Игра окончена! Очки: " + score);
        resultLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px;");
        
        Button restartButton = new Button("Рестарт");
        restartButton.setOnAction(event -> {
            gameRoot.getChildren().remove(resultsBox);
            startGame();
        });
        
        Button exitButton = new Button("Выход");
        exitButton.setOnAction(event -> {
            Stage stage = (Stage) gameRoot.getScene().getWindow();
            stage.close();
            // Или System.exit(0); если нужно полностью закрыть приложение
        });
        
        resultsBox.getChildren().addAll(resultLabel, restartButton, exitButton);
        gameRoot.getChildren().add(resultsBox);
    }

    private void spawnRandomTarget() {
        if (!gameActive) return;
        
        // --- ИСПОЛЬЗУЕМ РАЗМЕРЫ ПАНЕЛИ gameRoot ---
        double paneWidth = gameRoot.getWidth();  // Вместо gameRoot.getScene().getWidth()
        double paneHeight = gameRoot.getHeight(); // Вместо gameRoot.getScene().getHeight()

        // Дополнительная защита
        if (paneWidth <= 0 || paneHeight <= 0) {
             System.out.println("Предупреждение: spawnRandomTarget вызван с нулевыми размерами панели.");
             return; // Выходим, чтобы избежать деления на ноль
        }

        double radius = 20 + random.nextDouble() * 30;
        // Генерируем координаты относительно размеров ПАНЕЛИ gameRoot
        double x = radius + random.nextDouble() * (paneWidth - 2 * radius);
        double y = radius + random.nextDouble() * (paneHeight - 2 * radius);

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
            if (!gameActive) return;
            
            Circle target = (Circle) event.getSource();
            gameRoot.getChildren().remove(target);
            activeCircles--;

            score++;
            scoreLabel.setText("Очки: " + score);

            if (activeCircles < MAX_CIRCLES) {
                spawnRandomTarget();
            }
        });

        gameRoot.getChildren().add(circle);
        activeCircles++;
    }
}