package com.example.move_arm.model.settings;

// @Entity
// @Table(name = "hover_settings")
public class HoverGameSettings extends BaseSettings {

    // Поля специфичные ТОЛЬКО для этой игры
    private int durationSeconds = 30;
    private int maxCirclesCount = 3;
    private double minRadius = 20.0;
    private double maxRadius = 50.0;
    private double radius = 35.0;

    // Пустой конструктор обязателен для ORM
    public HoverGameSettings() {}

    public double getRadius() {return radius;}
    public void setRadius(double radius) {this.radius = radius;}

    // Геттеры и Сеттеры (ORM использует их для маппинга)
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }

    public int getMaxCirclesCount() { return maxCirclesCount; }
    public void setMaxCirclesCount(int maxCirclesCount) { this.maxCirclesCount = maxCirclesCount; }

    public double getMinRadius() { return minRadius; }
    public void setMinRadius(double minRadius) { this.minRadius = minRadius; }

    public double getMaxRadius() { return maxRadius; }
    public void setMaxRadius(double maxRadius) { this.maxRadius = maxRadius; }
}