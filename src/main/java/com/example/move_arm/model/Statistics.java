// model/Statistics.java
package com.example.move_arm.model;

import java.util.ArrayList;
import java.util.List;

public class Statistics {

    public static List<Double> getIntervalsMs(List<Long> nanoTimes) {
        List<Double> intervals = new ArrayList<>();
        for (int i = 1; i < nanoTimes.size(); i++) {
            double diff = (nanoTimes.get(i) - nanoTimes.get(i - 1)) / 1_000_000.0;
            intervals.add(diff);
        }
        return intervals;
    }

    public static double getAverageMs(List<Long> nanoTimes) {
        return getIntervalsMs(nanoTimes).stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    public static double getMinMs(List<Long> nanoTimes) {
        return getIntervalsMs(nanoTimes).stream()
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0.0);
    }

    public static double getMaxMs(List<Long> nanoTimes) {
        return getIntervalsMs(nanoTimes).stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);
    }

    public static int getSpawnCount(List<Long> nanoTimes) {
        return nanoTimes.size();
    }
}