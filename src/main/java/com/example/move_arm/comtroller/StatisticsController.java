package com.example.move_arm.comtroller;

import com.example.move_arm.ui.SceneManager;
import com.example.move_arm.database.GameResultDao;
import com.example.move_arm.model.GameResult;
import com.example.move_arm.model.GeneratorType;
import com.example.move_arm.model.TrajectoryDifficulty;
import com.example.move_arm.model.settings.HoverGameSettings;
import com.example.move_arm.service.GameService;
import com.example.move_arm.service.SettingsService;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class StatisticsController {

    @FXML private Slider radiusSlider;
    @FXML private Label radiusLabel;
    @FXML private GridPane statsGrid;
    @FXML private LineChart<Number, Number> scoresChart;
    @FXML private Button backButton;
    @FXML private ComboBox<GeneratorType> generatorTypeComboBox;
    @FXML private ComboBox<Integer> seedComboBox;
    @FXML private ComboBox<TrajectoryDifficulty> difficultyComboBox;
    @FXML private VBox seedContainer;
    @FXML private VBox adaptiveContainer;

    private int seed = 67;
    private TrajectoryDifficulty difficulty = TrajectoryDifficulty.MEDIUM;
    private GeneratorType generatorType = GeneratorType.ADAPTIVE;

    private SceneManager sceneManager;
    private final GameService gameService = GameService.getInstance();
    private final SettingsService settingsService = SettingsService.getInstance();
    private final GameResultDao gameResultDao = new GameResultDao();
    private final HoverGeneratorOptionsBinder generatorOptionsBinder = new HoverGeneratorOptionsBinder();
    private int radius = 50;

    public void setSceneManager(SceneManager sm) {
        this.sceneManager = sm;
    }

    @FXML
    public void initialize() {
        HoverGameSettings hoverSettings = settingsService.getHoverSettings();
        if (hoverSettings != null) {
            seed = hoverSettings.getSeed();
            difficulty = hoverSettings.getDifficulty();
            generatorType = hoverSettings.getGeneratorType();
            radius = hoverSettings.getRadius();
        }

        radiusLabel.setText(String.valueOf(radius));

        radiusSlider.setMin(20);
        radiusSlider.setMax(100);
        radiusSlider.setBlockIncrement(10); // шаг при перемещении стрелками
        radiusSlider.setMajorTickUnit(10);  // шаг для рисования делений
        radiusSlider.setMinorTickCount(0);  // без промежуточных делений
        radiusSlider.setSnapToTicks(true);  // "привязка" к делениям
        radiusSlider.setValue(radius);

        seedComboBox.getItems().setAll(0, 1, 67, 123, 999, 2024);
        seedComboBox.setValue(seed);
        difficultyComboBox.getItems().setAll(TrajectoryDifficulty.values());
        difficultyComboBox.setValue(difficulty);

        generatorOptionsBinder
                .register(GeneratorType.RANDOM, seedContainer)
                .register(GeneratorType.ADAPTIVE, adaptiveContainer);
        generatorType = generatorOptionsBinder.normalizeType(generatorType);
        generatorTypeComboBox.getItems().setAll(generatorOptionsBinder.getSupportedTypes());
        generatorTypeComboBox.setValue(generatorType);
        generatorOptionsBinder.showOptionsFor(generatorType);

        generatorTypeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal != generatorType) {
                generatorType = newVal;
                generatorOptionsBinder.showOptionsFor(newVal);
                updateStatistics();
            }
        });

        seedComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal != seed) {
                seed = newVal;
                updateStatistics();
            }
        });

        difficultyComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal != difficulty) {
                difficulty = newVal;
                updateStatistics();
            }
        });

        radiusSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int rounded = ((int) Math.round(newVal.doubleValue() / 10)) * 10;
            if (rounded != (int) oldVal.doubleValue()) {
                // только если реально изменилось целое значение
                radiusSlider.setValue(rounded);
                radius = rounded;
                radiusLabel.setText(String.valueOf(radius));
                updateStatistics();
            }
        });

        backButton.setOnAction(e -> sceneManager.showMenu());

        // начальная загрузка
        updateStatistics();
    }

    private void updateStatistics() {
        statsGrid.getChildren().clear();
        scoresChart.getData().clear();

        List<GameResult> results = gameService.getResultsForCurrentUser();
        if (results == null || results.isEmpty()) {
            statsGrid.add(new Label("Нет данных"), 0, 0);
            return;
        }

        // вычисляем рекорд, среднее очков, среднее время между кликами
        List<Integer> ScoresList = gameResultDao.findListScoreByUserGameTypeAndRadiusSeedDifficulty(gameService.getCurrentUser().getId(), gameService.getCurrentGameTypeId(), radius, seed, difficulty);
        List<Double> intervals = gameResultDao.findListAvgTimesByUserGameTypeAndRadiusSeedDifficulty(gameService.getCurrentUser().getId(), gameService.getCurrentGameTypeId(), radius, seed, difficulty);
        double avgIntervalMs = intervals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double avgScore = ScoresList.stream().mapToDouble(Integer::doubleValue).average().orElse(0.0);
        int bestScore = gameResultDao.findRecordScoreByUserGameTypeAndRadiusSeedDifficulty(gameService.getCurrentUser().getId(), gameService.getCurrentGameTypeId(), radius, seed, difficulty);

        statsGrid.add(new Label("Рекорд:"), 0, 0);
        statsGrid.add(new Label(String.valueOf(bestScore)), 1, 0);

        statsGrid.add(new Label("Среднее количество очков:"), 0, 1);
        statsGrid.add(new Label(String.format("%.2f", avgScore)), 1, 1);

        statsGrid.add(new Label("Среднее время между кликами (мс):"), 0, 2);
        statsGrid.add(new Label(String.format("%.2f", avgIntervalMs)), 1, 2);

        NumberAxis xAxis = (NumberAxis) scoresChart.getXAxis();
        NumberAxis yAxis = (NumberAxis) scoresChart.getYAxis();
        xAxis.setAutoRanging(true);
        yAxis.setAutoRanging(true);


        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Очки по играм");
        int i = 1;
        for (int r : ScoresList) {
            series.getData().add(new XYChart.Data<>(i++, r));
        }
        scoresChart.getData().add(series);
    }
}
