package com.example.move_arm.service;

import java.util.Random;

/**
 * Генератор третьей цели в тройке.
 * Пока: рандом или взвешенный рандом для сбора данных.
 * Потом: нейросеть с RL.
 */
public class NeuralTripletGenerator {
    
    private final Random random = new Random();
    private TripletData lastData;
    
    public static class TripletData {
        public int t1Cell, t2Cell, t3Cell;
        public long spawnNs;
        public int radius;
        public int hitIndex = -1;
        public long hitTtkNs = 0;
    }
    
    /**
     * Генерирует третью цель по двум активным.
     * Пока: чистый рандом для максимального покрытия паттернов.
     */
    public int generateThirdCell(int active1, int active2, double screenW, double screenH) {
        int newCell;
        do {
            newCell = random.nextInt(96);
        } while (newCell == active1 || newCell == active2);
        
        lastData = new TripletData();
        lastData.t1Cell = active1;
        lastData.t2Cell = active2;
        lastData.t3Cell = newCell;
        lastData.spawnNs = System.nanoTime();
        lastData.radius = 40; // TODO: из настроек
        
        return newCell;
    }
    
    /**
     * Фиксируем результат попадания
     */
    public void onHit(int hitCell, long lifetimeNs) {
        if (lastData == null) return;
        
        lastData.hitTtkNs = lifetimeNs;
        
        if (hitCell == lastData.t1Cell) lastData.hitIndex = 0;
        else if (hitCell == lastData.t2Cell) lastData.hitIndex = 1;
        else if (hitCell == lastData.t3Cell) lastData.hitIndex = 2;
    }
    
    public TripletData getLastData() {
        return lastData;
    }
    
    public void reset() {
        lastData = null;
    }
    
    // ==================== RL заглушки ====================
    
    public void updatePolicy(int hitCell, long lifetimeNs) {
        // TODO: мини-апдейт во время игры
    }
    
    public void trainOnEpisode(java.util.List<?> episode) {
        // TODO: обучение после игры
    }
}