package com.example.move_arm.service;

import java.util.LinkedList;
import java.util.Random;

import com.example.move_arm.util.TripletGeometry;
import com.example.move_arm.util.TripletGeometry.GeometryData;

/**
 * Генератор третьей цели в тройке на основе предсказаний модели CatBoost.
 * Реализует полный перебор свободных ячеек с расчётом математического ожидания TTK
 * через симуляцию трёх возможных сценариев клика (hitIndex от 0 до 2).
 */
public class NeuralTripletGenerator {
    
    private final Random random = new Random();
    private final CatBoostModelService modelService = CatBoostModelService.getInstance();
    private TripletData lastData;
    
    // История последних кликов (в миллисекундах) для расчета скользящих метрик
    private final LinkedList<Double> ttkHistory = new LinkedList<>();
    private int lastHitCellGlobal = -1;

    public static class TripletData {
        public int t1Cell, t2Cell, t3Cell;
        public long spawnNs;
        public int radius;
        public int hitIndex = -1;
        public long hitTtkNs = 0;
    }
    
    /**
     * Генерирует лучшую третью ячейку, сканируя все свободное поле и усредняя
     * предсказания модели для всех возможных сценариев клика.
     */
    /**
     * Генерирует третью ячейку, сканируя всё свободное поле и выбирая ту,
     * для которой модель предсказывает МАКСИМАЛЬНОЕ время отклика (максимальную сложность).
     */
    public int generateThirdCell(int active1, int active2, double screenW, double screenH) {
        int totalCells = 96; 

        System.out.println("-> Генератор вызван! active1=" + active1 + ", active2=" + active2);

        if (!modelService.isModelReady()) {
            System.out.println("⚠️ ВНИМАНИЕ: CatBoostModelService.isModelReady() вернул FALSE! Откат на рандом.");
            return generateRandomCell(active1, active2);
        }

        int bestCell = -1;
        // Инициализируем минимально возможным числом для поиска максимума
        double maxPredictedTtk = Double.NEGATIVE_INFINITY; 
        double minDelta = Double.POSITIVE_INFINITY; 
        double bestPredictedTtk = 0.0;

        float rollingMean = calculateHistoryMean();
        float rollingStd = calculateHistoryStd(rollingMean);
        float ttkDelta = 0.0f;
        if (!ttkHistory.isEmpty()) {
            ttkDelta = (float) (rollingMean - ttkHistory.getLast());
        }

        System.out.println(String.format("Текущая статистика сессии: rollingMean=%.2f, rollingStd=%.2f, ttkDelta=%.2f", 
                rollingMean, rollingStd, ttkDelta));

        try {
            // Полный перебор игрового поля
            for (int candidateCell = 0; candidateCell < totalCells; candidateCell++) {
                // Пропускаем уже занятые две мишени
                if (candidateCell == active1 || 
                    candidateCell == active2 || 
                    candidateCell == lastHitCellGlobal) {
                    continue;
                }

                String[] catFeatures = new String[] {
                    String.valueOf(active1), String.valueOf(active2), String.valueOf(candidateCell), String.valueOf(lastHitCellGlobal)
                };

                double sumPredictedTtk = 0.0;

                // Симуляция трех сценариев клика
                for (int simulatedHitIndex = 0; simulatedHitIndex < 3; simulatedHitIndex++) {
                    GeometryData geom = TripletGeometry.compute(active1, active2, candidateCell, simulatedHitIndex);

                    float[] numFeatures = new float[] {
                        (float) geom.centroidRow, (float) geom.centroidCol,
                        (float) geom.t1Angle, (float) geom.t2Angle, (float) geom.t3Angle,
                        (float) geom.hitToMiss1Dist, (float) geom.hitToMiss2Dist, (float) geom.miss1ToMiss2Dist,
                        (float) geom.spread, (float) geom.distanceFromCenter, (float) geom.angleVariance,
                        rollingMean, rollingStd, ttkDelta
                    };

                    sumPredictedTtk += modelService.predict(catFeatures, numFeatures);
                }

                // Математическое ожидание времени отклика на данную тройку мишеней
                double averagePredictedTtk = sumPredictedTtk / 3.0; // Добавляем небольшой буфер для смещения в сторону более сложных паттернов

                // Изменяем условие: ищем строго НАИБОЛЬШЕЕ предсказанное время (самый сложный паттерн)
                double currentDelta = Math.abs(averagePredictedTtk - rollingMean);

                // Ищем ячейку с МИНИМАЛЬНЫМ отклонением от rollingMean
                if (currentDelta < minDelta) {
                    minDelta = currentDelta;
                    bestCell = candidateCell;
                    bestPredictedTtk = averagePredictedTtk;
                }
            }
            
            System.out.println("✅ Модель успешно применилась! Выбрана самая сложная ячейка: " + bestCell 
                    + " с ожидаемым TTK: " + String.format("%.2f", bestPredictedTtk) + " мс");

        } catch (Exception e) {
            System.out.println("❌ КРИТИЧЕСКАЯ ОШИБКА ПРИ ПРЕДСКАЗАНИИ МОДЕЛИ:");
            e.printStackTrace();
        }

        // Если перебор сломался или не нашел кандидатов, страхуемся рандомом
        if (bestCell == -1) {
            System.out.println("⚠️ Перебор завершился с ошибкой. Откат на рандом.");
            bestCell = generateRandomCell(active1, active2);
        }

        // КРИТИЧЕСКИЙ ФИКС: Инициализируем lastData, чтобы метод onHit() не падал по 'if (lastData == null) return;'
        this.lastData = new TripletData();
        this.lastData.t1Cell = active1;
        this.lastData.t2Cell = active2;
        this.lastData.t3Cell = bestCell;
        this.lastData.spawnNs = System.nanoTime(); // Фиксируем время создания тройки

        return bestCell;
    }
    
