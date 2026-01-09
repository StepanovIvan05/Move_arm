package com.example.move_arm;

import com.example.move_arm.database.ClickDao;
import com.example.move_arm.model.ClickData;
import com.example.move_arm.model.GameResult;
import com.example.move_arm.service.GameService;
import com.example.move_arm.model.Statistics;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.util.List;

public class MoreResultsController {

    @FXML private LineChart<Number, Number> clickIntervalsChart;
    @FXML private LineChart<Number, Number> cursorDistanceChart;
    @FXML private LineChart<Number, Number> movementSpeedChart;
    @FXML private LineChart<Number, Number> normalizedDeviationChart;
    @FXML private GridPane summaryTable;

    private SceneManager sceneManager;
    private final GameService gameService = GameService.getInstance();
    private final ClickDao clickDao = new ClickDao();

    public void setSceneManager(SceneManager manager) { this.sceneManager = manager; }

    @FXML
    public void initialize() {
        AppLogger.info("MoreResultsController: инициализация");
        showMoreResults();
    }

    @FXML
    public void showMoreResults() {
        summaryTable.getChildren().clear();
        clickIntervalsChart.getData().clear();
        cursorDistanceChart.getData().clear();
        movementSpeedChart.getData().clear();
        normalizedDeviationChart.getData().clear();

        List<GameResult> results = gameService.getResultsForCurrentUser();
        if (results == null || results.isEmpty()) {
            summaryTable.add(new Label("Нет данных"), 0, 0);
            return;
        }

        GameResult last = results.get(0);
        int resultId = last.getId();

        List<ClickData> clicks = clickDao.readClicksForResult(resultId);
        if (clicks == null || clicks.isEmpty()) {
            summaryTable.add(new Label("Нет кликов для этого результата"), 0, 0);
            return;
        }

        int currentRadius = clicks.getFirst().getRadius(); // допустим, радиус последнего клика
        int bestScore = clickDao.getMaxClicksForUserAndRadius(last.getUserId(), currentRadius);

        summaryTable.add(new Label("Рекорд для этого радиуса:"), 0, 4);
        summaryTable.add(new Label(String.valueOf(bestScore)), 1, 4);

        // Таблица средних значений (из сохранённого GameResult)
        summaryTable.add(new Label("Средний интервал кликов (мс):"), 0, 0);
        summaryTable.add(new Label(String.format("%.2f", last.getAvgIntervalMs())), 1, 0);

        summaryTable.add(new Label("Среднее расстояние до центра (px):"), 0, 1);
        summaryTable.add(new Label(String.format("%.2f", last.getAvgDistancePx())), 1, 1);

        summaryTable.add(new Label("Средняя скорость (px/мс):"), 0, 2);
        summaryTable.add(new Label(String.format("%.4f", last.getAvgSpeed())), 1, 2);

        summaryTable.add(new Label("Процент попаданий (%):"), 0, 3);
        summaryTable.add(new Label(String.format("%.2f", last.getHitRate())), 1, 3);

        // === Построение графиков с реальными данными ===

        // 1) Интервалы между кликами (ms)
        XYChart.Series<Number, Number> intervalsSeries = new XYChart.Series<>();
        intervalsSeries.setName("Интервал (мс)");
        for (int i = 1; i < clicks.size(); i++) {
            long prev = clicks.get(i-1).getClickTimeNs();
            long curr = clicks.get(i).getClickTimeNs();
            double intervalMs = (curr - prev) / 1_000_000.0;
            intervalsSeries.getData().add(new XYChart.Data<>(i, intervalMs));
        }
        clickIntervalsChart.getData().add(intervalsSeries);
        ((NumberAxis)clickIntervalsChart.getXAxis()).setLabel("Номер клика");
        ((NumberAxis)clickIntervalsChart.getYAxis()).setLabel("Интервал (мс)");

        // 2) Расстояние курсора от центра (px)
        XYChart.Series<Number, Number> distSeries = new XYChart.Series<>();
        distSeries.setName("Расстояние (px)");
        for (int i = 0; i < clicks.size(); i++) {
            double dist = clicks.get(i).getCursor().distance(clicks.get(i).getCenter());
            distSeries.getData().add(new XYChart.Data<>(i+1, dist));
        }
        cursorDistanceChart.getData().add(distSeries);
        ((NumberAxis)cursorDistanceChart.getXAxis()).setLabel("Номер клика");
        ((NumberAxis)cursorDistanceChart.getYAxis()).setLabel("Расстояние (px)");

        // 3) Скорость (px / ms): расстояние / интервал
        XYChart.Series<Number, Number> speedSeries = new XYChart.Series<>();
        speedSeries.setName("Скорость (px/ms)");
        for (int i = 1; i < clicks.size(); i++) {
            double dist = clicks.get(i).getCursor().distance(clicks.get(i-1).getCursor());
            double dtMs = (clicks.get(i).getClickTimeNs() - clicks.get(i-1).getClickTimeNs()) / 1_000_000.0;
            double speed = dtMs > 0 ? dist / dtMs : 0.0;
            speedSeries.getData().add(new XYChart.Data<>(i, speed));
        }
        movementSpeedChart.getData().add(speedSeries);
        ((NumberAxis)movementSpeedChart.getXAxis()).setLabel("Номер клика");
        ((NumberAxis)movementSpeedChart.getYAxis()).setLabel("Скорость (px/ms)");

        // 4) Нормализованное отклонение (если у тебя radius есть)
        XYChart.Series<Number, Number> normSeries = new XYChart.Series<>();
        normSeries.setName("Нормализованное отклонение");
        for (int i = 0; i < clicks.size(); i++) {
            double deviation = clicks.get(i).getCursor().distance(clicks.get(i).getCenter());
            double radius = clicks.get(i).getRadius();
            double norm = radius > 0 ? deviation / radius : 0.0;
            normSeries.getData().add(new XYChart.Data<>(i+1, norm));
        }
        normalizedDeviationChart.getData().add(normSeries);
        ((NumberAxis)normalizedDeviationChart.getXAxis()).setLabel("Номер клика");
        ((NumberAxis)normalizedDeviationChart.getYAxis()).setLabel("Нормализованное отклонение");
    }

    @FXML
    private void handleToMenuButton() {
        gameService.clear();
        try {
            sceneManager.showMenu();
        } catch (Exception e) {
            AppLogger.error("MoreResultsController: Ошибка при переходе в меню", e);
        }
    }
}
