package com.example.move_arm.ui.presenter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.example.move_arm.model.ClickData;
import com.example.move_arm.model.settings.HoverGameSettings;
import com.example.move_arm.service.GameService;
import com.example.move_arm.service.GeneratorFactory;
import com.example.move_arm.service.LevelGeneratorService;
import com.example.move_arm.service.PointGenerator;
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
    private final SceneManager sceneManager;
    private PointGenerator trajectoryGenerator;

    private HoverGameSettings settings;
    private final List<ClickData> clickData = new ArrayList<>();
    private Timeline timer;
    private long gameStartTimeNs = 0;
    private int score = 0;
    private int activeCircles = 0;
    private int remainingTime;
    private boolean gameActive = false;

    // Presenter как единственный источник правды для активных целей
    private final List<double[]> activeTargets = new ArrayList<>();
    // Последняя сбитая цель (для генератора траектории)
    private double[] lastHitTarget = null;

    private final Random random = new Random();

    public HoverGamePresenter(GameView view, SceneManager sceneManager) {
        this.view = view;
        this.sceneManager = sceneManager;
        this.gameService = GameService.getInstance();
        this.settingsService = SettingsService.getInstance();

        view.setOnTargetHit(this::onTargetHit);
        view.setOnToMenu(this::goToMenu);
        view.setOnRestart(this::restartGame);
        view.setOnViewReady(this::onViewReady);
    }

    public void startNewGame() {
        AppLogger.info("HoverGamePresenter: startNewGame()");

        settings = settingsService.getHoverSettings();
        
        trajectoryGenerator = GeneratorFactory.createGenerator(settings.getGeneratorType());
        
        if (trajectoryGenerator instanceof VectorTrajectoryGenerator) {
            ((VectorTrajectoryGenerator) trajectoryGenerator).setDifficulty(settings.getDifficulty());
        }
        
        if (trajectoryGenerator instanceof LevelGeneratorService) {
            ((LevelGeneratorService) trajectoryGenerator).initialize(settings.getSeed());
        }
        
        trajectoryGenerator.reset();

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
        activeTargets.clear();
        lastHitTarget = null;
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

        // Подготавливаем список для генератора: активные цели + последняя сбитая в конце
        List<double[]> pointsForGenerator = new ArrayList<>(activeTargets);
        if (lastHitTarget != null) {
            pointsForGenerator.add(lastHitTarget.clone());
        }

        double[] coords = trajectoryGenerator.nextPoint(
            paneWidth,
            paneHeight,
            settings.getRadius(),
            pointsForGenerator
        );

        double x = coords[0];
        double y = coords[1];

        Color color = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256), 0.85);

        view.addTarget(x, y, settings.getRadius(), color);
        activeTargets.add(new double[]{x, y});
        activeCircles++;
    }

    private void onTargetHit(TargetHitEvent event) {
        if (!gameActive) return;

        long relNs = System.nanoTime() - gameStartTimeNs;

        clickData.add(new ClickData(relNs,
                event.cursorX(), event.cursorY(),
                event.targetX(), event.targetY(),
                event.radius()));

        // Обновляем последнюю сбитую цель
        lastHitTarget = new double[]{
            event.targetX(),
            event.targetY()
        };

        // Удаляем сбитую цель из активных
        activeTargets.removeIf(target -> 
            Math.abs(target[0] - event.targetX()) < 0.1 && 
            Math.abs(target[1] - event.targetY()) < 0.1
        );

        score++;
        activeCircles--;
        view.setScore(score);

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
            gameService.addGameClicks(settings.getRadius(), settings.getSeed(), settings.getDifficulty(), new ArrayList<>(clickData));
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
