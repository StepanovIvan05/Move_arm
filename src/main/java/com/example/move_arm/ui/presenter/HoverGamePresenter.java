package com.example.move_arm.ui.presenter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.example.move_arm.model.ClickData;
import com.example.move_arm.model.settings.HoverGameSettings;
import com.example.move_arm.service.GameService;
import com.example.move_arm.service.LevelGeneratorService;
import com.example.move_arm.service.SettingsService;
import com.example.move_arm.service.VectorTrajectoryGenerator;
import com.example.move_arm.ui.SceneManager;
import com.example.move_arm.ui.view.GameView;
import com.example.move_arm.ui.view.TargetHitEvent;
import com.example.move_arm.util.AppLogger;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class HoverGamePresenter {

    private final GameView view;
    private final GameService gameService;
    private final SettingsService settingsService;
    private final LevelGeneratorService levelGenerator;
    private final SceneManager sceneManager;
    private final VectorTrajectoryGenerator trajectoryGenerator;

    private HoverGameSettings settings;
    private final List<ClickData> clickData = new ArrayList<>();
    private Timeline timer;
    private long gameStartTimeNs = 0;
    private int score = 0;
    private int activeCircles = 0;
    private int remainingTime;
    private boolean gameActive = false;

    // Важная логика избегания спавна рядом с последней сбитой целью
    private final double[] lastCircle = new double[2];
    private boolean hasLastCircle = false;

    private final Random random = new Random();

    public HoverGamePresenter(GameView view, SceneManager sceneManager) {
        this.view = view;
        this.sceneManager = sceneManager;
        this.gameService = GameService.getInstance();
        this.settingsService = SettingsService.getInstance();
        this.levelGenerator = LevelGeneratorService.getInstance();
        this.trajectoryGenerator = VectorTrajectoryGenerator.getInstance();

        view.setOnTargetHit(this::onTargetHit);
        view.setOnToMenu(this::goToMenu);
        view.setOnRestart(this::restartGame);
        view.setOnViewReady(this::onViewReady);
    }

    public void startNewGame() {
        AppLogger.info("HoverGamePresenter: startNewGame()");

        settings = settingsService.getHoverSettings();
        levelGenerator.initialize(settings.getSeed());

        resetGameState();

        view.start();
        view.setScore(0);
        view.setTime(settings.getDurationSeconds());
        view.setUserName(gameService.getCurrentUser().getUsername());

        gameService.clear();
        gameStartTimeNs = System.nanoTime();
    }

    private void resetGameState() {
        gameActive = true;
        score = 0;
        activeCircles = 0;
        hasLastCircle = false;
        remainingTime = settings.getDurationSeconds();
        clickData.clear();
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        view.clearField();
    }

    private void onViewReady() {
        AppLogger.info("HoverGamePresenter: View готов — начинаем спавн целей");
        spawnInitialTargets();
        startTimer();
    }

    private void spawnInitialTargets() {
        while (activeCircles < settings.getMaxCirclesCount() && gameActive) {
            spawnRandomTarget();
        }
    }

    private void spawnRandomTarget() {
        if (!gameActive) return;

        double paneWidth = view.getWidth();
        double paneHeight = view.getHeight();

        if (paneWidth <= 50 || paneHeight <= 50) {
            AppLogger.warn("HoverGamePresenter: Размеры ещё малы");
            return;
        }

        // Правильно собираем активные точки
        List<double[]> activePoints = view.getActiveTargetPositions();

        // Добавляем последнюю сбитую цель (важная логика!)
        if (hasLastCircle) {
            activePoints.add(lastCircle);
        }

        // Получаем новые координаты
        double[] coords = trajectoryGenerator.nextPoint(paneWidth, paneHeight, settings.getRadius(), activePoints);

        double x = coords[0];
        double y = coords[1];

        Color color = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256), 0.85);

        view.addTarget(x, y, settings.getRadius(), color);
        activeCircles++;
    }
    private void onTargetHit(TargetHitEvent event) {
        if (!gameActive) return;

        long relNs = System.nanoTime() - gameStartTimeNs;

        clickData.add(new ClickData(relNs,
                event.cursorX(), event.cursorY(),
                event.targetX(), event.targetY(),
                event.radius()));

        // === КРИТИЧНО ВАЖНО ===
        // Сохраняем координаты только что уничтоженной цели
        lastCircle[0] = event.targetX();
        lastCircle[1] = event.targetY();
        hasLastCircle = true;

        score++;
        activeCircles--;
        view.setScore(score);

        // Анимация уничтожения — вызываем из View
        // (пока оставляем вызов в HoverGameView.addTarget)

        if (activeCircles < settings.getMaxCirclesCount()) {
            spawnRandomTarget();
        }
    }

    private void startTimer() {
        if (timer != null) timer.stop();

        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingTime--;
            view.setTime(remainingTime);
            if (remainingTime <= 0) endGame();
        }));

        timer.setCycleCount(settings.getDurationSeconds());
        timer.play();
    }

    private void endGame() {
        gameActive = false;
        if (timer != null) timer.stop();

        try {
            gameService.addGameClicks(settings.getRadius(), settings.getSeed(), new ArrayList<>(clickData));
        } catch (Exception e) {
            AppLogger.error("Ошибка сохранения результата", e);
        }

        sceneManager.showResults();
    }

    private void goToMenu() {
        gameActive = false;
        if (timer != null) timer.stop();
        sceneManager.showMenu();
    }

    private void restartGame() {
        startNewGame();
    }
}