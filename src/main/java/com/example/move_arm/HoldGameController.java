package com.example.move_arm;

import java.util.Random;

import com.example.move_arm.model.User;
import com.example.move_arm.model.settings.HoverGameSettings;
import com.example.move_arm.service.GameService;
import com.example.move_arm.service.SettingsService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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

        remainingTime = settings.getDurationSeconds();
        scoreLabel.setText("Очки: " + score);
        timeLabel.setText("Время: " + remainingTime);
        
        gameRoot.getChildren().clear();

        // Защита от спавна до инициализации размеров
        if (gameRoot.getWidth() <= 0 || gameRoot.getHeight() <= 0) {
            gameRoot.widthProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() > 0 && activeCircles == 0) {
                    spawnInitialTargets();
                }
            });
            gameRoot.heightProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() > 0 && activeCircles == 0) {
                    spawnInitialTargets();
                }
            });
        } else {
            spawnInitialTargets();
        }

        startTimer();
    }

    private void spawnInitialTargets() {
        gameRoot.widthProperty().removeListener(null);
        gameRoot.heightProperty().removeListener(null);
        
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

        // Создаем HoldTarget с колбэком завершения
        HoldTarget target = new HoldTarget(radius, color, HOLD_TIME_SECONDS, () -> {
            if (!gameActive) return;

            // Проигрываем звук
            if (hoverSound != null) hoverSound.play();
            
            // Обновляем счёт
            score++;
            scoreLabel.setText("Очки: " + score);
            activeCircles--;

            // Вычисляем центр цели для анимации
            double centerX = target.getLayoutX() + target.getRadius();
            double centerY = target.getLayoutY() + target.getRadius();
            
            // Удаляем HoldTarget
            gameRoot.getChildren().remove(target);
            
            // Создаем временный круг для передачи позиции в анимацию
            Circle explosionDummy = new Circle(target.getRadius(), color);
            explosionDummy.setCenterX(centerX);
            explosionDummy.setCenterY(centerY);
            gameRoot.getChildren().add(explosionDummy);
            
            // ✅ ЗАПУСКАЕМ КОНТУРНОЕ ОСЫПАНИЕ С БЕЛЫМИ ЧАСТИЦАМИ
            DestroyAnimationService.playContourCollapse(gameRoot, explosionDummy, null);

            // Спавним новую цель
            if (activeCircles < settings.getMaxCirclesCount()) {
                spawnHoldTarget();
            }
        });

        target.setLayoutX(x);
        target.setLayoutY(y);
        gameRoot.getChildren().add(target);
        activeCircles++;
    }

    @FXML
    private void handleToMenu() {
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
}