package com.example.move_arm.ui.presenter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.example.move_arm.model.TripletRecord;
import com.example.move_arm.model.settings.HoverGameSettings;
import com.example.move_arm.service.GameService;
import com.example.move_arm.service.NeuralTripletGenerator;
import com.example.move_arm.service.NeuralTripletGenerator.TripletData;
import com.example.move_arm.service.SettingsService;
import com.example.move_arm.ui.SceneManager;
import com.example.move_arm.ui.view.NeuralGameView;
import com.example.move_arm.ui.view.NeuralGameView.TargetCell;
import com.example.move_arm.ui.view.NeuralHitEvent;
import com.example.move_arm.util.AppLogger;
import com.example.move_arm.util.GridUtils;
import com.example.move_arm.util.TripletGeometry;
import com.example.move_arm.util.TripletGeometry.GeometryData;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class NeuralGamePresenter {

    private final NeuralGameView view;
    private final GameService gameService;
    private final SettingsService settingsService;
    private final SceneManager sceneManager;
    private final NeuralTripletGenerator generator;

    private final int radius = 40;                    // фиксированный радиус

    private HoverGameSettings settings;
    private Timeline timer;

    private long gameStartTimeNs;
    private int score;
    private int remainingTime;
    private boolean gameActive;

    private int lastHitCell = -1;

    // Буфер данных для БД
    private final List<TripletRecord> gameBuffer = new ArrayList<>();
    private int tripletCounter = 0;

    private final Random random = new Random();

    public NeuralGamePresenter(NeuralGameView view, SceneManager sceneManager) {
        this.view = view;
        this.sceneManager = sceneManager;
        this.gameService = GameService.getInstance();
        this.settingsService = SettingsService.getInstance();
        this.generator = new NeuralTripletGenerator();

        view.setOnNeuralTargetHit(this::onNeuralTargetHit);
        view.setOnToMenu(this::goToMenu);
        view.setOnRestart(this::restartGame);
        view.setOnViewReady(this::onViewReady);
    }

    public void startNewGame() {
        AppLogger.info("NeuralGamePresenter: startNewGame() — запуск Neural режима");
        settings = settingsService.getHoverSettings();
        resetGameState();
        view.start();
        view.setScore(0);
        view.setTime(settings.getDurationSeconds());
        view.setUserName(gameService.getCurrentUser().getUsername());
        gameStartTimeNs = System.nanoTime();
    }

    private void resetGameState() {
        gameActive = true;
        score = 0;
        remainingTime = settings.getDurationSeconds();
        lastHitCell = -1;
        gameBuffer.clear();
        tripletCounter = 0;
        generator.reset();

        if (timer != null) {
            timer.stop();
            timer = null;
        }

        view.clearField();
        AppLogger.info("NeuralGamePresenter: состояние сброшено");
    }

    private void onViewReady() {
        AppLogger.info("NeuralGamePresenter: View готов — спавним начальную тройку");
        spawnInitialTriplet();
        startTimer();
    }

    private void spawnInitialTriplet() {
        int cell1 = randomCell();
        int cell2 = randomCell(cell1);
        spawnCell(cell1);
        spawnCell(cell2);
        spawnThirdTarget(cell1, cell2);
    }

    private void spawnCell(int cell) {
        double[] xy = GridUtils.cellToXy(cell, view.getWidth(), view.getHeight());
        view.addTargetWithCell(xy[0], xy[1], this.radius, randomColor(), cell);
    }

    private void spawnThirdTarget(int activeCell1, int activeCell2) {
        int newCell = generator.generateThirdCell(activeCell1, activeCell2,
                view.getWidth(), view.getHeight());
        spawnCell(newCell);
    }

    private void onNeuralTargetHit(NeuralHitEvent event) {
        if (!gameActive) return;

        score++;
        view.setScore(score);

        int previousHitCell = lastHitCell;

        generator.onHit(event.cellIndex(), event.lifetimeNs());

        TripletData data = generator.getLastData();
        if (data != null && data.hitIndex >= 0) {
            GeometryData geom = TripletGeometry.compute(
                data.t1Cell, data.t2Cell, data.t3Cell, data.hitIndex);

            TripletRecord rec = new TripletRecord();
            rec.tripletIndex = tripletCounter++;
            rec.t1Cell = data.t1Cell;
            rec.t2Cell = data.t2Cell;
            rec.t3Cell = data.t3Cell;
            rec.hitTargetIndex = data.hitIndex;
            rec.hitTtkNs = data.hitTtkNs;
            rec.spawnNs = data.spawnNs;
            rec.radius = this.radius;
            rec.screenWidth = (int) view.getWidth();
            rec.screenHeight = (int) view.getHeight();
            rec.centroidRow = geom.centroidRow;
            rec.centroidCol = geom.centroidCol;
            rec.t1Angle = geom.t1Angle;
            rec.t2Angle = geom.t2Angle;
            rec.t3Angle = geom.t3Angle;
            rec.hitToMiss1Dist = geom.hitToMiss1Dist;
            rec.hitToMiss2Dist = geom.hitToMiss2Dist;
            rec.miss1ToMiss2Dist = geom.miss1ToMiss2Dist;
            rec.spread = geom.spread;
            rec.previousHitCell = previousHitCell;

            gameBuffer.add(rec);
        }

        lastHitCell = event.cellIndex();
        generator.reset();

        List<TargetCell> active = view.getActiveTargetsWithCells();
        if (active.size() >= 2) {
            spawnThirdTarget(active.get(0).cellIndex(), active.get(1).cellIndex());
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
        if (timer != null) {
            timer.stop();
            timer = null;
        }

        saveGameData();

        // Важно: помечаем текущий режим
        gameService.setCurrentGameTypeToNeural();

        AppLogger.info("NeuralGamePresenter: Игра завершена → показываем результаты");
        sceneManager.showResults();
    }

    private void saveGameData() {
        int userId = gameService.getCurrentUser().getId();
        long timestamp = System.currentTimeMillis() / 1000;

        for (TripletRecord rec : gameBuffer) {
            rec.userId = userId;
            rec.timestamp = timestamp;
        }

        gameService.saveTripletsBatch(gameBuffer);
        AppLogger.info("NeuralGamePresenter: сохранено " + gameBuffer.size() + " троек");
    }

    // ==================== Restart — ИСПРАВЛЕННЫЙ ====================
    private void restartGame() {
        AppLogger.info("NeuralGamePresenter: === РЕСТАРТ NEURAL ИГРЫ ===");

        gameActive = false;
        if (timer != null) {
            timer.stop();
            timer = null;
        }

        view.clearField();
        gameBuffer.clear();
        tripletCounter = 0;
        lastHitCell = -1;
        generator.reset();

        // Запускаем заново
        startNewGame();
    }

    private void goToMenu() {
        gameActive = false;
        if (timer != null) timer.stop();
        sceneManager.showMenu();
    }

    // ==================== Вспомогательные методы ====================
    private int randomCell(int... exclude) {
        int cell;
        do {
            cell = random.nextInt(GridUtils.CELLS);
        } while (contains(exclude, cell));
        return cell;
    }

    private boolean contains(int[] arr, int val) {
        for (int v : arr) {
            if (v == val) return true;
        }
        return false;
    }

    private Color randomColor() {
        return Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256), 0.85);
    }
}