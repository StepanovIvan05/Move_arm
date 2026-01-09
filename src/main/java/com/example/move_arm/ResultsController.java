package com.example.move_arm;

import com.example.move_arm.database.ClickDao;
import com.example.move_arm.model.ClickData;
import com.example.move_arm.model.GameResult;
import com.example.move_arm.service.GameService;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.util.List;

public class ResultsController {

    @FXML private LineChart<Number, Number> scoreChart;
    @FXML private GridPane statsGrid;

    private SceneManager sceneManager;
    private final GameService gameService = GameService.getInstance();
    private final ClickDao clickDao = new ClickDao();

    public void setSceneManager(SceneManager manager) { this.sceneManager = manager; }

    @FXML
    public void initialize() {
        AppLogger.info("ResultsController: инициализация");
        showResults();
    }

    @FXML
    public void showResults() {
        statsGrid.getChildren().clear();
        scoreChart.getData().clear();

        List<GameResult> results = gameService.getResultsForCurrentUser();
        if (results == null || results.isEmpty()) {
            statsGrid.add(new Label("Нет данных для отображения."), 0, 0);
            return;
        }

        GameResult last = results.get(0); // последний сохранённый
        int resultId = last.getId();
        System.out.println(resultId);

        // Получаем клики из БД для этого результата
        List<ClickData> clicks = clickDao.readClicksForResult(resultId);
        if (clicks == null || clicks.isEmpty()) {
            statsGrid.add(new Label("Нет кликов для последней игры."), 0, 0);
            return;
        }

        // === 1. Построим график очков по времени (в секундах) ===
        XYChart.Series<Number, Number> scoreSeries = new XYChart.Series<>();
        scoreSeries.setName("Очки во времени");

        for (int i = 0; i < clicks.size(); i++) {
            ClickData c = clicks.get(i);
            double timeSec = c.getClickTimeNs() / 1_000_000_000.0;
            int score = i + 1;
            scoreSeries.getData().add(new XYChart.Data<>(timeSec, score));
        }

        scoreChart.getData().add(scoreSeries);
        // Убедимся, что оси подписаны
        NumberAxis xAxis = (NumberAxis) scoreChart.getXAxis();
        NumberAxis yAxis = (NumberAxis) scoreChart.getYAxis();
        if (xAxis != null) xAxis.setLabel("Время (сек)");
        if (yAxis != null) yAxis.setLabel("Очки");

        // === 2. Заполним таблицу статистики из GameResult (сохранённая) ===
        statsGrid.add(new Label("Кликов:"), 0, 0);
        statsGrid.add(new Label(String.valueOf(last.getScore())), 1, 0);

        statsGrid.add(new Label("Длительность (ms):"), 0, 1);
        statsGrid.add(new Label(String.valueOf(last.getDurationMs())), 1, 1);

        statsGrid.add(new Label("Ср. интервал (ms):"), 0, 2);
        statsGrid.add(new Label(String.format("%.2f", last.getAvgIntervalMs())), 1, 2);

        statsGrid.add(new Label("Ср. расстояние (px):"), 0, 3);
        statsGrid.add(new Label(String.format("%.2f", last.getAvgDistancePx())), 1, 3);

        statsGrid.add(new Label("Ср. скорость (px/ms):"), 0, 4);
        statsGrid.add(new Label(String.format("%.4f", last.getAvgSpeed())), 1, 4);

        statsGrid.add(new Label("Попадания (%):"), 0, 5);
        statsGrid.add(new Label(String.format("%.2f", last.getHitRate())), 1, 5);
    }

    @FXML private void handleRestartButton() {
        gameService.clear();
        sceneManager.startNewGame();
    }

    @FXML private void handleToMenuButton() {
        gameService.clear();
        sceneManager.showMenu();
    }

    @FXML private void handleToMoreResultsButton() {
        sceneManager.showMoreResults();
    }
}
