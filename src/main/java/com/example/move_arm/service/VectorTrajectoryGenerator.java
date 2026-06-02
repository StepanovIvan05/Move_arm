package com.example.move_arm.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VectorTrajectoryGenerator {

    private static VectorTrajectoryGenerator instance;
    private final Random random = new Random();

    public enum Difficulty { EASY, MEDIUM, HARD }
    private Difficulty currentDifficulty = Difficulty.EASY;

    private static final double MIN_SAFE_DISTANCE = 100.0;
    private static final double HARD_LIVE_TARGET_DIST = 130.0;

    private static final double EASY_DISTANCE = 120.0;
    private static final double EASY_MAX_ANGLE_DEV = 25.0;

    private static final double MEDIUM_DISTANCE = 190.0;
    private static final double MEDIUM_MAX_ANGLE_DEV = 60.0;

    private static final double ABSOLUTE_HARD_MIN_DIST = 300.0;
    private static final double HARD_LINE_SAFE_MARGIN = 80.0;

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

    public double[] nextPoint(
            double width,
            double height,
            int radius,
            List<double[]> liveTargets,
            double[] previousHit,
            double[] lastHit) {

        if (currentDifficulty == Difficulty.HARD) {
            return generateHardCoreRandomPoint(
                    width,
                    height,
                    radius,
                    liveTargets,
                    lastHit,
                    previousHit
            );
        }

        boolean hasTrajectoryData =
                previousHit != null &&
                lastHit != null &&
                liveTargets != null &&
                liveTargets.size() >= 2;

        double[] resultPoint;

        if (!hasTrajectoryData) {
            resultPoint = findRandomSafePoint(width, height, radius, liveTargets, null, MIN_SAFE_DISTANCE, width);
        } else {
            double[] live1 = liveTargets.get(0);
            double[] live2 = liveTargets.get(1);
            double baseAngle = Math.atan2(
                    lastHit[1] - previousHit[1],
                    lastHit[0] - previousHit[0]
            );

            double idealDistance = (currentDifficulty == Difficulty.EASY) ? EASY_DISTANCE : MEDIUM_DISTANCE;

            resultPoint = runMultipliersScan(lastHit, live1, live2, width, height, radius, idealDistance, baseAngle);

            if (resultPoint == null) {
                resultPoint = runWideAngleScan(lastHit, live1, live2, width, height, radius, idealDistance, baseAngle);
            }

            if (resultPoint == null) {
                double maxLimit = (currentDifficulty == Difficulty.EASY) ? 220.0 : 350.0;
                resultPoint = findRandomSafePoint(width, height, radius, liveTargets, lastHit, MIN_SAFE_DISTANCE, maxLimit);
            }
        }

        if (resultPoint == null) {
            resultPoint = fallbackPoint(width, height, radius);
        }

        return resultPoint;
    }

    private double[] generateHardCoreRandomPoint(
            double w,
            double h,
            int r,
            List<double[]> liveTargets,
            double[] currentHit,
            double[] previousHit) {
        double margin = r * 2.5;

        for (int i = 0; i < 600; i++) {
            double x = randomCoordinate(margin, w);
            double y = randomCoordinate(margin, h);

            if (currentHit != null) {
                double distToCursor = Math.hypot(x - currentHit[0], y - currentHit[1]);
                if (distToCursor < ABSOLUTE_HARD_MIN_DIST) continue;
            }

            if (liveTargets != null && liveTargets.size() >= 2) {
                double[] l1 = liveTargets.get(0);
                double[] l2 = liveTargets.get(1);

                if (Math.hypot(x - l1[0], y - l1[1]) < HARD_LIVE_TARGET_DIST ||
                        Math.hypot(x - l2[0], y - l2[1]) < HARD_LIVE_TARGET_DIST) continue;

                if (currentHit != null) {
                    if (previousHit != null && isPointNearLine(x, y, previousHit, currentHit, HARD_LINE_SAFE_MARGIN)) continue;
                    if (isPointNearLine(x, y, currentHit, l1, HARD_LINE_SAFE_MARGIN)) continue;
                    if (isPointNearLine(x, y, currentHit, l2, HARD_LINE_SAFE_MARGIN)) continue;
                }
            }
            return new double[]{x, y};
        }

        for (int i = 0; i < 300; i++) {
            double x = randomCoordinate(margin, w);
            double y = randomCoordinate(margin, h);

            if (currentHit != null) {
                double distToCursor = Math.hypot(x - currentHit[0], y - currentHit[1]);
                if (distToCursor < ABSOLUTE_HARD_MIN_DIST) continue;
            }

            if (isSafeFromAll(x, y, liveTargets, MIN_SAFE_DISTANCE, w, h, r)) {
                return new double[]{x, y};
            }
        }

        if (currentHit != null) {
            double randomAngle = random.nextDouble() * 2 * Math.PI;
            double x = currentHit[0] + ABSOLUTE_HARD_MIN_DIST * Math.cos(randomAngle);
            double y = currentHit[1] + ABSOLUTE_HARD_MIN_DIST * Math.sin(randomAngle);

            x = clampToField(x, margin, w);
            y = clampToField(y, margin, h);
            return new double[]{x, y};
        }

        double[] point = findRandomSafePoint(w, h, r, liveTargets, currentHit, ABSOLUTE_HARD_MIN_DIST, w);
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

    private static class AngleSector {
        final double start;
        final double end;

        AngleSector(double start, double end) {
            this.start = start;
            this.end = end;
        }
    }

    public void reset() {
    }
}
