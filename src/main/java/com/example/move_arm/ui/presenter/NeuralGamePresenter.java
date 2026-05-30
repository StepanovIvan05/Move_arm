package com.example.move_arm.ui.presenter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.example.move_arm.model.TripletRecord;
import com.example.move_arm.model.settings.HoverGameSettings;
import com.example.move_arm.service.GameService;
import com.example.move_arm.service.NeuralTripletGenerator;
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

        int clickedCell = event.cellIndex();
        
        // 1. Получаем ячейки, которые ОСТАЛИСЬ на экране (их должно быть 2)
        List<TargetCell> remainingTargets = view.getActiveTargetsWithCells();
        
        // Кликнутая ячейка уже исчезла из view, значит в remainingTargets её нет.
        // Всего на экране до клика было: clickedCell + эти две оставшиеся.
        if (remainingTargets.size() < 2) {
            AppLogger.info("NeuralGamePresenter: Критическая ошибка, на поле осталось меньше 2 целей!");
            return;
        }

        score++;
        view.setScore(score);
        int previousHitCell = lastHitCell;

        // Восстанавливаем тройку ячеек, которая была на экране в момент клика:
        // Пусть clickedCell будет на первом месте (t1), а оставшиеся две — на t2 и t3.
        int t1 = clickedCell;
        int t2 = remainingTargets.get(0).cellIndex();
        int t3 = remainingTargets.get(1).cellIndex();

        // Так как мы сами назначили clickedCell как t1, то индекс попадания ВСЕГДА равен 0!
        int hitTargetIndex = 0;

        // --- СТАРАЯ РОДНАЯ ЛОГИКА СОХРАНЕНИЯ ---
        GeometryData geom = TripletGeometry.compute(t1, t2, t3, hitTargetIndex);

        TripletRecord rec = new TripletRecord();
        rec.tripletIndex = tripletCounter++;
        rec.t1Cell = t1;
        rec.t2Cell = t2;
        rec.t3Cell = t3;
        rec.hitTargetIndex = hitTargetIndex;
        rec.hitTtkNs = event.lifetimeNs();
        rec.spawnNs = System.nanoTime(); 
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
        AppLogger.info("NeuralGamePresenter: Тройка успешно добавлена в буфер. В буфере: " + gameBuffer.size());

        lastHitCell = clickedCell;

        // Передаем в генератор факт клика для истории скоростей
        generator.onHit(clickedCell, event.lifetimeNs());

        // --- ГЕНЕРАЦИЯ СЛЕДУЮЩЕЙ ЦЕЛИ ---
        // Передаем генератору две выжившие ячейки, чтобы он нашел для них идеальную третью
        spawnThirdTarget(t2, t3);
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

        //saveGameData();

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