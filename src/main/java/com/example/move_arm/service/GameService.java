// service/GameService.java
package com.example.move_arm.service;

import java.util.ArrayList;
import java.util.List;

import com.example.move_arm.model.ClickData;

public class GameService {

    private static final GameService INSTANCE = new GameService();
    private final List<ClickData> allClicks = new ArrayList<>();

    private GameService() {}

    public static GameService getInstance() {
        return INSTANCE;
    }

    /**
     * Добавить клики из одной игры
     */
    public void addGameClicks(List<ClickData> clicks) {
        if (clicks != null && !clicks.isEmpty()) {
            allClicks.addAll(clicks);
        }
    }

    /**
     * Получить все клики из всех игр
     */
    public List<ClickData> getAllClicks() {
        return new ArrayList<>(allClicks);
    }

    /**
     * Получить клики только из последней игры
     */
    public List<ClickData> getLastGameClicks() {
        if (allClicks.isEmpty()) return new ArrayList<>();

        // Находим начало последней игры: последний клик
        // (можно улучшить с помощью разделителей, но пока просто всё)
        return new ArrayList<>(allClicks);
    }

    /**
     * Очистить всю статистику
     */
    public void clear() {
        allClicks.clear();
    }

    /**
     * Отладка: вывести статистику по последней игре
     */
    public void printLastGameSummary() {
        List<ClickData> lastGame = getLastGameClicks();
        if (lastGame.isEmpty()) {
            System.out.println("Нет данных для анализа");
            return;
        }

        String summary = com.example.move_arm.model.Statistics.getSummary(lastGame);
        System.out.println("СТАТИСТИКА ПОСЛЕДНЕЙ ИГРЫ:");
        System.out.println(summary);
    }
}