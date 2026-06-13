package com.example.move_arm.ui.presenter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.example.move_arm.HoldTarget;
import com.example.move_arm.model.HoldAttempt;
import com.example.move_arm.model.User;
import com.example.move_arm.model.settings.HoldGameSettings;
import com.example.move_arm.service.GameService;
import com.example.move_arm.service.SettingsService;
import com.example.move_arm.ui.SceneManager;
import com.example.move_arm.ui.view.HoldGameView;
import com.example.move_arm.util.AppLogger;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class HoldGamePresenter {

    private final HoldGameView view;
    private final SceneManager sceneManager;
    private final GameService gameService;
    private final SettingsService settingsService;
    private final Random random = new Random();

    private HoldGameSettings settings;
    private Timeline timer;
    private AudioClip hoverSound;

    private final List<HoldAttempt> allAttempts = new ArrayList<>();
    private int score = 0;
    private int activeTargets = 0;
    private int remainingTime = 0;
    private int globalAttemptCounter = 0;
    private boolean gameActive = false;

    public HoldGamePresenter(HoldGameView view, SceneManager sceneManager) {
        this.view = view;
        this.sceneManager = sceneManager;
        this.gameService = GameService.getInstance();
        this.settingsService = SettingsService.getInstance();

        loadSound();

        view.setOnToMenu(this::goToMenu);
        view.setOnRestart(this::restartGame);
        view.setOnViewReady(this::onViewReady);
    }

    public void startNewGame() {
        settings = settingsService.getHoldSettings();
        resetGameState();

        view.start();
        view.setScore(0);
        view.setTime(remainingTime);

        User user = gameService.getCurrentUser();
        view.setUserName(user != null ? user.getUsername() : "guest");
    }

    private void loadSound() {
        try {
            var url = getClass().getResource("/com/example/move_arm/sounds/cartoon-bubble-pop-01-.mp3");
            if (url != null) {
                hoverSound = new AudioClip(url.toExternalForm());
            }
        } catch (Exception ignored) {
        }
    }

    private void resetGameState() {
        gameActive = true;
        score = 0;
        activeTargets = 0;
        globalAttemptCounter = 0;
        allAttempts.clear();
        remainingTime = settings.getDurationSeconds();

        if (timer != null) {
            timer.stop();
            timer = null;
        }

        view.clearField();
    }

    private void onViewReady() {
        if (!gameActive || activeTargets > 0) {
            return;
        }

        spawnInitialTargets();
        startTimer();
    }

    private void spawnInitialTargets() {
        while (gameActive && activeTargets < settings.getMaxCirclesCount()) {
            spawnHoldTarget();
        }
    }

    private void spawnHoldTarget() {
        if (!gameActive) {
            return;
        }

        double paneWidth = view.getWidth();
        double paneHeight = view.getHeight();
        if (paneWidth <= 0 || paneHeight <= 0) {
            return;
        }

        int radius = settings.getRadius();
        double x = random.nextDouble() * (paneWidth - radius * 2);
        double y = random.nextDouble() * (paneHeight - radius * 2);
        double centerX = x + radius;
        double centerY = y + radius;
        Color color = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256), 1.0);
        double holdDurationSeconds = Math.max(settings.getHoldTimeMs(), 1) / 1000.0;

        final long[] entryTimeNs = {0L};
        final boolean[] processed = {false};
        final HoldTarget[] targetRef = new HoldTarget[1];

        Runnable onComplete = () -> {
            if (!gameActive || processed[0] || targetRef[0] == null) {
                return;
            }

            processed[0] = true;
            long now = System.nanoTime();

            allAttempts.add(new HoldAttempt(
                    ++globalAttemptCounter,
                    entryTimeNs[0],
                    now,
                    settings.getHoldTimeMs(),
                    true,
                    centerX,
                    centerY
            ));

            if (hoverSound != null) {
                hoverSound.play();
            }

            view.removeTarget(targetRef[0]);
            view.playDestroyAnimation(centerX, centerY, radius, color);

            score++;
            activeTargets--;
            view.setScore(score);

            if (activeTargets < settings.getMaxCirclesCount()) {
                spawnHoldTarget();
            }
        };

        Runnable onEntered = () -> entryTimeNs[0] = System.nanoTime();

        Runnable onExited = () -> {
            if (!gameActive || processed[0] || entryTimeNs[0] <= 0 || targetRef[0] == null) {
                return;
            }

            if (view.containsTarget(targetRef[0])) {
                long now = System.nanoTime();
                long actualMs = (now - entryTimeNs[0]) / 1_000_000;

                allAttempts.add(new HoldAttempt(
                        ++globalAttemptCounter,
                        entryTimeNs[0],
                        now,
                        actualMs,
                        false,
                        centerX,
                        centerY
                ));

                entryTimeNs[0] = 0L;
            }
        };

        targetRef[0] = view.addHoldTarget(
                x,
                y,
                radius,
                color,
                holdDurationSeconds,
                onComplete,
                onEntered,
                onExited
        );

        activeTargets++;
    }

    private void startTimer() {
        if (timer != null) {
            timer.stop();
        }

        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            remainingTime--;
            view.setTime(remainingTime);
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

        view.clearField();

        try {
            int savedId = gameService.addHoldGameResults(settings.getRadius(), new ArrayList<>(allAttempts));
            AppLogger.info("Hold game result saved, id=" + savedId);
        } catch (Exception e) {
            AppLogger.error("Failed to save hold game result", e);
        }

        sceneManager.showResults();
    }

    private void goToMenu() {
        gameActive = false;
        if (timer != null) {
            timer.stop();
        }
        sceneManager.showMenu();
    }

    private void restartGame() {
        startNewGame();
    }
}
