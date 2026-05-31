package com.example.move_arm.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VectorTrajectoryGenerator implements PointGenerator {

    private static VectorTrajectoryGenerator instance;
    private final Random random = new Random();
    private double[] lastHitPoint = null;

    public enum Difficulty { EASY, MEDIUM, HARD }
    private Difficulty currentDifficulty = Difficulty.EASY; // По умолчанию ставим на HARD, чтобы сразу было заметно отличие от рандома

    // Глобальные ограничения расстояний
    private static final double MIN_SAFE_DISTANCE = 100.0; // Базовая безопасность для Easy/Medium
    private static final double HARD_LIVE_TARGET_DIST = 130.0; // Дистанция между живыми целями на Харде

    // Константы для EASY (Плавная змейка)
    private static final double EASY_DISTANCE = 120.0;
    private static final double EASY_MAX_ANGLE_DEV = 25.0;
    
    // Константы для MEDIUM (Зигзаги)
    private static final double MEDIUM_DISTANCE = 190.0;
    private static final double MEDIUM_MAX_ANGLE_DEV = 60.0;
    
    // Константы для HARD (Абсолютный хаос с жесткими фильтрами)
    private static final double ABSOLUTE_HARD_MIN_DIST = 300.0; // Железный минимум флика от курсора
    private static final double HARD_LINE_SAFE_MARGIN = 80.0; // Толщина запретных линий отчуждения

    private static final double[] DISTANCE_MULTIPLIERS = {1.0, 0.85, 0.70};
    private static final int ANGLE_SAMPLES = 180; 

    private VectorTrajectoryGenerator() {}

    public static synchronized VectorTrajectoryGenerator getInstance() {
        if (instance == null) instance = new VectorTrajectoryGenerator();
        return instance;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.currentDifficulty = difficulty;
    }

    @Override
    public double[] nextPoint(double width, double height, int radius, List<double[]> activePoints) {
        
        // --- РЕЖИМ HARD: БЕЗКОМПРОМИССНЫЙ ГЛОБАЛЬНЫЙ РАНДОМ ---
        if (currentDifficulty == Difficulty.HARD) {
            double[] currentHit = (activePoints != null && activePoints.size() >= 3) ? activePoints.get(2) : null;
            return generateHardCoreRandomPoint(width, height, radius, activePoints, currentHit);
        }

        // --- РЕЖИМЫ EASY И MEDIUM: СЕКТОРНАЯ АНАЛИТИКА ---
        boolean hasTrajectoryData = (lastHitPoint != null) 
            && (activePoints != null) 
            && (activePoints.size() >= 3);
        
        double[] resultPoint = null;
        
        if (!hasTrajectoryData) {
            resultPoint = findRandomSafePoint(width, height, radius, activePoints, null, MIN_SAFE_DISTANCE, width);
        } else {
            double[] live1 = activePoints.get(0);
            double[] live2 = activePoints.get(1);
            double[] currentHit = activePoints.get(2);

            double baseAngle = Math.atan2(
                currentHit[1] - lastHitPoint[1], 
                currentHit[0] - lastHitPoint[0]
            );
            
            double idealDistance = (currentDifficulty == Difficulty.EASY) ? EASY_DISTANCE : MEDIUM_DISTANCE;
            
            // Аналитический скан в родных углах
            resultPoint = runMultipliersScan(currentHit, live1, live2, width, height, radius, idealDistance, baseAngle);

            // Адаптивный отскок от борта (расширение углов, если уперлись в край)
            if (resultPoint == null) {
                resultPoint = runWideAngleScan(currentHit, live1, live2, width, height, radius, idealDistance, baseAngle);
            }

            // Локальный аварийный рандом (без прыжков через весь экран)
            if (resultPoint == null) {
                double maxLimit = (currentDifficulty == Difficulty.EASY) ? 220.0 : 350.0;
                resultPoint = findRandomSafePoint(width, height, radius, activePoints, currentHit, MIN_SAFE_DISTANCE, maxLimit);
            }
        }

        // Страховочный fallback для экзотических ситуаций
        if (resultPoint == null && activePoints != null && activePoints.size() >= 3) {
            resultPoint = new double[]{width / 2 + random.nextInt(20) - 10, height / 2 + random.nextInt(20) - 10};
        }

        if (activePoints != null && activePoints.size() >= 3) {
            lastHitPoint = activePoints.get(2);
        }
        
        return resultPoint;
    }

    /**
     * Трехэтапный геометрический генератор хаоса для HARD режима.
     * Гарантирует расстояние >= 300 пикселей от курсора при любых условиях на экране.
     */
    private double[] generateHardCoreRandomPoint(double w, double h, int r, List<double[]> activePoints, double[] currentHit) {
        double margin = r * 2.5;
        
        // ЭТАП 1: Идеальный поиск (все фильтры включены: дистанция, живые цели, запретные линии)
        for (int i = 0; i < 600; i++) {
            double x = margin + random.nextDouble() * (w - 2 * margin);
            double y = margin + random.nextDouble() * (h - 2 * margin);
            
            if (currentHit != null) {
                double distToCursor = Math.hypot(x - currentHit[0], y - currentHit[1]);
                if (distToCursor < ABSOLUTE_HARD_MIN_DIST) continue; 
            }
            
            if (activePoints != null && activePoints.size() >= 2) {
                double[] l1 = activePoints.get(0);
                double[] l2 = activePoints.get(1);
                
                if (Math.hypot(x - l1[0], y - l1[1]) < HARD_LIVE_TARGET_DIST || 
                    Math.hypot(x - l2[0], y - l2[1]) < HARD_LIVE_TARGET_DIST) continue; 

                if (currentHit != null) {
                    // Запрет спавна на продлении вектора движения руки
                    if (lastHitPoint != null && isPointNearLine(x, y, lastHitPoint, currentHit, HARD_LINE_SAFE_MARGIN)) continue;
                    // Запрет спавна на линиях «сбитая цель -> живые цели»
                    if (isPointNearLine(x, y, currentHit, l1, HARD_LINE_SAFE_MARGIN)) continue;
                    if (isPointNearLine(x, y, currentHit, l2, HARD_LINE_SAFE_MARGIN)) continue;
                }
            }
            return new double[]{x, y}; 
        }
        
        // ЭТАП 2: Аварийный поиск (если экран перегружен линиями)
        // Отключаем проверку траекторных линий и живых целей, но ДИСТАНЦИЮ 300+ ПИКСЕЛЕЙ ДЕРЖИМ ЖЕСТКО
        for (int i = 0; i < 300; i++) {
            double x = margin + random.nextDouble() * (w - 2 * margin);
            double y = margin + random.nextDouble() * (h - 2 * margin);
            
            if (currentHit != null) {
                double distToCursor = Math.hypot(x - currentHit[0], y - currentHit[1]);
                if (distToCursor < ABSOLUTE_HARD_MIN_DIST) continue; 
            }
            
            if (isSafeFromAll(x, y, activePoints, MIN_SAFE_DISTANCE, w, h, r)) {
                return new double[]{x, y};
            }
        }
        
        // ЭТАП 3: Математический щит (если вообще нет места на экране)
        // Выталкиваем точку строго на 300 пикселей от курсора под случайным углом, удерживая в бортах
        if (currentHit != null) {
            double randomAngle = random.nextDouble() * 2 * Math.PI;
            double x = currentHit[0] + ABSOLUTE_HARD_MIN_DIST * Math.cos(randomAngle);
            double y = currentHit[1] + ABSOLUTE_HARD_MIN_DIST * Math.sin(randomAngle);
            
            x = Math.max(margin, Math.min(w - margin, x));
            y = Math.max(margin, Math.min(h - margin, y));
            return new double[]{x, y};
        }
        
        return findRandomSafePoint(w, h, r, activePoints, currentHit, ABSOLUTE_HARD_MIN_DIST, w);
    }

    /**
     * Проверка близости точки (px, py) к бесконечной прямой линии, проходящей через p1 и p2
     */
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

    private double[] runMultipliersScan(double[] currentHit, double[] live1, double[] live2, 
                                        double w, double h, int r, double idealDistance, double baseAngle) {
        for (double multiplier : DISTANCE_MULTIPLIERS) {
            double[] point = findPointAnalytical(currentHit, live1, live2, w, h, r, idealDistance * multiplier, baseAngle, false);
            if (point != null) return point;
        }
        return null;
    }

    private double[] runWideAngleScan(double[] currentHit, double[] live1, double[] live2, 
                                      double w, double h, int r, double idealDistance, double baseAngle) {
        for (double multiplier : DISTANCE_MULTIPLIERS) {
            double[] point = findPointAnalytical(currentHit, live1, live2, w, h, r, idealDistance * multiplier, baseAngle, true);
            if (point != null) return point;
        }
        return null;
    }

    private double[] findPointAnalytical(double[] start, double[] live1, double[] live2,
                                         double w, double h, int r, double targetDist, 
                                         double baseAngle, boolean forceWideAngles) {
        
        List<double[]> angleRanges = new ArrayList<>();
        if (forceWideAngles) {
            double dev = Math.toRadians(currentDifficulty == Difficulty.EASY ? 90.0 : 130.0);
            angleRanges.add(new double[]{baseAngle - dev, baseAngle + dev});
        } else {
            double dev = Math.toRadians(currentDifficulty == Difficulty.EASY ? EASY_MAX_ANGLE_DEV : MEDIUM_MAX_ANGLE_DEV);
            angleRanges.add(new double[]{baseAngle - dev, baseAngle + dev});
        }

        List<AngleSector> validSectors = new ArrayList<>();
        
        for (double[] range : angleRanges) {
            double angleMin = normalizeAngle(range[0]);
            double angleMax = normalizeAngle(range[1]);
            List<double[]> normalizedRanges = splitAngleRange(angleMin, angleMax);
            
            for (double[] subRange : normalizedRanges) {
                double subMin = subRange[0];
                double subMax = subRange[1];
                double step = (subMax - subMin) / ANGLE_SAMPLES;
                if (step <= 0) continue;
                
                Double sectorStart = null;
                double prevAngle = subMin;
                
                for (int i = 0; i <= ANGLE_SAMPLES; i++) {
                    double angle = subMin + i * step;
                    double tx = start[0] + targetDist * Math.cos(angle);
                    double ty = start[1] + targetDist * Math.sin(angle);
                    
                    boolean isValid = isPointValid(tx, ty, live1, live2, start, w, h, r);
                    
                    if (isValid && sectorStart == null) {
                        sectorStart = prevAngle;
                    } else if (!isValid && sectorStart != null) {
                        validSectors.add(new AngleSector(sectorStart, prevAngle));
                        sectorStart = null;
                    }
                    prevAngle = angle;
                }
                
                if (sectorStart != null) {
                    validSectors.add(new AngleSector(sectorStart, subMax));
                }
            }
        }
        
        if (!validSectors.isEmpty()) {
            AngleSector sector = validSectors.get(random.nextInt(validSectors.size()));
            double chosenAngle = sector.start + random.nextDouble() * (sector.end - sector.start);
            return new double[]{
                start[0] + targetDist * Math.cos(chosenAngle),
                start[1] + targetDist * Math.sin(chosenAngle)
            };
        }
        return null;
    }

    private double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    private List<double[]> splitAngleRange(double min, double max) {
        List<double[]> result = new ArrayList<>();
        if (min <= max) {
            result.add(new double[]{min, max});
        } else {
            result.add(new double[]{min, Math.PI});
            result.add(new double[]{-Math.PI, max});
        }
        return result;
    }

    private boolean isPointValid(double x, double y, double[] l1, double[] l2, double[] start,
                                  double w, double h, int r) {
        double margin = r * 2.0;
        if (x < margin || x > w - margin || y < margin || y > h - margin) return false;
        
        double minDistSq = MIN_SAFE_DISTANCE * MIN_SAFE_DISTANCE;
        if ((x - l1[0]) * (x - l1[0]) + (y - l1[1]) * (y - l1[1]) < minDistSq) return false;
        if ((x - l2[0]) * (x - l2[0]) + (y - l2[1]) * (y - l2[1]) < minDistSq) return false;
        if ((x - start[0]) * (x - start[0]) + (y - start[1]) * (y - start[1]) < minDistSq) return false;
        
        return true;
    }

    private double[] findRandomSafePoint(double w, double h, int r, List<double[]> activePoints, 
                                         double[] start, double minDist, double maxDist) {
        double margin = r * 2.0;
        for (int i = 0; i < 200; i++) {
            double x = margin + random.nextDouble() * (w - 2 * margin);
            double y = margin + random.nextDouble() * (h - 2 * margin);
            
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

    private static class AngleSector {
        final double start;
        final double end;
        AngleSector(double start, double end) {
            this.start = start;
            this.end = end;
        }
    }

    public void reset() {
        this.lastHitPoint = null;
    }
}