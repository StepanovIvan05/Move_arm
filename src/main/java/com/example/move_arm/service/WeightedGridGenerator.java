package com.example.move_arm.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WeightedGridGenerator implements PointGenerator {

    private final Random random = new Random();
    private List<Region> regions = new ArrayList<>();
    private double lastWidth = -1, lastHeight = -1;
    private int radiusCache = 0;
    private List<double[]> previousActivePoints = new ArrayList<>();
    private int spawnCounter = 0;

    private static class Region {
        double minX, maxX, minY, maxY;
        double centerX, centerY;
        double weight = 1.0;
        int lastSpawnedAt = -200;
    }

    public WeightedGridGenerator() {}

    public void initialize(int seed) {
        if (seed != 0) random.setSeed(seed);
        reset();
    }

    @Override
    public void reset() {
        previousActivePoints.clear();
        spawnCounter = 0;
        for (Region r : regions) {
            r.weight = 1.0;
            r.lastSpawnedAt = -200;
        }
    }

    @Override
    public double[] nextPoint(double width, double height, int radius, List<double[]> currentActivePoints) {
        spawnCounter++;

        if (width < 200 || height < 200) {
            previousActivePoints = new ArrayList<>(currentActivePoints);
            return fallbackRandom(width, height, radius);
        }

        if (Math.abs(width - lastWidth) > 1 || Math.abs(height - lastHeight) > 1 || radius != radiusCache) {
            rebuildGrid(width, height, radius);
            lastWidth = width;
            lastHeight = height;
            radiusCache = radius;
        }

    
        double[] justHit = detectJustHitPoint(currentActivePoints);

        updateRegionWeights(justHit, currentActivePoints);

        previousActivePoints = new ArrayList<>(currentActivePoints);

        int b = 0;

        if (spawnCounter % 3 == 0 || b == 1) {   // выводим каждые 3 спавна, чтобы не было слишком часто
            printRegionWeights(currentActivePoints, justHit);
        }

        return choosePoint(currentActivePoints, justHit);
    }

    private void printRegionWeights(List<double[]> currentActive, double[] justHit) {
        System.out.println("\n=== ВЕСА РЕГИОНОВ — Спавн #" + spawnCounter + " ===");
        if (justHit != null) {
            System.out.printf("Последняя сбитая цель: (%.0f, %.0f)%n", justHit[0], justHit[1]);
        } else {
            System.out.println("Последняя сбитая цель: —");
        }

        System.out.println("№  | Центр (X,Y)     | Вес     | Возраст | Занят | Последний спавн");
        System.out.println("---|------------------|---------|---------|-------|----------------");

        for (int i = 0; i < regions.size(); i++) {
            Region r = regions.get(i);
            boolean occupied = isRegionOccupied(r, currentActive);
            int age = spawnCounter - r.lastSpawnedAt;

            System.out.printf("%2d | (%4.0f,%4.0f)   | %6.3f  | %6d  | %-5s | %d%n",
                    i,
                    r.centerX, r.centerY,
                    r.weight,
                    age,
                    occupied ? "Да" : "Нет",
                    r.lastSpawnedAt);
        }
        System.out.println("====================================================================\n");
    }

    private double[] detectJustHitPoint(List<double[]> current) {
        if (previousActivePoints.isEmpty()) return null;
        for (double[] prev : previousActivePoints) {
            boolean stillExists = current.stream().anyMatch(curr ->
                    Math.hypot(prev[0] - curr[0], prev[1] - curr[1]) < 3.0);
            if (!stillExists) return prev;
        }
        return null;
    }

    private void updateRegionWeights(double[] justHit, List<double[]> currentActive) {
        // 1. Пассивный бонус для давно неиспользованных регионов (твоя главная идея)
        for (Region r : regions) {
            int age = spawnCounter - r.lastSpawnedAt;
            double passive = 0.4 + 0.028 * Math.max(0, age - 6);   // сильнее чем раньше
            r.weight = r.weight * 0.935 + passive;
        }

        // 2. Hit — наказание
        if (justHit != null) {
            Region r = findRegion(justHit[0], justHit[1]);
            if (r != null) {
                r.lastSpawnedAt = spawnCounter;
                r.weight = r.weight * 0.78 + 0.25;   // сильнее наказываем
            }
        }

        // 3. Очень слабый игнор-бонус
        for (double[] p : currentActive) {
            Region r = findRegion(p[0], p[1]);
            if (r != null) {
                r.weight = r.weight * 1.025 + 0.8;   // минимальный
            }
        }

        // 4. Сглаживание весов соседей (плавность!)
        smoothWeights();
    }

    // Новое: распространение веса на соседей
    private void smoothWeights() {
        double[] newWeights = new double[regions.size()];

        for (int i = 0; i < regions.size(); i++) {
            Region r = regions.get(i);
            double sum = r.weight;
            int count = 1;

            // Берём веса соседей (простая 8-соседняя окрестность)
            for (int j = 0; j < regions.size(); j++) {
                if (i == j) continue;
                Region other = regions.get(j);
                double dist = Math.hypot(r.centerX - other.centerX, r.centerY - other.centerY);
                if (dist < 300) {   // соседние клетки
                    sum += other.weight * 0.35;
                    count++;
                }
            }
            newWeights[i] = sum / count;
        }

        // Применяем сглаженные веса
        for (int i = 0; i < regions.size(); i++) {
            regions.get(i).weight = newWeights[i];
        }
    }

    private double[] choosePoint(List<double[]> currentActive, double[] justHit) {
        double totalWeight = 0.0;
        double[] cumulative = new double[regions.size()];
        double maxDist = Math.hypot(lastWidth, lastHeight);

        for (int i = 0; i < regions.size(); i++) {
            Region r = regions.get(i);
            if (isRegionOccupied(r, currentActive)) {
                cumulative[i] = totalWeight;
                continue;
            }

            double bonus = calculateBonus(r, currentActive, justHit, maxDist);
            double finalWeight = Math.max(0.45, r.weight * bonus);   // высокий минимум

            totalWeight += finalWeight;
            cumulative[i] = totalWeight;
        }

        if (totalWeight <= 0) return fallbackRandom(lastWidth, lastHeight, radiusCache);

        double roll = random.nextDouble() * totalWeight;
        for (int i = 0; i < cumulative.length; i++) {
            if (roll <= cumulative[i]) {
                Region chosen = regions.get(i);
                chosen.lastSpawnedAt = spawnCounter;

                double margin = 30;
                double x = chosen.minX + margin + random.nextDouble() * (chosen.maxX - chosen.minX - 2 * margin);
                double y = chosen.minY + margin + random.nextDouble() * (chosen.maxY - chosen.minY - 2 * margin);
                return new double[]{x, y};
            }
        }
        return fallbackRandom(lastWidth, lastHeight, radiusCache);
    }

    private double calculateBonus(Region r, List<double[]> currentActive, double[] justHit, double maxDist) {
        double exploration = 0.6;
        if (justHit != null) {
            double dist = Math.hypot(r.centerX - justHit[0], r.centerY - justHit[1]);
            exploration = 0.6 + 3.8 * (dist / maxDist);
        }

        double sparsity = 4.2;
        if (!currentActive.isEmpty()) {
            double minD = currentActive.stream()
                    .mapToDouble(p -> Math.hypot(r.centerX - p[0], r.centerY - p[1]))
                    .min().orElse(1500);
            sparsity = 1.0 + 2.9 * Math.min(1.0, minD / 450.0);
        }
        return exploration * sparsity;
    }

    private Region findRegion(double x, double y) {
        for (Region r : regions) {
            if (x >= r.minX && x <= r.maxX && y >= r.minY && y <= r.maxY) return r;
        }
        return null;
    }

    private boolean isRegionOccupied(Region r, List<double[]> activePoints) {
        return activePoints.stream().anyMatch(p ->
                p[0] >= r.minX && p[0] <= r.maxX && p[1] >= r.minY && p[1] <= r.maxY);
    }

    private void rebuildGrid(double width, double height, int radius) {
        regions.clear();
        double effectiveW = width - 2 * radius;
        double effectiveH = height - 2 * radius;

        int numCols = Math.max(3, Math.min(6, (int) (effectiveW / 270)));
        int numRows = Math.max(2, Math.min(5, (int) (effectiveH / 270)));

        double cellW = effectiveW / numCols;
        double cellH = effectiveH / numRows;

        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                Region r = new Region();
                r.minX = radius + col * cellW;
                r.maxX = r.minX + cellW;
                r.minY = radius + row * cellH;
                r.maxY = r.minY + cellH;
                r.centerX = (r.minX + r.maxX) / 2;
                r.centerY = (r.minY + r.maxY) / 2;
                regions.add(r);
            }
        }
    }

    private double[] fallbackRandom(double width, double height, int radius) {
        return new double[]{
                radius + random.nextDouble() * (width - 2 * radius),
                radius + random.nextDouble() * (height - 2 * radius)
        };
    }
}