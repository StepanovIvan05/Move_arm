package com.example.move_arm.model;

/**
 * DTO для записи тройки целей в БД.
 * Используется NeuralGamePresenter → GameService → NeuralTripletService.
 */
public class TripletRecord {
    
    public int userId;
    public long timestamp;
    public int tripletIndex;
    public long spawnNs;
    public int t1Cell, t2Cell, t3Cell;
    public int hitTargetIndex;
    public long hitTtkNs;
    public int radius;
    public double centroidRow, centroidCol;
    public double t1Angle, t2Angle, t3Angle;
    public double hitToMiss1Dist, hitToMiss2Dist, miss1ToMiss2Dist;
    public double spread;
    public int screenWidth, screenHeight;
    public int previousHitCell = -1;
    
    public TripletRecord() {}
    
    // Можно добавить конструктор со всеми полями если нужно
}