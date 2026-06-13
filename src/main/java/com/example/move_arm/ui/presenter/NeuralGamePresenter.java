package com.example.move_arm.ui.presenter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.example.move_arm.model.ClickData;
import com.example.move_arm.model.TripletRecord;
import com.example.move_arm.model.settings.NeuralGameSettings;
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

    private int radius;

    private NeuralGameSettings settings;
    private Timeline timer;

    private long gameStartTimeNs;
    private int score;
    private int remainingTime;
    private boolean gameActive;

    private int lastHitCell = -1;

    // Буфер данных для пакетной записи в БД (neural triplets)
    private final List<TripletRecord> gameBuffer = new ArrayList<>();
    private int tripletCounter = 0;

    // Буфер данных для пакетной записи в БД (neural clicks как hover-clicks)
    private final List<ClickData> clickData = new ArrayList<>();

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
        AppLogger.info("NeuralGamePresenter: startNewGame()");

        settings = settingsService.getNeuralSettings();
        radius = settings.getRadius();
        resetGameState();

        view.start();

        view.setScore(0);
        view.setTime(settings.getDurationSeconds());
        view.setUserName(gameService.getCurrentUser().getUsername());

        gameStartTimeNs = System.nanoTime();

        AppLogger.info("width=" + view.getWidth() + ", height=" + view.getHeight() + ", radius=" + radius);

        spawnInitialTriplet();
        startTimer();
    }

    private void resetGameState() {
        gameActive = true;
        score = 0;
        remainingTime = settings.getDurationSeconds();
        lastHitCell = -1;
        gameBuffer.clear();
        clickData.clear();
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
        AppLogger.info("NeuralGamePresenter: View готов");
        startNewGame();
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
        view.addTargetWithCell(xy[0], xy[1], radius, randomColor(), cell);
    }

    private void spawnThirdTarget(int activeCell1, int activeCell2) {
        int newCell = generator.generateThirdCell(activeCell1, activeCell2,
                view.getWidth(), view.getHeight());
        spawnCell(newCell);
    }

    private void onNeuralTargetHit(NeuralHitEvent event) {
        if (!gameActive) return;

        int clickedCell = event.cellIndex();

        // Получаем ячейки, которые ОСТАЛИСЬ на экране (их должно быть ровно 2)
        List<TargetCell> remainingTargets = view.getActiveTargetsWithCells();

        if (remainingTargets.size() < 2) {
            AppLogger.info("NeuralGamePresenter: Критическая ошибка, на поле осталось меньше 2 целей!");
            return;
        }

        long gameTimeMs = (System.nanoTime() - gameStartTimeNs);

        score++;
        view.setScore(score);
        int previousHitCell = lastHitCell;

        // Честное восстановление исходных ролей целей для БД на основе генератора логов
        NeuralTripletGenerator.TripletData lastGenData = generator.getLastData();

        int t1, t2, t3;
        int hitTargetIndex = 0;

        if (lastGenData != null) {
            t1 = lastGenData.t1Cell;
            t2 = lastGenData.t2Cell;
            t3 = lastGenData.t3Cell;

            if (clickedCell == t1) hitTargetIndex = 0;
            else if (clickedCell == t2) hitTargetIndex = 1;
            else if (clickedCell == t3) hitTargetIndex = 2;
        } else {
            // Резервный фолбэк для первого выстрела
            t1 = clickedCell;
            t2 = remainingTargets.get(0).cellIndex();
            t3 = remainingTargets.get(1).cellIndex();
            hitTargetIndex = 0;
        }

        // Вычисляем честную геометрию без сдвигов и хардкода
        GeometryData geom = TripletGeometry.compute(t1, t2, t3, hitTargetIndex);

        TripletRecord rec = new TripletRecord();
        rec.tripletIndex = tripletCounter++;
        rec.t1Cell = t1;
        rec.t2Cell = t2;
        rec.t3Cell = t3;
        rec.hitTargetIndex = hitTargetIndex;
        rec.hitTtkNs = event.lifetimeNs();
        rec.spawnNs = System.nanoTime();
        rec.radius = radius;
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
        AppLogger.info("NeuralGamePresenter: Тройка добавлена в буфер. Всего: " + gameBuffer.size());

        // Собирать клики как в Hover: ClickData (time/cursor/center/radius)
        // В NeuralHitEvent есть только cellIndex; поэтому используем координаты ячеек.
        // cursor/center в hover — это позиция курсора и центр круга.
        // Для neural делаем: cursor = центр "попавшей" клетки, center = центр той же клетки.
        double[] xy = GridUtils.cellToXy(clickedCell, view.getWidth(), view.getHeight());

        clickData.add(new ClickData(
                gameTimeMs,
                event.cursorX(),
                event.cursorY(),
                event.targetX(),
                event.targetY(),
                radius
        ));

        lastHitCell = clickedCell;

        // Оповещаем историю генератора о совершенном попадании
        generator.onHit(clickedCell, event.lifetimeNs());

        // Передаем генератору две выжившие ячейки из JavaFX, чтобы он построил на них адаптивное продолжение
        spawnThirdTarget(remainingTargets.get(0).cellIndex(), remainingTargets.get(1).cellIndex());
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
        AppLogger.info("NeuralGamePresenter: Сохранено в БД: " + gameBuffer.size() + " троек");

        // Сохранение neural clicks как в hover:
        // В проекте сейчас нет seed/difficulty/generatorType для neural (NeuralGameSettings пустой),
        // поэтому используем значения по умолчанию, как это делает ClickGameService через ADAPTIVE/seed/difficulty.
        // Если для neural в будущем появятся параметры — сюда нужно будет подставить их.
        gameService.addGameClicks(
                radius,
                0, // seed недоступен для neural в текущей модели
                com.example.move_arm.model.TrajectoryDifficulty.MEDIUM,
                new ArrayList<>(clickData)
        );

        AppLogger.info("NeuralGamePresenter: Сохранено в БД: " + clickData.size() + " neural кликов (как hover)");
    }

    private void restartGame() {
        AppLogger.info("=== РЕСТАРТ NEURAL ИГРЫ ===");

        gameActive = false;

        if (timer != null) {
            timer.stop();
            timer = null;
        }

        startNewGame();
    }

    private void goToMenu() {
        gameActive = false;
        if (timer != null) timer.stop();
        sceneManager.showMenu();
    }

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