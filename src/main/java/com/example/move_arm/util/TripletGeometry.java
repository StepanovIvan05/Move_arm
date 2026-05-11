package com.example.move_arm.util;

/**
 * Вычисление геометрических фич для тройки целей.
 */
public class TripletGeometry {
    
    public static class GeometryData {
        public double centroidRow, centroidCol;
        public double t1Angle, t2Angle, t3Angle;
        public double hitToMiss1Dist, hitToMiss2Dist, miss1ToMiss2Dist;
        public double spread;
    }
    
    /**
     * Вычисляет все геометрические фичи для тройки.
     */
    public static GeometryData compute(int t1Cell, int t2Cell, int t3Cell, int hitIndex) {
        GeometryData g = new GeometryData();
        
        int[] t1 = GridUtils.cellToRowCol(t1Cell);
        int[] t2 = GridUtils.cellToRowCol(t2Cell);
        int[] t3 = GridUtils.cellToRowCol(t3Cell);
        
        // Центр масс
        g.centroidRow = (t1[0] + t2[0] + t3[0]) / 3.0;
        g.centroidCol = (t1[1] + t2[1] + t3[1]) / 3.0;
        
        // Углы от центра масс
        g.t1Angle = angle(t1Cell, g.centroidRow, g.centroidCol);
        g.t2Angle = angle(t2Cell, g.centroidRow, g.centroidCol);
        g.t3Angle = angle(t3Cell, g.centroidRow, g.centroidCol);
        
        // Расстояния между всеми парами
        double d12 = distance(t1Cell, t2Cell);
        double d13 = distance(t1Cell, t3Cell);
        double d23 = distance(t2Cell, t3Cell);
        
        // Расстояния от сбитой до побочных
        int hitCell = (hitIndex == 0) ? t1Cell : (hitIndex == 1) ? t2Cell : t3Cell;
        int miss1Cell = (hitIndex == 0) ? t2Cell : (hitIndex == 1) ? t1Cell : t1Cell;
        int miss2Cell = (hitIndex == 0) ? t3Cell : (hitIndex == 1) ? t3Cell : t2Cell;
        
        g.hitToMiss1Dist = distance(hitCell, miss1Cell);
        g.hitToMiss2Dist = distance(hitCell, miss2Cell);
        g.miss1ToMiss2Dist = d23;
        
        // Spread — максимальное расстояние между любыми двумя
        g.spread = Math.max(d12, Math.max(d13, d23));
        
        return g;
    }
    
    private static double angle(int cell, double centroidRow, double centroidCol) {
        int[] rc = GridUtils.cellToRowCol(cell);
        return Math.atan2(rc[0] - centroidRow, rc[1] - centroidCol);
    }
    
    private static double distance(int cell1, int cell2) {
        int[] a = GridUtils.cellToRowCol(cell1);
        int[] b = GridUtils.cellToRowCol(cell2);
        return Math.hypot(a[0] - b[0], a[1] - b[1]);
    }
}