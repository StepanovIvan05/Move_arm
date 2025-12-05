package com.example.move_arm.model;

import java.time.Instant;

public class GameResult {
    private int id;
    private int userId;
    private int gameTypeId;
    private int score;
    private long durationMs;
    private long timestamp; // epoch ms
    private double hitRate;
    private double avgIntervalMs;
    private double avgDistancePx;
    private double avgSpeed;

    public GameResult() { this.timestamp = Instant.now().toEpochMilli(); }

    // getters / setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getGameTypeId() { return gameTypeId; }
    public void setGameTypeId(int gameTypeId) { this.gameTypeId = gameTypeId; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public double getHitRate() { return hitRate; }
    public void setHitRate(double hitRate) { this.hitRate = hitRate; }

    public double getAvgIntervalMs() { return avgIntervalMs; }
    public void setAvgIntervalMs(double avgIntervalMs) { this.avgIntervalMs = avgIntervalMs; }

    public double getAvgDistancePx() { return avgDistancePx; }
    public void setAvgDistancePx(double avgDistancePx) { this.avgDistancePx = avgDistancePx; }

    public double getAvgSpeed() { return avgSpeed; }
    public void setAvgSpeed(double avgSpeed) { this.avgSpeed = avgSpeed; }
}
