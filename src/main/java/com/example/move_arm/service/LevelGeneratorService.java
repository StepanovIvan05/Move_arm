package com.example.move_arm.service;

import java.util.List;
import java.util.Random;

public class LevelGeneratorService implements PointGenerator {
    private static LevelGeneratorService instance;
    private Random random;
    private double minDistanceBetweenCircles = 150.0; // Можно вынести в настройки

    private LevelGeneratorService() {
        this.random = new Random();
    }

    public static LevelGeneratorService getInstance() {
        if (instance == null) instance = new LevelGeneratorService();
        return instance;
    }

    // Инициализация: если seed == null, будет рандом
    public void initialize(int seed) {
        if (seed != 0) {
            this.random = new Random(seed);
        } else {
            this.random = new Random();
        }
    }

    @Override
    public double[] nextPoint(double width, double height, int radius, List<double[]> activePoints) {
        int maxAttempts = 100;
        double minDistanceNow = minDistanceBetweenCircles;
        for (int j = 0; j < 10; j++){
            for (int i = 0; i < maxAttempts; i++) {
                double x = radius + random.nextDouble() * (width - 2 * radius);
                double y = radius + random.nextDouble() * (height - 2 * radius);

                if (isSafe(x, y, radius, activePoints, minDistanceNow)) {
                    return new double[]{x, y};
                }
            }
            minDistanceNow *= 0.8;
            System.out.println(minDistanceNow);
        }
        // Fallback: если место не нашли, возвращаем просто случайную точку
        return new double[]{radius + random.nextDouble() * (width - 2 * radius), 
                            radius + random.nextDouble() * (height - 2 * radius)};
    }

    private boolean isSafe(double x, double y, int radius, List<double[]> activePoints, double minDistance) {
        // Рассчитываем необходимую дистанцию один раз до цикла
        double minAllowedDist = radius * 2 + minDistance;
        // Возводим её в квадрат заранее
        double minAllowedDistSq = minAllowedDist * minAllowedDist;

        for (double[] p : activePoints) {
            double dx = Math.abs(x - p[0]);
            if (dx > minAllowedDist) continue; // Точно не пересекаются по горизонтали

            double dy = Math.abs(y - p[1]);
            if (dy > minAllowedDist) continue; // Точно не пересекаются по вертикали

            // И только если обе проверки выше не сработали, считаем точное расстояние по формуле
            if (dx * dx + dy * dy < minAllowedDistSq) return false;
        }
        return true;
    }

    @Override
    public void reset() {
        // LevelGeneratorService не требует специального сброса состояния
    }
}