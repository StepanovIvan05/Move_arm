package com.example.move_arm;

import com.example.move_arm.database.ClickDao;
import com.example.move_arm.model.GameResult;
import com.example.move_arm.service.GameService;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;

import java.util.List;

public class StatisticsController {

    @FXML private Slider radiusSlider;
    @FXML private Label radiusLabel;
    @FXML private GridPane statsGrid;
    @FXML private LineChart<Number, Number> scoresChart;
    @FXML private Button backButton;

    private SceneManager sceneManager;
    private final GameService gameService = GameService.getInstance();
    private final ClickDao clickDao = new ClickDao();
    private int radius = 50;

    public void setSceneManager(SceneManager sm) {
        this.sceneManager = sm;
    }

    @FXML
    public void initialize() {
        radiusLabel.setText(String.valueOf((int) radius));

        radiusSlider.setMin(20);
        radiusSlider.setMax(100);
        radiusSlider.setBlockIncrement(10); // шаг при перемещении стрелками
        radiusSlider.setMajorTickUnit(10);  // шаг для рисования делений
        radiusSlider.setMinorTickCount(0);  // без промежуточных делений
        radiusSlider.setSnapToTicks(true);  // "привязка" к делениям

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
        List<Integer> AvgClicsList = clickDao.getAvgClicksListForUserAndRadius(gameService.getCurrentUser().getId(), radius);
        List<Double> intervals = clickDao.getAvgClickIntervalsForUserAndRadius(gameService.getCurrentUser().getId(), radius);
        double avgIntervalMs = intervals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double avgScore = AvgClicsList.stream().mapToDouble(Integer::doubleValue).average().orElse(0.0);
        int bestScore = clickDao.getMaxClicksForUserAndRadius(gameService.getCurrentUser().getId(), radius);

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

// Добавляем серию
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Очки по играм");
        int i = 1;
        for (int r : AvgClicsList) {
            series.getData().add(new XYChart.Data<>(i++, r));
        }
        scoresChart.getData().add(series);
    }
}
