package com.example.move_arm;

import com.example.move_arm.model.ClickData;
import com.example.move_arm.service.GameService;
import com.example.move_arm.model.Statistics;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

import java.util.List;

public class MoreResultsController {
    private SceneManager sceneManager;
    private final GameService gameService = GameService.getInstance();

    @FXML private LineChart<Number, Number> clickIntervalsChart;
    @FXML private LineChart<Number, Number> cursorDistanceChart;
    @FXML private LineChart<Number, Number> movementSpeedChart;
    @FXML private LineChart<Number, Number> normalizedDeviationChart;
    @FXML private GridPane summaryTable;

    public void setSceneManager(SceneManager manager) {
        this.sceneManager = manager;
    }

    @FXML
    private void initialize() {
        AppLogger.info("MoreResultsController: Инициализация");
        showMoreResults();
    }

    @FXML
    private void showMoreResults() {
        AppLogger.info("MoreResultsController: Отображение графиков");

        List<ClickData> lastGame = gameService.getLastGameClicks();
        if (lastGame == null || lastGame.isEmpty()) {
            AppLogger.info("Нет данных для отображения");
            return;
        }

        // === Получаем статистику ===
        List<Double> clickIntervalsMs = Statistics.getClickIntervalsMs(lastGame);
        double averageClickIntervalMs = Statistics.getAverageClickIntervalMs(lastGame);
        List<Double> cursorDistances = Statistics.getCursorDistances(lastGame);
        double averageCursorDistance = Statistics.getAverageCursorDistance(lastGame);
        List<Double> movementSpeeds = Statistics.getMovementSpeeds(lastGame);
        double averageSpeedPxPerMs = Statistics.getAverageSpeedPxPerMs(lastGame);
        double maxSpeedPxPerMs = Statistics.getMaxSpeedPxPerMs(lastGame);
        List<Double> normalizedDeviations = Statistics.getNormalizedDeviations(lastGame);
        double averageNormalizedDeviation = Statistics.getAverageNormalizedDeviation(lastGame);
        double hitRatePercent = Statistics.getHitRatePercent(lastGame);

        // === Таблица средних значений ===
        summaryTable.addRow(0, createWhiteLabel("Средний интервал кликов (мс):"), valueLabel(averageClickIntervalMs));
        summaryTable.addRow(1, createWhiteLabel("Среднее расстояние до центра (px):"), valueLabel(averageCursorDistance));
        summaryTable.addRow(2, createWhiteLabel("Средняя скорость (px/мс):"), valueLabel(averageSpeedPxPerMs));
        summaryTable.addRow(3, createWhiteLabel("Макс. скорость (px/мс):"), valueLabel(maxSpeedPxPerMs));
        summaryTable.addRow(4, createWhiteLabel("Среднее нормализ. отклонение:"), valueLabel(averageNormalizedDeviation));
        summaryTable.addRow(5, createWhiteLabel("Процент попаданий (%):"), valueLabel(hitRatePercent));


        // === Графики ===
        fillChart(clickIntervalsChart, clickIntervalsMs, "Интервалы между кликами");
        fillChart(cursorDistanceChart, cursorDistances, "Расстояние от центра");
        fillChart(movementSpeedChart, movementSpeeds, "Скорость движения");
        fillChart(normalizedDeviationChart, normalizedDeviations, "Нормализованное отклонение");
    }

    private Label valueLabel(double value) {
        Label label = new Label(String.format("%.2f", value));
        label.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        return label;
    }

    private void fillChart(LineChart<Number, Number> chart, List<Double> data, String name) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(name);
        for (int i = 0; i < data.size(); i++) {
            series.getData().add(new XYChart.Data<>(i + 1, data.get(i)));
        }
        chart.getData().setAll(series);
    }

    @FXML
    private void handleToMenuButton() {
        AppLogger.info("MoreResultsController: Нажата кнопка 'В меню'");
        try {
            sceneManager.clearCache();
            sceneManager.showStart();
        } catch (Exception e) {
            AppLogger.error("MoreResultsController: Ошибка при переходе в меню", e);
            throw e;
        }
    }

    private Label createWhiteLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.WHITE); // белый текст
        return label;
    }
}
