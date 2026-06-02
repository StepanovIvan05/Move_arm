package com.example.move_arm.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.example.move_arm.model.TrajectoryDifficulty;

public class VectorTrajectoryGenerator {

    private static VectorTrajectoryGenerator instance;
    private final Random random = new Random();

    private TrajectoryDifficulty currentDifficulty = TrajectoryDifficulty.MEDIUM;

    // Состояние виртуальной траектории для EASY и MEDIUM
    private double[] virtualHead = null;
    private double currentAngle = 0.0;

    // Глобальные константы безопасности и ограничений
    private static final double MIN_SAFE_DISTANCE = 100.0;
    private static final double HARD_LIVE_TARGET_DIST = 130.0;

    // Параметры для EASY (Близкие цели, плавная змейка)
    private static final double EASY_DISTANCE = 120.0;
    private static final double EASY_MAX_ANGLE_DEV = 25.0;

    // Параметры для MEDIUM (Далекие цели, размашистые зигзаги)
    private static final double MEDIUM_DISTANCE = 190.0;
    private static final double MEDIUM_MAX_ANGLE_DEV = 60.0;

    // Параметры для HARD
    private static final double ABSOLUTE_HARD_MIN_DIST = 300.0;
    private static final double HARD_LINE_SAFE_MARGIN = 80.0;

    private VectorTrajectoryGenerator() {}

    public static synchronized VectorTrajectoryGenerator getInstance() {
        if (instance == null) {
            instance = new VectorTrajectoryGenerator();
        }
        return instance;
    }

    public void setDifficulty(TrajectoryDifficulty difficulty) {
        this.currentDifficulty = difficulty;
    }

    /**
     * Сброс виртуальной траектории. 
     * ОБЯЗАТЕЛЬНО вызывать в HoverGamePresenter перед стартом новой игры.
     */
    public void resetTrajectory() {
        this.virtualHead = null;
        this.currentAngle = random.nextDouble() * 2 * Math.PI;
    }

