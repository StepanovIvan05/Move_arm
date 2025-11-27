package com.example.move_arm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.example.move_arm.model.ClickData;
import com.example.move_arm.model.settings.HoverGameSettings;
import com.example.move_arm.service.GameService;
import com.example.move_arm.service.SettingsService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
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

    private HoverGameSettings settings;

    private final Random random = new Random();
    private boolean sceneReady = false;
    private boolean gameActive = false;
    private final List<ClickData> clickData = new ArrayList<>();
    private Timeline timer;
    private SceneManager sceneManager;
    private final GameService gameService = GameService.getInstance();
    private AudioClip hoverSound;

    @FXML
    public void initialize() {
        scoreLabel = new Label("Очки: 0");
        scoreLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px;");

        settings = SettingsService.getInstance().getHoverSettings();
        timeLabel = new Label("Время: " + settings.getDurationSeconds());
        timeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px;");
        
        topPanel.getChildren().addAll(scoreLabel, timeLabel);
        topPanel.setSpacing(20);
        hoverSound = new AudioClip(getClass().getResource("/sounds/cartoon-bubble-pop-01-.mp3").toExternalForm());
        
        gameRoot.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                BooleanBinding ready = Bindings.createBooleanBinding(
                    () -> gameRoot.getWidth() > 100 && gameRoot.getHeight() > 100,
                    gameRoot.widthProperty(),
                    gameRoot.heightProperty()
                );

                ready.addListener((o, was, isReady) -> {
                    if (isReady && !sceneReady) {
                        sceneReady = true;
                        checkAndGenerate(); // ← РОВНО 1 РАЗ
                    }
                });
            }
        });
    }

    private void checkAndGenerate() {
        // Проверяем, готовы ли размеры gameRoot (не сцены!)

        double w = gameRoot.getWidth(); // Получаем ширину ПАНЕЛИ gameRoot
        double h = gameRoot.getHeight(); // Получаем высоту ПАНЕЛИ gameRoot

        // Ждём, пока размеры ПАНЕЛИ станут разумными (> 100)
        if (w > 100 && h > 100) {
            sceneReady = true;
            startGame();
        }
    }

    public void setSceneManager(SceneManager manager) {
        this.sceneManager = manager;
    }

    public void startGame() {
        if (gameActive) return;
        
        settings = SettingsService.getInstance().getHoverSettings();

        gameActive = true;
        score = 0;
        activeCircles = 0;
        clickData.clear();
        gameService.clear();
        remainingTime = settings.getDurationSeconds();
        scoreLabel.setText("Очки: 0");
        timeLabel.setText("Время: " + remainingTime);
        
        // Очищаем панель от предыдущих элементов (если рестарт)
        gameRoot.getChildren().clear();
        
        // Спавним начальные круги
        while (activeCircles < settings.getMaxCirclesCount()) {
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
        timer.setCycleCount(settings.getDurationSeconds());
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

        gameService.addGameClicks(clickData);
        gameService.printLastGameSummary();

        
        // Показываем экран результатов
        sceneManager.showResults();
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
        
        Button menuButton = new Button("В меню");
        menuButton.setOnAction(e -> {
            AppLogger.info("GameController: Нажата кнопка 'В меню'");
            try {
                sceneManager.showStart();
                AppLogger.info("GameController: Возврат в главное меню успешен");
            } catch (Exception ex) {
                AppLogger.error("GameController: Ошибка при возврате в меню", ex);
            }
        });
        
        resultsBox.getChildren().addAll(resultLabel, restartButton, menuButton);
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

        double radius = settings.getRadius();
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

            if (hoverSound != null) {
                hoverSound.play();
            }

            Circle target = (Circle) event.getSource();
            gameRoot.getChildren().remove(target);
            activeCircles--;

            score++;
            scoreLabel.setText("Очки: " + score);

            double targetRadius = circle.getRadius() + circle.getStrokeWidth() / 2;

            clickData.add(new ClickData(System.nanoTime(), event.getX(), event.getY(), x, y, targetRadius));

            if (activeCircles < settings.getMaxCirclesCount()) {
                spawnRandomTarget();
            }
            DestroyAnimationService.playExplosion(gameRoot, target, null);
        });

        gameRoot.getChildren().add(circle);
        activeCircles++;
        
    }
    @FXML
    private void handleSettings() {
        // 1. Останавливаем текущую игру/таймер
        if (timer != null) {
            timer.stop();
        }
        gameActive = false;

        // 2. Переходим на экран настроек
        AppLogger.info("GameController: Переход в настройки");
        sceneManager.showSettings();
    }

    @FXML
    private void handleToMenu() {
        if (timer != null) timer.stop();
        gameActive = false;
        sceneManager.showStart();
    }

    @FXML
    private void handleRestart() {
        gameActive = false;
        startGame();
}
}