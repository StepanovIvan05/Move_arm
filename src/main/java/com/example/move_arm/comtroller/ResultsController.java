package com.example.move_arm.comtroller;

import java.util.List;

import com.example.move_arm.database.ClickDao;
import com.example.move_arm.database.HoldAttemptDao;
import com.example.move_arm.model.ClickData;
import com.example.move_arm.model.GameResult;
import com.example.move_arm.model.HoldAttempt;
import com.example.move_arm.service.GameService; // Не забудь импорт
import com.example.move_arm.ui.SceneManager;
import com.example.move_arm.util.AppLogger;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

public class ResultsController {

    @FXML private LineChart<Number, Number> scoreChart;
    @FXML private GridPane statsGrid;
    @FXML private HBox legendBox;

    private SceneManager sceneManager;
    private final GameService gameService = GameService.getInstance();
    private final ClickDao clickDao = new ClickDao();
    private final HoldAttemptDao holdAttemptDao = new HoldAttemptDao(); // Добавлено

    public void setSceneManager(SceneManager manager) { this.sceneManager = manager; }
    private boolean isNeuralMode = false;

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

        GameResult last = results.get(0);

        // ПРОВЕРКА РЕЖИМА: 
        // Здесь мы решаем, какой метод отрисовки вызвать
        if ("hold".equals(gameService.getCurrentGameTypeString())) {
            System.out.println("нашло ===============");
            showHoldResults(last);
        } else {
            showClickResults(last);
        }
    }

    // --- МЕТОД ДЛЯ ОБЫЧНОГО РЕЖИМА (КЛИКИ) ---
    private void showClickResults(GameResult last) {
        List<ClickData> clicks = clickDao.readClicksForResult(last.getId());
        useAutoLegend();
        if (clicks == null || clicks.isEmpty()) {
            statsGrid.add(new Label("Нет кликов для последней игры."), 0, 0);
            return;
        }

        XYChart.Series<Number, Number> scoreSeries = new XYChart.Series<>();
        scoreSeries.setName("Очки во времени");

        for (int i = 0; i < clicks.size(); i++) {
            ClickData c = clicks.get(i);
            double timeSec = c.getClickTimeNs() / 1_000_000_000.0;
            scoreSeries.getData().add(new XYChart.Data<>(timeSec, i + 1));
        }

        scoreChart.getData().add(scoreSeries);
        setupAxes("Время (сек)", "Очки");
        fillStatsTable(last);
    }

    private void showHoldResults(GameResult last) {
        List<HoldAttempt> attempts = holdAttemptDao.readAttemptsForResult(last.getId());
        if (attempts == null || attempts.isEmpty()) return;

        useManualLegend();
        scoreChart.getData().clear();

        XYChart.Series<Number, Number> currentSeries = null;
        Boolean lastStatus = null;
        
        // Флаги, чтобы добавить в легенду только по одному разу
        boolean addedSuccessLegend = false;
        boolean addedFailLegend = false;

        for (int i = 0; i < attempts.size(); i++) {
            HoldAttempt a = attempts.get(i);
            
            // Если статус изменился — создаем новый сегмент линии
            if (lastStatus == null || a.isSuccess() != lastStatus) {
                currentSeries = new XYChart.Series<>();
                
                // Логика легенды: называем серию только ПЕРВЫЙ раз, когда она встречается
                if (a.isSuccess() && !addedSuccessLegend) {
                    currentSeries.setName("Успех");
                    addedSuccessLegend = true;
                } else if (!a.isSuccess() && !addedFailLegend) {
                    currentSeries.setName("Срыв");
                    addedFailLegend = true;
                } else {
                    // Пустое имя убирает серию из легенды в большинстве случаев
                    currentSeries.setName(null); 
                }

                scoreChart.getData().add(currentSeries);
            }

            currentSeries.getData().add(new XYChart.Data<>(i + 1, a.getActualHoldMs()));
            
            // Прячем узел в легенде, если это "дублирующая" серия
            // (делается чуть позже, так как узел создается не сразу)
            
            lastStatus = a.isSuccess();
        }

        setupAxes("Попытка №", "Время (ms)");
        fillStatsTable(last);
        
        // Вызываем покраску (теперь она необходима, чтобы все сегменты 
        // одного типа были одного цвета)
        applyDynamicStyles(attempts);
    }

    private void applyDynamicStyles(List<HoldAttempt> attempts) {
        javafx.application.Platform.runLater(() -> {
            int seriesIndex = 0;
            Boolean lastStatus = null;
            
            // Нам нужно отследить, какие серии мы уже "показали" в легенде
            boolean successLegendDone = false;
            boolean failLegendDone = false;

            for (int i = 0; i < attempts.size(); i++) {
                HoldAttempt a = attempts.get(i);
                
                if (lastStatus == null || a.isSuccess() != lastStatus) {
                    if (seriesIndex >= scoreChart.getData().size()) break;
                    
                    XYChart.Series<Number, Number> s = scoreChart.getData().get(seriesIndex);
                    String color = a.isSuccess() ? "#2ecc71" : "#e74c3c"; 
                    
                    // Красим линию и точки
                    if (s.getNode() != null) {
                        s.getNode().setStyle("-fx-stroke: " + color + "; -fx-stroke-width: 2px;");
                    }
                    for (XYChart.Data<Number, Number> data : s.getData()) {
                        if (data.getNode() != null) {
                            data.getNode().setStyle("-fx-background-color: " + color + ", white;");
                        }
                    }

                    seriesIndex++;
                }
                lastStatus = a.isSuccess();
            }
        });
    }

    // Вспомогательный метод для подписи осей
    private void setupAxes(String xLabel, String yLabel) {
        NumberAxis xAxis = (NumberAxis) scoreChart.getXAxis();
        NumberAxis yAxis = (NumberAxis) scoreChart.getYAxis();
        if (xAxis != null) xAxis.setLabel(xLabel);
        if (yAxis != null) yAxis.setLabel(yLabel);
    }

    // Универсальный метод для заполнения таблицы (формат прежний)
    private void fillStatsTable(GameResult last) {
        addStatRow("Результат (Очки/Успехи):", String.valueOf(last.getScore()), 0);
        addStatRow("Длительность (ms):", String.valueOf(last.getDurationMs()), 1);
        addStatRow("Ср. интервал (ms):", String.format("%.2f", last.getAvgIntervalMs()), 2);
        addStatRow("Ср. расстояние (px):", String.format("%.2f", last.getAvgDistancePx()), 3);
        addStatRow("Ср. скорость (px/ms):", String.format("%.4f", last.getAvgSpeed()), 4);
        addStatRow("Точность (%):", String.format("%.2f", last.getHitRate()), 5);
    }

    private void addStatRow(String labelText, String valueText, int row) {
        statsGrid.add(new Label(labelText), 0, row);
        statsGrid.add(new Label(valueText), 1, row);
    }

    @FXML
    private void handleRestartButton() {
        gameService.clear();

        String currentType = gameService.getCurrentGameTypeString();

        if ("neural".equalsIgnoreCase(currentType)) {
            AppLogger.info("ResultsController: Рестарт Neural режима");
            sceneManager.startNeuralGame();
        } 
        else if ("hold".equals(currentType)) {
            sceneManager.showHoldGame();
        } 
        else {
            // обычный hover
            sceneManager.startNewGame();
        }
    }

    @FXML private void handleToMenuButton() {
        gameService.clear();
        sceneManager.showMenu();
    }

    @FXML private void handleToMoreResultsButton() {
        sceneManager.showMoreResults();
    }
    private void useManualLegend() {
        scoreChart.setLegendVisible(false);

        if (legendBox != null) {
            legendBox.getChildren().clear();
            setupLegend();
        }
    }

    private void useAutoLegend() {
        scoreChart.setLegendVisible(true);

        if (legendBox != null) {
            legendBox.getChildren().clear();
        }
    }
    private Node createLegendItem(String text, String color) {

        Region colorCircle = new Region();
        colorCircle.setPrefSize(12, 12);
        colorCircle.setStyle(
                "-fx-background-color: " + color + ";" +
                "-fx-background-radius: 6px;"
        );

        Label label = new Label(text);

        HBox item = new HBox(6, colorCircle, label);
        item.setAlignment(Pos.CENTER_LEFT);

        return item;
    }
    private void setupLegend() {

        legendBox.getChildren().clear();

        legendBox.getChildren().addAll(
                createLegendItem("Успех", "#2ecc71"),
                createLegendItem("Срыв", "#e74c3c")
        );
    }
    public void setIsNeuralMode(boolean neural) {
        this.isNeuralMode = neural;
        AppLogger.info("ResultsController: Neural mode = " + neural);
    }
}