    public double[] nextPoint(
            double width,
            double height,
            int radius,
            List<double[]> liveTargets,
            double[] previousHit,
            double[] lastHit) {

        // --- РЕЖИМ HARD: БЕЗКОМПРОМИССНЫЙ ХАОС ---
        if (currentDifficulty == TrajectoryDifficulty.HARD) {
            return generateHardCoreRandomPoint(width, height, radius, liveTargets, lastHit, previousHit);
        }

        // --- РЕЖИМЫ EASY И MEDIUM: УМНАЯ ЗМЕЙКА С ЗАЩИТОЙ ОТ НАЛОЖЕНИЙ И УГЛОВЫХ ТУПИКОВ ---
        double margin = radius * 2.5;

        // Если игра только началась (первый вызов), спавним первую точку в случайном безопасном месте
        if (virtualHead == null) {
            virtualHead = new double[]{
                    margin + random.nextDouble() * (width - 2 * margin),
                    margin + random.nextDouble() * (height - 2 * margin)
            };
            return virtualHead.clone();
        }

        // Настройка шага и углов на основе сложности
        double idealDistance = (currentDifficulty == TrajectoryDifficulty.EASY) ? EASY_DISTANCE : MEDIUM_DISTANCE;
        double maxAngleDevRad = Math.toRadians(
                (currentDifficulty == TrajectoryDifficulty.EASY) ? EASY_MAX_ANGLE_DEV : MEDIUM_MAX_ANGLE_DEV
        );

        // Расстояние между центрами кругов, при котором они начинают пересекаться (диаметр)
        double safeOverlapDistanceSq = (radius * 2.0) * (radius * 2.0);

        boolean pointFound = false;
        double nextX = 0;
        double nextY = 0;
        double chosenAngle = currentAngle;

        // Пробуем до 8 раз найти свободное направление. С каждой неудачной попыткой расширяем угол поиска.
        for (int attempt = 0; attempt < 8; attempt++) {
            double searchSpread = maxAngleDevRad + (attempt * Math.toRadians(20.0));
            double angleDeviation = (random.nextDouble() * 2 - 1) * searchSpread;
            double testAngle = currentAngle + angleDeviation;

            double tx = virtualHead[0] + idealDistance * Math.cos(testAngle);
            double ty = virtualHead[1] + idealDistance * Math.sin(testAngle);

            // 1. Проверяем столкновение со стенами и применяем скольжение
            boolean hitLeft = (tx < margin);
            boolean hitRight = (tx > width - margin);
            boolean hitTop = (ty < margin);
            boolean hitBottom = (ty > height - margin);

            if (hitLeft || hitRight || hitTop || hitBottom) {
                double sin = Math.sin(currentAngle);
                double cos = Math.cos(currentAngle);

                if (hitLeft || hitRight) {
                    testAngle = (sin < 0) ? -Math.PI / 2.0 : Math.PI / 2.0;
                } else {
                    testAngle = (cos < 0) ? Math.PI : 0.0;
                }

                tx = virtualHead[0] + idealDistance * Math.cos(testAngle);
                ty = virtualHead[1] + idealDistance * Math.sin(testAngle);
                
                tx = clampToField(tx, margin, width);
                ty = clampToField(ty, margin, height);
            }

            // 2. Проверяем, не накладывается ли новая точка на уже существующие круги
            boolean overlaps = false;
            if (liveTargets != null) {
                for (double[] target : liveTargets) {
                    double dx = tx - target[0];
                    double dy = ty - target[1];
                    if ((dx * dx + dy * dy) < safeOverlapDistanceSq) {
                        overlaps = true;
                        break;
                    }
                }
            }

            // Проверяем, сдвинулась ли змейка физически (защита от "залипания" в углах из-за зажима в clampToField)
            double distToOldHeadSq = (tx - virtualHead[0]) * (tx - virtualHead[0]) + (ty - virtualHead[1]) * (ty - virtualHead[1]);
            
            // Если точка свободна и голова реально продвинется вперед, а не останется на месте
            if (!overlaps && distToOldHeadSq > (radius * radius * 0.25)) {
                nextX = tx;
                nextY = ty;
                chosenAngle = testAngle;
                pointFound = true;
                break;
            }
        }

        // 3. Экстремальный выход из глухого угла (Поворот на 180 градусов назад)
        if (!pointFound) {
            // Если все пути заблокированы или зажаты, разворачиваем змейку вспять по собственной траектории
            currentAngle = normalizeAngle(currentAngle + Math.PI);
            
            nextX = virtualHead[0] + idealDistance * Math.cos(currentAngle);
            nextY = virtualHead[1] + idealDistance * Math.sin(currentAngle);

            nextX = clampToField(nextX, margin, width);
            nextY = clampToField(nextY, margin, height);
        } else {
            currentAngle = normalizeAngle(chosenAngle);
        }

        // Фиксируем новое положение виртуальной головы
        virtualHead[0] = nextX;
        virtualHead[1] = nextY;

        return virtualHead.clone();
    }

