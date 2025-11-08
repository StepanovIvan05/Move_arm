package com.example.move_arm;

import com.example.move_arm.model.ClickData;
import com.example.move_arm.model.Statistics;
import com.example.move_arm.service.GameService;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResultsController {

    @FXML
    private LineChart<Number, Number> scoreChart;

    @FXML
    private GridPane statsGrid;

    private SceneManager sceneManager;
    private final GameService gameService = GameService.getInstance();

    public void setSceneManager(SceneManager manager) {
        this.sceneManager = manager;
    }

    @FXML
    public void initialize() {
        AppLogger.info("ResultsController: Контроллер инициализирован");
        showResults();
    }

    @FXML
    private void showResults() {
        AppLogger.info("ResultsController: Отображение графика результатов");

        List<ClickData> lastGame = gameService.getLastGameClicks();

        if (lastGame == null || lastGame.isEmpty()) {
            statsGrid.add(new Label("Нет данных для отображения."), 0, 0);
            return;
        }

        // === 1. Строим график ===
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Рост очков по времени");

        long startTime = lastGame.getFirst().getClickTimeNs();
        for (int i = 0; i < lastGame.size(); i++) {
            ClickData click = lastGame.get(i);
            double timeSec = (click.getClickTimeNs() - startTime) / 1_000_000_000.0;
            int score = i + 1;
            series.getData().add(new XYChart.Data<>(timeSec, score));
        }

        scoreChart.getData().clear();
        scoreChart.getData().add(series);

        // === 2. Получаем статистику ===
        double hitRate = Statistics.getHitRatePercent(lastGame);
        String summary = Statistics.getSummary(lastGame);

        // Преобразуем summary в пары ключ-значение
        Map<String, String> metrics = parseSummary(summary);
        metrics.put("Попадания", String.format("%.1f %%", hitRate));

        // === 3. Заполняем таблицу ===
        statsGrid.getChildren().clear();
        int row = 0;
        for (Map.Entry<String, String> entry : metrics.entrySet()) {
            Label key = new Label(entry.getKey() + ":");
            key.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 16px; -fx-font-weight: bold;");

            Label value = new Label(entry.getValue());
            value.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");

            statsGrid.add(key, 0, row);
            statsGrid.add(value, 1, row);
            row++;
        }
    }

    private Map<String, String> parseSummary(String summary) {
        Map<String, String> map = new LinkedHashMap<>();
        String[] parts = summary.split("\\|");
        for (String p : parts) {
            String trimmed = p.trim();
            if (trimmed.isEmpty()) continue;

            int colonIndex = trimmed.indexOf(':');
            if (colonIndex > 0) {
                String key = trimmed.substring(0, colonIndex).trim();
                String value = trimmed.substring(colonIndex + 1).trim();
                map.put(key, value);
            }
        }
        return map;
    }

    // Остальные методы без изменений ↓
    @FXML
    private void handleRestartButton() {
        sceneManager.clearCache();
        sceneManager.showGame();
    }

    @FXML
    private void handleToMenuButton() {
        sceneManager.clearCache();
        sceneManager.showStart();
    }

    @FXML
    private void handleToMoreResultsButton() {
        sceneManager.showMoreResults();
    }
}