    private int generateRandomCell(int active1, int active2) {
        int cell;
        do {
            cell = random.nextInt(96);
        } while (cell == active1 || cell == active2);
        return cell;
    }

    /**
     * Вызывается из Presenter при успешном клике по мишени.
     */
    public void onHit(int hitCell, long lifetimeNs) {
        if (lastData == null) return;
        
        lastData.hitTtkNs = lifetimeNs;
        lastHitCellGlobal = hitCell;
        
        // Переводим наносекунды в миллисекунды для скользящей истории кликов
        double currentTtkMs = lifetimeNs / 1_000_000.0;
        ttkHistory.addLast(currentTtkMs);
        if (ttkHistory.size() > 5) {
            ttkHistory.removeFirst(); // Держим окно ровно в 5 последних кликов
        }
        
        if (hitCell == lastData.t1Cell) lastData.hitIndex = 0;
        else if (hitCell == lastData.t2Cell) lastData.hitIndex = 1;
        else if (hitCell == lastData.t3Cell) lastData.hitIndex = 2;
    }
    
    /**
     * Сброс состояния при перезапуске игры (вызывается из Presenter).
     */
    public void reset() {
        lastData = null;
        ttkHistory.clear();
        lastHitCellGlobal = -1;
    }

    // --- Математический блок вычисления статистических фич ---

    private float calculateHistoryMean() {
        if (ttkHistory.isEmpty()) return 450.0f; // Начальная точка адаптации
        double sum = 0;
        for (double val : ttkHistory) sum += val;
        return (float) (sum / ttkHistory.size());
    }

    private float calculateHistoryStd(float mean) {
        if (ttkHistory.size() <= 1) return 0.0f;
        double sumSquares = 0;
        for (double val : ttkHistory) {
            sumSquares += Math.pow(val - mean, 2);
        }
        return (float) Math.sqrt(sumSquares / ttkHistory.size());
    }

    public TripletData getLastData() { 
        return lastData; 
    }
}