    /**
     * Трехэтапный геометрический генератор хаоса для HARD режима.
     */
    private double[] generateHardCoreRandomPoint(
            double w,
            double h,
            int r,
            List<double[]> liveTargets,
            double[] lastHit,
            double[] previousHit) {
        double margin = r * 2.5;

        for (int i = 0; i < 600; i++) {
            double x = randomCoordinate(margin, w);
            double y = randomCoordinate(margin, h);

            if (lastHit != null) {
                double distToCursor = Math.hypot(x - lastHit[0], y - lastHit[1]);
                if (distToCursor < ABSOLUTE_HARD_MIN_DIST) continue;
            }

            if (liveTargets != null && liveTargets.size() >= 2) {
                double[] l1 = liveTargets.get(0);
                double[] l2 = liveTargets.get(1);

                if (Math.hypot(x - l1[0], y - l1[1]) < HARD_LIVE_TARGET_DIST ||
                        Math.hypot(x - l2[0], y - l2[1]) < HARD_LIVE_TARGET_DIST) continue;

                if (lastHit != null) {
                    if (previousHit != null && isPointNearLine(x, y, previousHit, lastHit, HARD_LINE_SAFE_MARGIN)) continue;
                    if (isPointNearLine(x, y, lastHit, l1, HARD_LINE_SAFE_MARGIN)) continue;
                    if (isPointNearLine(x, y, lastHit, l2, HARD_LINE_SAFE_MARGIN)) continue;
                }
            }
            return new double[]{x, y};
        }

        for (int i = 0; i < 300; i++) {
            double x = randomCoordinate(margin, w);
            double y = randomCoordinate(margin, h);

            if (lastHit != null) {
                double distToCursor = Math.hypot(x - lastHit[0], y - lastHit[1]);
                if (distToCursor < ABSOLUTE_HARD_MIN_DIST) continue;
            }

            if (isSafeFromAll(x, y, liveTargets, MIN_SAFE_DISTANCE, w, h, r)) {
                return new double[]{x, y};
            }
        }

        if (lastHit != null) {
            double randomAngle = random.nextDouble() * 2 * Math.PI;
            double x = lastHit[0] + ABSOLUTE_HARD_MIN_DIST * Math.cos(randomAngle);
            double y = lastHit[1] + ABSOLUTE_HARD_MIN_DIST * Math.sin(randomAngle);

            x = clampToField(x, margin, w);
            y = clampToField(y, margin, h);
            return new double[]{x, y};
        }

        double[] point = findRandomSafePoint(w, h, r, liveTargets, lastHit, ABSOLUTE_HARD_MIN_DIST, w);
        return point != null ? point : fallbackPoint(w, h, r);
    }

    private boolean isPointNearLine(double px, double py, double[] p1, double[] p2, double margin) {
        double x1 = p1[0];
        double y1 = p1[1];
        double x2 = p2[0];
        double y2 = p2[1];

        double numerator = Math.abs((y2 - y1) * px - (x2 - x1) * py + x2 * y1 - y2 * x1);
        double denominator = Math.hypot(y2 - y1, x2 - x1);

        if (denominator == 0) return false;

        return (numerator / denominator) < margin;
    }

    private double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    private double[] findRandomSafePoint(double w, double h, int r, List<double[]> activePoints,
                                         double[] start, double minDist, double maxDist) {
        double margin = r * 2.0;
        for (int i = 0; i < 200; i++) {
            double x = randomCoordinate(margin, w);
            double y = randomCoordinate(margin, h);

            if (start != null) {
                double distToStart = Math.hypot(x - start[0], y - start[1]);
                if (distToStart < minDist || distToStart > maxDist) continue;
            }

            if (isSafeFromAll(x, y, activePoints, MIN_SAFE_DISTANCE, w, h, r)) {
                return new double[]{x, y};
            }
        }
        return null;
    }

    private boolean isSafeFromAll(double x, double y, List<double[]> activePoints,
                                  double minDist, double w, double h, int r) {
        double margin = r * 2.0;
        if (x < margin || x > w - margin || y < margin || y > h - margin) return false;

        if (activePoints != null) {
            double minDistSq = minDist * minDist;
            for (double[] p : activePoints) {
                double dx = x - p[0];
                double dy = y - p[1];
                if (dx * dx + dy * dy < minDistSq) return false;
            }
        }
        return true;
    }

    private double[] fallbackPoint(double width, double height, int radius) {
        double margin = Math.max(radius, radius * 2.0);
        return new double[]{
                randomCoordinate(margin, width),
                randomCoordinate(margin, height)
        };
    }

    private double randomCoordinate(double margin, double size) {
        if (size <= margin * 2) {
            return Math.max(0, size / 2.0);
        }
        return margin + random.nextDouble() * (size - 2 * margin);
    }

    private double clampToField(double value, double margin, double size) {
        if (size <= margin * 2) {
            return Math.max(0, size / 2.0);
        }
        return Math.max(margin, Math.min(size - margin, value));
    }

    public void reset() {
        resetTrajectory();
    }
}