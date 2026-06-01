package com.example.move_arm.service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.example.move_arm.util.TripletGeometry;
import com.example.move_arm.util.TripletGeometry.GeometryData;

/**
 * Вероятностный адаптивный генератор мишеней под CatBoost.
 * Переводит предсказанное время в вероятности. Исключает спавн в одной точке
 * и динамически распределяет цели по всему экрану на основе "колеса рулетки".
 */
public class NeuralTripletGenerator {
    
    private final Random random = new Random();
    private final CatBoostModelService modelService = CatBoostModelService.getInstance();
    
    private final LinkedList<Double> ttkHistory = new LinkedList<>();
    private final LinkedList<Double> distanceHistory = new LinkedList<>();
    
    private int lastHitCellGlobal = -1;
    private double lastAngleGlobal = 0.0;
    
    private final int GRID_WIDTH = 12;
    private final float FIXED_RADIUS = 40.0f;

    private TripletData lastData;

    public static class TripletData {
        public int t1Cell, t2Cell, t3Cell;
        public long spawnNs;
        public int radius;
        public int hitIndex = -1;
        public long hitTtkNs = 0;
    }

    // Вспомогательный класс для хранения весов кандидатов
    private static class CellCandidate {
        int cellIndex;
        double weight;
        double predictedTtk;

        CellCandidate(int cellIndex, double weight, double predictedTtk) {
            this.cellIndex = cellIndex;
            this.weight = weight;
            this.predictedTtk = predictedTtk;
        }
    }

