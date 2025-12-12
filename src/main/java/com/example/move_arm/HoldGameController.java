package com.example.move_arm;

import java.util.Random;

import com.example.move_arm.model.User;
import com.example.move_arm.model.settings.HoverGameSettings;
import com.example.move_arm.service.GameService;
import com.example.move_arm.service.SettingsService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class HoldGameController {

    @FXML private Pane gameRoot;
    @FXML private HBox topPanel;

    private Label scoreLabel;
    private Label timeLabel;
    private Label userLabel;

    private int score = 0;
    private int activeCircles = 0;
    private int remainingTime;
    private final double HOLD_TIME_SECONDS = 1.0; 

    private HoverGameSettings settings;
    private final Random random = new Random();
    private boolean gameActive = false;
    private Timeline timer;

    private SceneManager sceneManager;
    private final GameService gameService = GameService.getInstance();
    private AudioClip hoverSound;

    // Сохраняем ссылки на слушатели для корректного удаления
    private ChangeListener<Number> widthListener;
    private ChangeListener<Number> heightListener;
    private boolean isInitialSpawnDone = false;

    @FXML
    public void initialize() {
        // Настройка UI
        scoreLabel = new Label("Очки: 0");
        scoreLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        timeLabel = new Label("Время: 0");
        timeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        userLabel = new Label();
        userLabel.setStyle("-fx-text-fill: #ddd; -fx-font-size: 14px;");

        topPanel.setAlignment(Pos.CENTER_LEFT);
        topPanel.setSpacing(18);
        topPanel.getChildren().addAll(scoreLabel, timeLabel, userLabel);

        settings = SettingsService.getInstance().getHoverSettings();

        // Загрузка звука
        try {
            var url = getClass().getResource("/sounds/cartoon-bubble-pop-01-.mp3");
            if (url != null) hoverSound = new AudioClip(url.toExternalForm());
        } catch (Exception ignored) { }

        // Получение SceneManager
        try {
            SceneManager fallback = SceneManager.get();
            if (fallback != null) this.sceneManager = fallback;
        } catch (Exception ignored) { }
        
        updateUserLabel();
    }

    public void setSceneManager(SceneManager manager) {
        this.sceneManager = manager;
        updateUserLabel();
    }

    private void updateUserLabel() {
        try {
            User user = gameService.getCurrentUser();
            userLabel.setText(user != null ? "Пользователь: " + user.getUsername() : "Пользователь: guest");
        } catch (Exception ignored) {}
    }

    public void startGame() {
        if (gameActive) return;

        settings = SettingsService.getInstance().getHoverSettings();
        gameActive = true;
        score = 0;
        activeCircles = 0;
        isInitialSpawnDone = false;

        remainingTime = settings.getDurationSeconds();
        scoreLabel.setText("Очки: " + score);
        timeLabel.setText("Время: " + remainingTime);
        
        gameRoot.getChildren().clear();

        if (gameRoot.getWidth() <= 0 || gameRoot.getHeight() <= 0) {
            widthListener = (obs, oldVal, newVal) -> {
                if (newVal.doubleValue() > 0 && !isInitialSpawnDone) {
                    spawnInitialTargets();
                }
            };
            
            heightListener = (obs, oldVal, newVal) -> {
                if (newVal.doubleValue() > 0 && !isInitialSpawnDone) {
                    spawnInitialTargets();
                }
            };
            
            gameRoot.widthProperty().addListener(widthListener);
            gameRoot.heightProperty().addListener(heightListener);
        } else {
            spawnInitialTargets();
        }

        startTimer();
    }

    private void spawnInitialTargets() {
        if (widthListener != null) {
            gameRoot.widthProperty().removeListener(widthListener);
            widthListener = null;
        }
        if (heightListener != null) {
            gameRoot.heightProperty().removeListener(heightListener);
            heightListener = null;
        }
        
        isInitialSpawnDone = true;
        
        while (activeCircles < settings.getMaxCirclesCount()) {
            spawnHoldTarget();
        }
    }

    private void startTimer() {
        if (timer != null) timer.stop();
        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            remainingTime--;
            timeLabel.setText("Время: " + remainingTime);
            if (remainingTime <= 0) endGame();
        }));
        timer.setCycleCount(settings.getDurationSeconds());
        timer.play();
    }

    private void endGame() {
        gameActive = false;
        if (timer != null) timer.stop();
        gameRoot.getChildren().clear();
        
        if (widthListener != null) {
            gameRoot.widthProperty().removeListener(widthListener);
            widthListener = null;
        }
        if (heightListener != null) {
            gameRoot.heightProperty().removeListener(heightListener);
            heightListener = null;
        }
        
        SceneManager mgr = (sceneManager != null) ? sceneManager : SceneManager.get();
        if (mgr != null) mgr.showResults(); 
    }

    private void spawnHoldTarget() {
        if (!gameActive) return;

        double paneWidth = gameRoot.getWidth();
        double paneHeight = gameRoot.getHeight();
        if (paneWidth <= 0 || paneHeight <= 0) return;

        double radius = settings.getRadius();
        double x = random.nextDouble() * Math.max(0, (paneWidth - 2 * radius));
        double y = random.nextDouble() * Math.max(0, (paneHeight - 2 * radius));
        Color color = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256), 0.85);

        // ✅ КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ: Используем массив для хранения ссылки
        final HoldTarget[] targetRef = new HoldTarget[1];
        
        // ✅ Вычисляем центр заранее
        final double centerX = x + radius;
        final double centerY = y + radius;

        // ✅ Создаем колбэк, который использует массив вместо переменной target
        Runnable onComplete = () -> {
            if (!gameActive) return;

            if (hoverSound != null) hoverSound.play();
            
            score++;
            scoreLabel.setText("Очки: " + score);
            activeCircles--;

            // ✅ Получаем target из массива
            HoldTarget currentTarget = targetRef[0];
            if (currentTarget != null) {
                gameRoot.getChildren().remove(currentTarget);
            }
            
            // ✅ Создаем dummy для анимации с заранее вычисленными координатами
            Circle explosionDummy = new Circle(radius, color);
            explosionDummy.setCenterX(centerX);
            explosionDummy.setCenterY(centerY);
            gameRoot.getChildren().add(explosionDummy);
            
            // ✅ Запускаем анимацию (белые частицы как в оригинале)
            DestroyAnimationService.playContourCollapse(gameRoot, explosionDummy, () -> {
                gameRoot.getChildren().remove(explosionDummy);
            });

            if (activeCircles < settings.getMaxCirclesCount()) {
                spawnHoldTarget();
            }
        };

        // ✅ Создаем target и сохраняем ссылку в массив
        HoldTarget target = new HoldTarget(radius, color, HOLD_TIME_SECONDS, onComplete);
        targetRef[0] = target; // ✅ Сохраняем ссылку здесь

        target.setLayoutX(x);
        target.setLayoutY(y);
        gameRoot.getChildren().add(target);
        activeCircles++;
    }

    @FXML
    private void handleToMenu() {
        if (widthListener != null) {
            gameRoot.widthProperty().removeListener(widthListener);
            widthListener = null;
        }
        if (heightListener != null) {
            gameRoot.heightProperty().removeListener(heightListener);
            heightListener = null;
        }
        
        if (timer != null) timer.stop();
        gameActive = false;
        SceneManager mgr = (sceneManager != null) ? sceneManager : SceneManager.get();
        if (mgr != null) mgr.showStart();
    }
    
    @FXML 
    private void handleRestart() {
        handleToMenu();
        startGame();
    }
    
    // ✅ Метод сохранения результатов (закомментирован как просили)
    /*
    private void saveResults() {
        try {
            User user = gameService.getCurrentUser();
            if (user != null) {
                gameService.saveGameResult(user.getId(), "hold_game", score, settings.getDurationSeconds());
                AppLogger.info("Результаты сохранены для пользователя: " + user.getUsername());
            }
        } catch (Exception e) {
            AppLogger.error("Ошибка сохранения результатов", e);
        }
    }
    */
}