package com.example.move_arm.model.settings;

public abstract class BaseSettings {
    private int durationSeconds = 60;
    private int maxCirclesCount = 3;
    private int radius = 50;
    private int seed = 0;

    public BaseSettings(){}

    public int getRadius() { return radius; }
    public void setRadius(int radius) { this.radius = radius; }

    public int getDurationSeconds() {return this.durationSeconds;}
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }

    public int getSeed(){return this.seed;}
    public void setSeed(int seed) {this.seed = seed;}

    public int getMaxCirclesCount() { return maxCirclesCount; }
    public void setMaxCirclesCount(int maxCirclesCount) { this.maxCirclesCount = maxCirclesCount; }



}
