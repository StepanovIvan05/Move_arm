package com.example.move_arm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.example.move_arm.model.ClickData;
import com.example.move_arm.model.User;
import com.example.move_arm.model.settings.HoverGameSettings;
import com.example.move_arm.service.AnimationService;
import com.example.move_arm.service.GameService;
import com.example.move_arm.service.SettingsService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Исправленная версия GameController: безопасный fallback для sceneManager == null.
 */
public class GameController {

    @FXML private Pane gameRoot;
    @FXML private HBox topPanel;

    private Label scoreLabel;
    private Label timeLabel;
    private Label userLabel;

    private int score = 0;
    private int activeCircles = 0;
    private int remainingTime;

    private HoverGameSettings settings;
    private final Random random = new Random();

    private boolean sceneReady = false;
    private boolean gameActive = false;

    private final List<ClickData> clickData = new ArrayList<>();
    private Timeline timer;

    private long gameStartTimeNs = 0L;
    private SceneManager sceneManager; // может быть null — используем fallback
    private final GameService gameService = GameService.getInstance();
    private AudioClip hoverSound;

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

        settings = SettingsService.getInstance().getHoverSettings();

        try {
            var url = getClass().getResource("/sounds/cartoon-bubble-pop-01-.mp3");
            if (url != null) hoverSound = new AudioClip(url.toExternalForm());
        } catch (Exception e) {
            hoverSound = null;
        }

        // Попробуем сразу получить SceneManager из singleton, если он уже инициализирован
        try {
            SceneManager fallback = SceneManager.get(); // может бросить IllegalStateException
            if (fallback != null) {
                this.sceneManager = fallback;
            }
        } catch (Exception ignored) { /* SceneManager ещё не инициализирован — нормально */ }

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
                        // не стартуем автоматически — startGame вызывается извне (SceneManager.showGame())
                    }
                });
            }
        });

        updateUserLabel();
    }

    public void setSceneManager(SceneManager manager) {
        this.sceneManager = manager;
        updateUserLabel();
    }

    private void updateUserLabel() {
        try {
            User user = gameService.getCurrentUser();
            if (user != null) userLabel.setText("Пользователь: " + user.getUsername());
            else userLabel.setText("Пользователь: guest");
        } catch (Exception ignored) {
            userLabel.setText("Пользователь: ?");
        }
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
        scoreLabel.setText("Очки: " + score);
        timeLabel.setText("Время: " + remainingTime);
        updateUserLabel();

        gameRoot.getChildren().clear();
        gameStartTimeNs = System.nanoTime();

        while (activeCircles < settings.getMaxCirclesCount()) {
            spawnRandomTarget();
        }

        startTimer();
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

        gameRoot.getChildren().removeIf(node -> node instanceof Circle);
        activeCircles = 0;

        // сохраняем результат — GameService сохраняет в БД и хранит lastGameClicks
        try {
            int savedId = gameService.addGameClicks(new ArrayList<>(clickData));
            AppLogger.info("GameController: Результат сохранён в БД (id=" + savedId + ")");
        } catch (Exception e) {
            AppLogger.error("GameController: Ошибка сохранения результата", e);
        }

        // печать сводки (совместимость)
        try { gameService.printLastGameSummary(); } catch (Exception ignored) {}

        // безопасный вызов showResults(): если sceneManager == null — пробуем SceneManager.get()
        SceneManager mgr = this.sceneManager;
        if (mgr == null) {
            try { mgr = SceneManager.get(); } catch (Exception ignored) { mgr = null; }
        }

        if (mgr != null) {
            mgr.showResults();
        } else {
            AppLogger.error("GameController: Невозможно показать Results — sceneManager == null");
        }
    }

    private void spawnRandomTarget() {
        if (!gameActive) return;

        double paneWidth = gameRoot.getWidth();
        double paneHeight = gameRoot.getHeight();
        if (paneWidth <= 0 || paneHeight <= 0) {
            AppLogger.warn("GameController: spawnRandomTarget с нулевыми размерами.");
            return;
        }

        double radius = settings.getRadius();
        double x = radius + random.nextDouble() * Math.max(0, (paneWidth - 2 * radius));
        double y = radius + random.nextDouble() * Math.max(0, (paneHeight - 2 * radius));

        Circle circle = new Circle(radius);
        circle.setCenterX(x);
        circle.setCenterY(y);
        circle.setFill(Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256), 0.85));
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(2);

        circle.setOnMouseEntered(event -> {
            if (!gameActive) return;

            if (hoverSound != null) hoverSound.play();

            gameRoot.getChildren().remove(circle);
            activeCircles--;

            score++;
            scoreLabel.setText("Очки: " + score);

            double targetRadius = circle.getRadius() + circle.getStrokeWidth() / 2.0;

            long relNs = System.nanoTime() - gameStartTimeNs;
            double cursorX = event.getX();
            double cursorY = event.getY();
            clickData.add(new ClickData(relNs, cursorX, cursorY, x, y, targetRadius));

            if (activeCircles < settings.getMaxCirclesCount()) spawnRandomTarget();

            try { AnimationService.playDestructionAnimation(gameRoot, circle, null); } catch (Exception ignored) {}
        });

        gameRoot.getChildren().add(circle);
        activeCircles++;
    }

    @FXML
    private void handleSettings() {
        if (timer != null) timer.stop();
        gameActive = false;

        SceneManager mgr = this.sceneManager;
        if (mgr == null) {
            try { mgr = SceneManager.get(); } catch (Exception ignored) { mgr = null; }
        }
        if (mgr != null) mgr.showSettings();
        else AppLogger.error("GameController: Невозможно открыть настройки — sceneManager == null");
    }

    @FXML
    private void handleToMenu() {
        if (timer != null) timer.stop();
        gameActive = false;

        SceneManager mgr = this.sceneManager;
        if (mgr == null) {
            try { mgr = SceneManager.get(); } catch (Exception ignored) { mgr = null; }
        }
        if (mgr != null) mgr.showStart();
        else AppLogger.error("GameController: Невозможно вернуться в меню — sceneManager == null");
    }

    @FXML
    private void handleRestart() {
        if (timer != null) timer.stop();
        gameActive = false;

        // безопасный restart: если SceneManager доступен, попросим его перезапустить сцену, иначе просто startGame()
        SceneManager mgr = this.sceneManager;
        if (mgr == null) {
            try { mgr = SceneManager.get(); } catch (Exception ignored) { mgr = null; }
        }
        if (mgr != null) {
            mgr.startNewGame();
        } else {
            startGame();
        }
    }
}
