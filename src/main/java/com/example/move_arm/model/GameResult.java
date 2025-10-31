// model/GameResult.java
package com.example.move_arm.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GameResult {
    private final int score;
    private final int duration;
    private final LocalDateTime timestamp;
    private final List<Long> spawnTimes;

    public GameResult(int score, int duration, List<Long> spawnTimes) {
        this.score = score;
        this.duration = duration;
        this.timestamp = LocalDateTime.now();
        this.spawnTimes = new ArrayList<>(spawnTimes);
    }

    // Геттеры
    public int getScore() { return score; }
    public int getDuration() { return duration; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<Long> getSpawnTimes() { return new ArrayList<>(spawnTimes); }
}