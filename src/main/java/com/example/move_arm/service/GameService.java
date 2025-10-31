// service/GameService.java
package com.example.move_arm.service;

import com.example.move_arm.model.GameResult;

import java.util.ArrayList;
import java.util.List;

public class GameService {
    private static final GameService INSTANCE = new GameService();
    private final List<GameResult> results = new ArrayList<>();

    private GameService() {}

    public static GameService getInstance() {
        return INSTANCE;
    }

    public void addResult(int score, int duration, List<Long> spawnTimes) {
        results.add(new GameResult(score, duration, spawnTimes));
    }

    public List<GameResult> getAllResults() {
        return new ArrayList<>(results);
    }

    public void clear() {
        results.clear();
    }

    // Для отладки (можно удалить позже)
    public void printLastResult() {
        if (results.isEmpty()) {
            System.out.println("Нет результатов");
            return;
        }
        GameResult last = results.get(results.size() - 1);
        double avg = com.example.move_arm.model.Statistics.getAverageMs(last.getSpawnTimes());
        System.out.printf("Последняя игра: Очки=%d, Спавнов=%d, Ср. интервал=%.1f мс%n",
                last.getScore(), last.getSpawnTimes().size(), avg);
    }
}