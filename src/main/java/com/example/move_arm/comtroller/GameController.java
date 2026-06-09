package com.example.move_arm.comtroller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.example.move_arm.model.ClickData;
import com.example.move_arm.model.User;
import com.example.move_arm.model.settings.HoverGameSettings;
import com.example.move_arm.service.AnimationService;
import com.example.move_arm.service.GameService;
import com.example.move_arm.service.LevelGeneratorService;
import com.example.move_arm.service.SettingsService;
import com.example.move_arm.ui.SceneManager;
import com.example.move_arm.util.AppLogger;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
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
    private final LevelGeneratorService levelGenerator = LevelGeneratorService.getInstance();
    private double[] lastCircle = new double[2];
    private boolean hasLastCircle = false;

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
            var url = getClass().getResource("/com/example/move_arm/sounds/cartoon-bubble-pop-01-.mp3");
            if (url != null) hoverSound = new AudioClip(url.toExternalForm());
        } catch (Exception e) {
            hoverSound = null;
        }
        hoverSound = null;

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
        AppLogger.info("=== GameController.startGame() ВЫЗВАН ===");
        AppLogger.info("Текущие размеры gameRoot: " + gameRoot.getWidth() + " x " + gameRoot.getHeight());

        if (gameActive) {
            AppLogger.warn("Игра уже активна, выходим");
            return;
        }

        resetGameState();

        settings = SettingsService.getInstance().getHoverSettings();
        levelGenerator.initialize(settings.getSeed());

        gameActive = true;
        score = 0;
        activeCircles = 0;
        hasLastCircle = false;
        clickData.clear();

        remainingTime = settings.getDurationSeconds();
        scoreLabel.setText("Очки: " + score);
        timeLabel.setText("Время: " + remainingTime);
        updateUserLabel();

        gameRoot.getChildren().clear();
        gameStartTimeNs = System.nanoTime();

        // === ОСНОВНОЕ ИСПРАВЛЕНИЕ ===
        if (gameRoot.getWidth() > 100 && gameRoot.getHeight() > 100) {
            AppLogger.info("Размеры уже готовы — начинаем спавн");
            spawnInitialTargets();
            startTimer();
        } else {
            AppLogger.info("Размеры ещё нулевые. Ждём 100мс и запускаем...");

            // Задержка + запуск
            Platform.runLater(() -> {
                new java.util.Timer().schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> {
                            AppLogger.info("После задержки размеры: " + gameRoot.getWidth() + " x " + gameRoot.getHeight());
                            spawnInitialTargets();
                            startTimer();
                        });
                    }
                }, 100); // 100 миллисекунд
            });
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

        gameRoot.getChildren().removeIf(node -> node instanceof Circle);
        activeCircles = 0;

        // сохраняем результат — GameService сохраняет в БД и хранит lastGameClicks
        try {
            int savedId = gameService.addGameClicks(
                    settings.getRadius(),
                    settings.getGeneratorType(),
                    settings.getSeed(),
                    settings.getDifficulty(),
                    new ArrayList<>(clickData)
            );
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

        int radius = settings.getRadius();

        // Собираем текущие круги на экране
        List<double[]> activePoints = new ArrayList<>();
        gameRoot.getChildren().stream()
                .filter(node -> node instanceof Circle)
                .map(node -> (Circle) node)
                .forEach(c -> activePoints.add(new double[]{c.getCenterX(), c.getCenterY()}));

        if (hasLastCircle){
            activePoints.add(lastCircle);
        }
        // ПОЛУЧАЕМ КООРДИНАТЫ ИЗ ГЕНЕРАТОРА
        double[] coords = levelGenerator.nextPoint(paneWidth, paneHeight, radius, activePoints);
        double x = coords[0];
        double y = coords[1];
    
        Circle circle = new Circle(radius);
        circle.setCenterX(x);
        circle.setCenterY(y);
        circle.setFill(Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256), 0.85));
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(2);

        circle.setOnMouseEntered(event -> {
            if (!gameActive) return;

            if (hoverSound != null) hoverSound.play();

            lastCircle[0] = circle.getCenterX();
            lastCircle[1] = circle.getCenterY();
            hasLastCircle = true;

            gameRoot.getChildren().remove(circle);
            activeCircles--;

            score++;
            scoreLabel.setText("Очки: " + score);

            int targetRadius = (int) circle.getRadius();

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
    private void handleToMenu() {
        if (timer != null) timer.stop();
        gameActive = false;

        SceneManager mgr = this.sceneManager;
        if (mgr == null) {
            try { mgr = SceneManager.get(); } catch (Exception ignored) { mgr = null; }
        }
        if (mgr != null) mgr.showMenu();
        else AppLogger.error("GameController: Невозможно вернуться в меню — sceneManager == null");
    }

    @FXML
    private void handleRestart() {
        if (timer != null) timer.stop();
        gameActive = false;

        // безопасный restart: если SceneManager доступен, попросим его перезапустить сцену, иначе просто startGame()
        SceneManager mgr = this.sceneManager;
        if (mgr == null) {
            try { mgr = SceneManager.get(); } catch (Exception ignored) {
            }
        }
        if (mgr != null) {
            mgr.startNewGame();
        } else {
            startGame();
        }
    }
    private void resetGameState() {
        gameActive = false;
        score = 0;
        activeCircles = 0;
        hasLastCircle = false;
        clickData.clear();

        if (timer != null) {
            timer.stop();
            timer = null;
        }

        // Важно: очищаем визуальные элементы
        if (gameRoot != null) {
            gameRoot.getChildren().clear();
        }

        gameService.clear();
    }

    private void spawnInitialTargets() {
        while (activeCircles < settings.getMaxCirclesCount() && gameActive) {
            spawnRandomTarget();
        }
    }
}
