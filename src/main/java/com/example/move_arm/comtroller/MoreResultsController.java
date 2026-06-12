package com.example.move_arm.comtroller;

import com.example.move_arm.util.AppLogger;
import com.example.move_arm.ui.SceneManager;
import com.example.move_arm.database.ClickDao;
import com.example.move_arm.database.GameResultDao;
import com.example.move_arm.database.HoldAttemptDao;
import com.example.move_arm.model.ClickData;
import com.example.move_arm.model.GameResult;
import com.example.move_arm.model.HoldAttempt;
import com.example.move_arm.model.Statistics;
import com.example.move_arm.service.GameService;
import javafx.application.Platform;
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
    @FXML private Label modeLabel;

    private SceneManager sceneManager;
    private final GameService gameService = GameService.getInstance();
    private final ClickDao clickDao = new ClickDao();
    private final GameResultDao gameResultDao = new GameResultDao();
    private final HoldAttemptDao holdAttemptDao = new HoldAttemptDao();

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

        if ("neural".equalsIgnoreCase(gameService.getCurrentGameTypeString())) {
            modeLabel.setText("Режим: Neural");
            showNeuralResults(last);
        } else if ("hold".equals(gameService.getCurrentGameTypeString())) {
            modeLabel.setText("Режим: Hold");
            showHoldResults(last);
        } else {
            modeLabel.setText("Режим: Hover");
            showHoverResults(last);
        }
    }

    // ========================= HOVER RESULTS =========================

    private void showHoverResults(GameResult last) {
        int resultId = last.getId();

        List<ClickData> clicks = clickDao.readClicksForResult(resultId);
        if (clicks == null || clicks.isEmpty()) {
            summaryTable.add(new Label("Нет кликов для этого результата"), 0, 0);
            return;
        }

        int currentRadius = clicks.getFirst().getRadius();
        int bestScore = gameResultDao.findRecordScoreByGeneratorSettings(
                last.getUserId(),
                last.getGameTypeId(),
                currentRadius,
                last.getGeneratorType(),
                last.getSeed(),
                last.getDifficulty()
        );

        summaryTable.add(new Label("Рекорд для этих настроек генератора:"), 0, 4);
        summaryTable.add(new Label(String.valueOf(bestScore)), 1, 4);

        summaryTable.add(new Label("Средний интервал кликов (мс):"), 0, 0);
        summaryTable.add(new Label(String.format("%.2f", last.getAvgIntervalMs())), 1, 0);

        summaryTable.add(new Label("Среднее расстояние до центра (px):"), 0, 1);
        summaryTable.add(new Label(String.format("%.2f", last.getAvgDistancePx())), 1, 1);

        summaryTable.add(new Label("Средняя скорость (px/мс):"), 0, 2);
        summaryTable.add(new Label(String.format("%.4f", last.getAvgSpeed())), 1, 2);

        summaryTable.add(new Label("Процент попаданий (%):"), 0, 3);
        summaryTable.add(new Label(String.format("%.2f", last.getHitRate())), 1, 3);

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

        // 3) Скорость (px / ms)
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

        // 4) Нормализованное отклонение
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

    // ========================= NEURAL RESULTS =========================

    private void showNeuralResults(GameResult last) {
        int resultId = last.getId();
        long durationMs = last.getDurationMs();
        int totalScore = last.getScore();

        // Базовая статистика Neural режима
        summaryTable.add(new Label("Всего попаданий:"), 0, 0);
        summaryTable.add(new Label(String.valueOf(totalScore)), 1, 0);

        summaryTable.add(new Label("Длительность (сек):"), 0, 1);
        summaryTable.add(new Label(String.format("%.1f", durationMs / 1000.0)), 1, 1);

        double avgIntervalMs = durationMs > 0 ? (double) durationMs / totalScore : 0;
        summaryTable.add(new Label("Средний интервал (мс):"), 0, 2);
        summaryTable.add(new Label(String.format("%.2f", avgIntervalMs)), 1, 2);

        double frequency = durationMs > 0 ? (totalScore * 1000.0) / durationMs : 0;
        summaryTable.add(new Label("Частота (попаданий/сек):"), 0, 3);
        summaryTable.add(new Label(String.format("%.2f", frequency)), 1, 3);

        // График: прогрессия попаданий по времени
        XYChart.Series<Number, Number> hitsTimeSeries = new XYChart.Series<>();
        hitsTimeSeries.setName("Прогрессия попаданий");
        if (totalScore > 0) {
            for (int i = 0; i <= totalScore; i++) {
                double timeSec = (i * avgIntervalMs) / 1000.0;
                hitsTimeSeries.getData().add(new XYChart.Data<>(timeSec, i));
            }
        }
        clickIntervalsChart.getData().add(hitsTimeSeries);
        ((NumberAxis)clickIntervalsChart.getXAxis()).setLabel("Время (сек)");
        ((NumberAxis)clickIntervalsChart.getYAxis()).setLabel("Накопленные попадания");

        // Остальные графики остаются пустыми (neural не собирает детальные данные как hover)
    }

    // ========================= HOLD RESULTS =========================

    private void showHoldResults(GameResult last) {
        int resultId = last.getId();

        List<HoldAttempt> attempts = holdAttemptDao.readAttemptsForResult(resultId);
        if (attempts == null || attempts.isEmpty()) {
            summaryTable.add(new Label("Нет попыток для этого результата"), 0, 0);
            return;
        }

        int currentRadius = last.getRadius();
        int bestScore = holdAttemptDao.getMaxSuccessForUserAndRadius(last.getUserId(), currentRadius);
        long successCount = attempts.stream().filter(HoldAttempt::isSuccess).count();
        double avgHoldMs = Statistics.getAverageHoldDurationMs(attempts);
        double maxHoldMs = Statistics.getMaxHoldDurationMs(attempts);
        double avgIntervalMs = last.getAvgIntervalMs();
        double successRate = Statistics.getHoldSuccessRatePercent(attempts);

        // Summary table
        summaryTable.add(new Label("Рекорд для этого радиуса:"), 0, 0);
        summaryTable.add(new Label(String.valueOf(bestScore)), 1, 0);

        summaryTable.add(new Label("Успешных удержаний:"), 0, 1);
        summaryTable.add(new Label(String.valueOf(successCount) + " / " + attempts.size()), 1, 1);

        summaryTable.add(new Label("Процент успеха (%):"), 0, 2);
        summaryTable.add(new Label(String.format("%.2f", successRate)), 1, 2);

        summaryTable.add(new Label("Среднее время удержания (мс):"), 0, 3);
        summaryTable.add(new Label(String.format("%.2f", avgHoldMs)), 1, 3);

        summaryTable.add(new Label("Макс. время удержания (мс):"), 0, 4);
        summaryTable.add(new Label(String.format("%.2f", maxHoldMs)), 1, 4);

        summaryTable.add(new Label("Средний интервал между попытками (мс):"), 0, 5);
        summaryTable.add(new Label(String.format("%.2f", avgIntervalMs)), 1, 5);

        // 1) Время удержания по попыткам (цвет = успех/срыв)
        XYChart.Series<Number, Number> holdTimeSeries = new XYChart.Series<>();
        holdTimeSeries.setName("Время удержания (мс)");
        for (int i = 0; i < attempts.size(); i++) {
            holdTimeSeries.getData().add(new XYChart.Data<>(i + 1, attempts.get(i).getActualHoldMs()));
        }
        clickIntervalsChart.getData().add(holdTimeSeries);
        ((NumberAxis)clickIntervalsChart.getXAxis()).setLabel("Номер попытки");
        ((NumberAxis)clickIntervalsChart.getYAxis()).setLabel("Время удержания (мс)");
        Platform.runLater(() -> styleHoldChartPoints(clickIntervalsChart, attempts, "#2ecc71", "#e74c3c"));

        // 2) Интервалы между попытками (мс)
        XYChart.Series<Number, Number> intervalSeries = new XYChart.Series<>();
        intervalSeries.setName("Интервал (мс)");
        for (int i = 1; i < attempts.size(); i++) {
            long prevEnd = attempts.get(i - 1).getEndTimeNs();
            long currStart = attempts.get(i).getStartTimeNs();
            double intervalMs = (currStart - prevEnd) / 1_000_000.0;
            intervalSeries.getData().add(new XYChart.Data<>(i, intervalMs));
        }
        cursorDistanceChart.getData().add(intervalSeries);
        ((NumberAxis)cursorDistanceChart.getXAxis()).setLabel("Номер попытки");
        ((NumberAxis)cursorDistanceChart.getYAxis()).setLabel("Интервал между попытками (мс)");

        // 3) Успех / неуспех (1 / 0)
        XYChart.Series<Number, Number> successSeries = new XYChart.Series<>();
        successSeries.setName("Успех (1 = да, 0 = нет)");
        for (int i = 0; i < attempts.size(); i++) {
            int val = attempts.get(i).isSuccess() ? 1 : 0;
            successSeries.getData().add(new XYChart.Data<>(i + 1, val));
        }
        movementSpeedChart.getData().add(successSeries);
        ((NumberAxis)movementSpeedChart.getXAxis()).setLabel("Номер попытки");
        ((NumberAxis)movementSpeedChart.getYAxis()).setLabel("Успех");
        Platform.runLater(() -> styleHoldChartPoints(movementSpeedChart, attempts, "#2ecc71", "#e74c3c"));

        // 4) Накопленный процент успешных попыток
        XYChart.Series<Number, Number> cumulativeSeries = new XYChart.Series<>();
        cumulativeSeries.setName("Накопленный % успеха");
        long cumSuccess = 0;
        for (int i = 0; i < attempts.size(); i++) {
            if (attempts.get(i).isSuccess()) cumSuccess++;
            double pct = (cumSuccess * 100.0) / (i + 1);
            cumulativeSeries.getData().add(new XYChart.Data<>(i + 1, pct));
        }
        normalizedDeviationChart.getData().add(cumulativeSeries);
        ((NumberAxis)normalizedDeviationChart.getXAxis()).setLabel("Номер попытки");
        ((NumberAxis)normalizedDeviationChart.getYAxis()).setLabel("Накопленный % успеха");
    }

    private void styleHoldChartPoints(LineChart<Number, Number> chart, List<HoldAttempt> attempts, String successColor, String failColor) {
        if (chart.getData().isEmpty()) return;
        XYChart.Series<Number, Number> series = chart.getData().get(0);
        if (series.getNode() != null) {
            series.getNode().setStyle("-fx-stroke: #888888; -fx-stroke-width: 1px;");
        }
        for (int i = 0; i < series.getData().size() && i < attempts.size(); i++) {
            XYChart.Data<Number, Number> data = series.getData().get(i);
            String color = attempts.get(i).isSuccess() ? successColor : failColor;
            if (data.getNode() != null) {
                data.getNode().setStyle("-fx-background-color: " + color + ", white; -fx-background-radius: 4px;");
            }
        }
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
