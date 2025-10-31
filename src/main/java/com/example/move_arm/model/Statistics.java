// model/Statistics.java
package com.example.move_arm.model;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Point2D;

public class Statistics {

    // === 1. Временные интервалы между кликами (мс) ===
    public static List<Double> getClickIntervalsMs(List<ClickData> clicks) {
        List<Double> intervals = new ArrayList<>();
        for (int i = 1; i < clicks.size(); i++) {
            long prev = clicks.get(i - 1).getClickTimeNs();
            long curr = clicks.get(i).getClickTimeNs();
            intervals.add((curr - prev) / 1_000_000.0);
        }
        return intervals;
    }

    public static double getAverageClickIntervalMs(List<ClickData> clicks) {
        return getClickIntervalsMs(clicks).stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    // === 2. Расстояние между курсорами (px) ===
    public static List<Double> getCursorDistances(List<ClickData> clicks) {
        List<Double> distances = new ArrayList<>();
        for (int i = 1; i < clicks.size(); i++) {
            Point2D prev = clicks.get(i - 1).getCursor();
            Point2D curr = clicks.get(i).getCursor();
            distances.add(prev.distance(curr));
        }
        return distances;
    }

    public static double getAverageCursorDistance(List<ClickData> clicks) {
        return getCursorDistances(clicks).stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    // === 3. Нормализованная скорость: расстояние / время (px/мс) ===
    public static List<Double> getMovementSpeeds(List<ClickData> clicks) {
        List<Double> speeds = new ArrayList<>();
        List<Double> distances = getCursorDistances(clicks);
        List<Double> intervals = getClickIntervalsMs(clicks);

        for (int i = 0; i < distances.size(); i++) {
            double timeMs = intervals.get(i);
            if (timeMs > 0) {
                speeds.add(distances.get(i) / timeMs);
            } else {
                speeds.add(0.0);
            }
        }
        return speeds;
    }

    public static double getAverageSpeedPxPerMs(List<ClickData> clicks) {
        return getMovementSpeeds(clicks).stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    public static double getMaxSpeedPxPerMs(List<ClickData> clicks) {
        return getMovementSpeeds(clicks).stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);
    }

    // === 4. Отклонение / радиус (0 = центр, 1 = граница, >1 = промах) ===
    public static List<Double> getNormalizedDeviations(List<ClickData> clicks) {
        List<Double> norms = new ArrayList<>();
        for (ClickData c : clicks) {
            double deviation = c.getCursor().distance(c.getCenter());
            double radius = c.getRadius();
            norms.add(radius > 0 ? deviation / radius : 0.0);
        }
        return norms;
    }

    public static double getAverageNormalizedDeviation(List<ClickData> clicks) {
        return getNormalizedDeviations(clicks).stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    public static double getHitRatePercent(List<ClickData> clicks) {
        long hits = clicks.stream()
                .filter(c -> c.getCursor().distance(c.getCenter()) <= c.getRadius())
                .count();
        return clicks.isEmpty() ? 0.0 : (hits * 100.0) / clicks.size();
    }

    // === Общая статистика одной игрой ===
    public static String getSummary(List<ClickData> clicks) {
        if (clicks.isEmpty()) return "Нет данных";

        double avgInterval = getAverageClickIntervalMs(clicks);
        double avgDistance = getAverageCursorDistance(clicks);
        double avgSpeed = getAverageSpeedPxPerMs(clicks);
        double avgNormDev = getAverageNormalizedDeviation(clicks);

        return String.format(
            "Кликов: %d | " +
            "Ср. интервал: %.1f мс | " +
            "Ср. расстояние: %.1f px | " +
            "Ср. скорость: %.2f px/мс | " +
            "Ср. отклонение: %.2fR | ",
            clicks.size(), avgInterval, avgDistance, avgSpeed, avgNormDev
        );
    }
}