    /**
     * Генерирует третью ячейку, используя вероятностное распределение на основе предсказаний модели.
     */
    public int generateThirdCell(int active1, int active2, double screenW, double screenH) {
        int totalCells = 96; 

        System.out.println("-> [PROBABILISTIC MODE] ИИ-Генератор вызван!");

        if (!modelService.isModelReady()) {
            System.out.println("⚠️ Модель не загружена. Откат на рандом.");
            return generateRandomCell(active1, active2);
        }

        // Фичи микро-тренда
        float rollingMean = calculateHistoryMean();
        float rollingStd = calculateHistoryStd(rollingMean);
        float ttkDelta = 0.0f;
        if (ttkHistory.size() >= 2) {
            ttkDelta = (float) (ttkHistory.get(ttkHistory.size() - 1) - ttkHistory.get(ttkHistory.size() - 2));
        }
        float prevVelocity = 0.01f; 
        if (!distanceHistory.isEmpty() && !ttkHistory.isEmpty()) {
            prevVelocity = (float) (distanceHistory.getLast() / ttkHistory.getLast());
        }

        int prevX = lastHitCellGlobal % GRID_WIDTH;
        int prevY = lastHitCellGlobal / GRID_WIDTH;

        List<CellCandidate> candidates = new ArrayList<>();
        double totalWeight = 0.0;

        try {
            // Шаг 1: Собираем предсказания для всех доступных ячеек
            for (int candidateCell = 0; candidateCell < totalCells; candidateCell++) {
                if (candidateCell == active1 || candidateCell == active2 || candidateCell == lastHitCellGlobal) {
                    continue;
                }

                String[] catFeatures = new String[] {
                    String.valueOf(active1), 
                    String.valueOf(active2), 
                    String.valueOf(candidateCell), 
                    String.valueOf(lastHitCellGlobal)
                };

                double sumTtk = 0.0;
                boolean hasAnomaly = false;

                // Просчитываем 3 сценария клика
                for (int simulatedHitIndex = 0; simulatedHitIndex < 3; simulatedHitIndex++) {
                    int targetCell = (simulatedHitIndex == 0) ? active1 : (simulatedHitIndex == 1) ? active2 : candidateCell;
                    
                    int tX = targetCell % GRID_WIDTH;
                    int tY = targetCell / GRID_WIDTH;

                    double distance = Math.hypot(tX - prevX, tY - prevY);
                    double angle = Math.atan2(tY - prevY, tX - prevX);
                    float angleDelta = (float) Math.abs(angle - lastAngleGlobal);

                    if (distance < 1.0) {
                        hasAnomaly = true;
                        break;
                    }

                    GeometryData geom = TripletGeometry.compute(active1, active2, candidateCell, simulatedHitIndex);

                    float[] numFeatures = new float[] {
                        (float) distance, (float) angle, (float) geom.centroidRow, (float) geom.centroidCol,
                        (float) geom.distanceFromCenter, (float) geom.angleVariance, (float) geom.spread,
                        (float) geom.hitToMiss1Dist, (float) geom.hitToMiss2Dist, (float) geom.miss1ToMiss2Dist,
                        FIXED_RADIUS, prevVelocity, angleDelta, rollingMean, rollingStd, ttkDelta
                    };

                    double predictedTtk = modelService.predict(catFeatures, numFeatures);
                    
                    // Отрезаем дикие галлюцинации модели
                    if (predictedTtk > 1500.0) predictedTtk = 600.0;

                    sumTtk += predictedTtk;
                }

                if (hasAnomaly) continue;

                double averageTtk = sumTtk / 3.0;

                // Вычисляем физическое расстояние от курсора до проверяемого кандидата
                int candX = candidateCell % GRID_WIDTH;
                int candY = candidateCell / GRID_WIDTH;
                double distFromCursor = Math.hypot(candX - prevX, candY - prevY);

                // МЯГКИЙ ШТРАФ ЗА БЛИЗОСТЬ (Математический барьер):
                // Если ячейка ближе чем на 3 шага, мы экспоненциально уменьшаем её базовый ТТК для расчета весов.
                if (lastHitCellGlobal != -1 && distFromCursor < 3.0) {
                    averageTtk *= (distFromCursor / 3.0); 
                }

                // Превращаем ТТК в вес. Используем смещение (-300), чтобы увеличить контраст между сложными и легкими целями
                double weight = Math.exp((averageTtk - 300.0) / 100.0);
                if (weight < 0.01) weight = 0.01; // минимальный вес, чтобы шанс не упал в ноль

                candidates.add(new CellCandidate(candidateCell, weight, averageTtk));
                totalWeight += weight;
            }

            // Шаг 2: Выбор случайной ячейки методом "Колеса Рулетки"
            if (!candidates.isEmpty()) {
                double targetRoll = random.nextDouble() * totalWeight;
                double currentSum = 0.0;

                for (CellCandidate candidate : candidates) {
                    currentSum += candidate.weight;
                    if (currentSum >= targetRoll) {
                        int bestCell = candidate.cellIndex;
                        System.out.println("🎲 [Рулетка] Выбрана ячейка: " + bestCell 
                                + " (Предсказанный ТТК: " + String.format("%.2f", candidate.predictedTtk) + " мс, Вес: " + String.format("%.2f", candidate.weight) + ")");
                        
                        // Запись метаданных
                        recordMetadata(active1, active2, bestCell);
                        return bestCell;
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("❌ Ошибка вероятностного расчета. Накатываем рандом.");
            e.printStackTrace();
        }

        int fallbackCell = generateRandomCell(active1, active2);
        recordMetadata(active1, active2, fallbackCell);
        return fallbackCell;
    }

    private void recordMetadata(int active1, int active2, int finalCell) {
        this.lastData = new TripletData();
        this.lastData.t1Cell = active1;
        this.lastData.t2Cell = active2;
        this.lastData.t3Cell = finalCell;
        this.lastData.spawnNs = System.nanoTime();
    }
    
    public void onHit(int hitCell, long lifetimeNs) {
        double currentTtkMs = lifetimeNs / 1_000_000.0;
        if (currentTtkMs > 3000.0) currentTtkMs = 3000.0;

        if (lastHitCellGlobal != -1) {
            int prevX = lastHitCellGlobal % GRID_WIDTH;
            int prevY = lastHitCellGlobal / GRID_WIDTH;
            int currX = hitCell % GRID_WIDTH;
            int currY = hitCell / GRID_WIDTH;
            double actualDistance = Math.hypot(currX - prevX, currY - prevY);
            distanceHistory.addLast(actualDistance);
            lastAngleGlobal = Math.atan2(currY - prevY, currX - prevX);
        } else {
            distanceHistory.addLast(0.0);
            lastAngleGlobal = 0.0;
        }

        ttkHistory.addLast(currentTtkMs);
        if (ttkHistory.size() > 5) ttkHistory.removeFirst();
        if (distanceHistory.size() > 5) distanceHistory.removeFirst();
        
        if (lastData != null) {
            lastData.hitTtkNs = lifetimeNs;
            if (hitCell == lastData.t1Cell) lastData.hitIndex = 0;
            else if (hitCell == lastData.t2Cell) lastData.hitIndex = 1;
            else if (hitCell == lastData.t3Cell) lastData.hitIndex = 2;
        }

        lastHitCellGlobal = hitCell;
    }
    
    public void reset() {
        ttkHistory.clear();
        distanceHistory.clear();
        lastHitCellGlobal = -1;
        lastAngleGlobal = 0.0;
        lastData = null;
    }

    public TripletData getLastData() { return lastData; }

    private int generateRandomCell(int active1, int active2) {
        int cell;
        do {
            cell = random.nextInt(96);
        } while (cell == active1 || cell == active2 || cell == lastHitCellGlobal);
        return cell;
    }

    private float calculateHistoryMean() {
        if (ttkHistory.isEmpty()) return 450.0f; 
        double sum = 0;
        for (double val : ttkHistory) sum += val;
        return (float) (sum / ttkHistory.size());
    }

    private float calculateHistoryStd(float mean) {
        if (ttkHistory.size() <= 1) return 0.0f;
        double sumSquares = 0;
        for (double val : ttkHistory) sumSquares += Math.pow(val - mean, 2);
        return (float) Math.sqrt(sumSquares / ttkHistory.size());
    }
}