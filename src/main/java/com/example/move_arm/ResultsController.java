package com.example.move_arm;

import com.example.move_arm.model.ClickData;
import com.example.move_arm.service.GameService;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.awt.*;
import java.util.List;

public class ResultsController {

    @FXML
    private LineChart<Number, Number> scoreChart;

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
    private void showResults(){
        AppLogger.info("ResultsController: Отображение графика результатов");

        List<ClickData> lastGame = gameService.getLastGameClicks();
        if (lastGame == null || lastGame.isEmpty()) {
            AppLogger.info("Нет данных для отображения");
            return;
        }

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Рост очков по времени");

        long startTime = lastGame.get(0).getClickTimeNs(); // начало первой сессии
        for (int i = 0; i < lastGame.size(); i++) {
            ClickData click = lastGame.get(i);

            // преобразуем в секунды относительно начала
            double timeSec = (click.getClickTimeNs() - startTime) / 1_000_000_000.0;
            int score = i + 1;

            series.getData().add(new XYChart.Data<>(timeSec, score));
        }

        scoreChart.getData().clear();
        scoreChart.getData().add(series);
    }

    @FXML
    private void handleRestartButton() {
        AppLogger.info("ResultsController: Нажата кнопка 'Рестарт'");

        try {
            sceneManager.clearCache();
            sceneManager.showGame();
            AppLogger.info("ResultsController: Успешный переход к игровой сцене");

        } catch (Exception e) {
            AppLogger.error("ResultsController: Неожиданная ошибка при переходе к игре", e);
            throw e;
        }
    }

    @FXML
    private void handleToMenuButton() {
        AppLogger.info("ResultsController: Нажата кнопка 'В меню'");

        try {
            sceneManager.clearCache();
            sceneManager.showStart();
            AppLogger.info("ResultsController: Успешный переход в меню");

        } catch (Exception e) {
            AppLogger.error("ResultsController: Неожиданная ошибка при переходе в меню", e);
            throw e;
        }
    }

    @FXML
    private void handleToMoreResultsButton() {
        AppLogger.info("ResultsController: Нажата кнопка 'Больше статистики'");

        try {
            sceneManager.showMoreResults();
            AppLogger.info("ResultsController: Успешный переход в более подробную статистику");

        } catch (Exception e) {
            AppLogger.error("ResultsController: Неожиданная ошибка при переходе в более подробную статистику", e);
            throw e;
        }
    }
}
