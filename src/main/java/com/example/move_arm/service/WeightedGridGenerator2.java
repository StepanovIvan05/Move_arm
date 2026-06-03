package com.example.move_arm.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Контекстный взвешенный генератор.
 * Веса регионов пересчитываются каждый раз на основе текущей ситуации на экране.
 * Не использует persistent веса и не требует изменений в GameController.
 */
public class WeightedGridGenerator2 implements PointGenerator {

    private final Random random = new Random();
    private List<Region> regions = new ArrayList<>();
    private double lastWidth = -1;
    private double lastHeight = -1;
    private int radiusCache = 0;

    private static class Region {
        double minX, maxX, minY, maxY;
        double centerX, centerY;
    }

    public WeightedGridGenerator2() {}

    public void initialize(int seed) {
        if (seed != 0) {
            random.setSeed(seed);
        }
    }

    @Override
    public double[] nextPoint(double width, double height, int radius, List<double[]> activePoints) {
        if (width < 200 || height < 200) {
            return fallbackRandom(width, height, radius);
        }

        // Перестраиваем грид только если размеры экрана изменились
        if (Math.abs(width - lastWidth) > 1 || Math.abs(height - lastHeight) > 1 || radius != radiusCache) {
            rebuildGrid(width, height, radius);
            lastWidth = width;
            lastHeight = height;
            radiusCache = radius;
        }

        // Последняя сбитая цель — всегда последний элемент в списке
        double[] lastHit = activePoints.isEmpty() ? null : activePoints.get(activePoints.size() - 1);

        // Все остальные цели — проигнорированные (висящие на экране)
        List<double[]> ignoredPoints = activePoints.size() > 1
                ? activePoints.subList(0, activePoints.size() - 1)
                : List.of();

        double maxDist = Math.hypot(width, height);

        // Подсчёт весов только для пустых регионов
        double totalWeight = 0.0;
        double[] cumulativeWeights = new double[regions.size()];
        int validRegions = 0;

        for (int i = 0; i < regions.size(); i++) {
            Region r = regions.get(i);

            if (isRegionOccupied(r, activePoints)) {
                cumulativeWeights[i] = totalWeight; // вес = 0
                continue;
            }

            double weight = calculateWeight(r, ignoredPoints, lastHit, maxDist);
            totalWeight += weight;
            cumulativeWeights[i] = totalWeight;
            validRegions++;
        }

        if (validRegions == 0 || totalWeight <= 0) {
            return fallbackRandom(width, height, radius);
        }

        // Взвешенный выбор региона
        double roll = random.nextDouble() * totalWeight;
        Region chosen = null;
        for (int i = 0; i < cumulativeWeights.length; i++) {
            if (roll <= cumulativeWeights[i]) {
                chosen = regions.get(i);
                break;
            }
        }

        // Генерируем точку внутри выбранного региона с небольшим отступом от краёв
        double margin = 25.0;
        double x = chosen.minX + margin + random.nextDouble() * (chosen.maxX - chosen.minX - 2 * margin);
        double y = chosen.minY + margin + random.nextDouble() * (chosen.maxY - chosen.minY - 2 * margin);

        return new double[]{x, y};
    }

    /**
     * Основная формула расчёта веса региона (пересчитывается каждый раз)
     */
    private double calculateWeight(Region r, List<double[]> ignoredPoints, double[] lastHit, double maxDist) {
        // 1. Exploration: чем дальше от последней сбитой цели — тем лучше
        double exploration = 0.5;
        if (lastHit != null) {
            double distToLast = Math.hypot(r.centerX - lastHit[0], r.centerY - lastHit[1]);
            exploration = 0.5 + 3.4 * (distToLast / maxDist);
        }

        // 2. Sparsity: чем дальше от всех висящих (проигнорированных) целей — тем лучше
        double sparsity = 4.0;
        if (!ignoredPoints.isEmpty()) {
            double minDistToIgnored = ignoredPoints.stream()
                    .mapToDouble(p -> Math.hypot(r.centerX - p[0], r.centerY - p[1]))
                    .min()
                    .orElse(1500.0);

            sparsity = 0.8 + 2.9 * Math.min(1.0, minDistToIgnored / 420.0);
        }

        double weight = exploration * sparsity;

        // Ограничения
        weight = Math.max(0.35, weight);                    // минимальный вес
        weight *= (0.93 + random.nextDouble() * 0.14);     // небольшой шум

        return weight;
    }

    private boolean isRegionOccupied(Region r, List<double[]> activePoints) {
        return activePoints.stream().anyMatch(p ->
                p[0] >= r.minX && p[0] <= r.maxX &&
                p[1] >= r.minY && p[1] <= r.maxY);
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
                r.centerX = (r.minX + r.maxX) / 2.0;
                r.centerY = (r.minY + r.maxY) / 2.0;
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

    @Override
    public void reset() {
        // WeightedGridGenerator2 не требует специального сброса состояния
    }
}