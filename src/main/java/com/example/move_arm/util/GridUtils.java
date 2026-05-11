package com.example.move_arm.util;

/**
 * Утилиты для работы с гридом 12×8 = 96 ячеек.
 */
public class GridUtils {
    
    public static final int COLS = 12;
    public static final int ROWS = 8;
    public static final int CELLS = COLS * ROWS; // 96
    
    /**
     * x,y (пиксели) → cell (0-95)
     */
    public static int xyToCell(double x, double y, double screenW, double screenH) {
        int col = (int) (x / screenW * COLS);
        int row = (int) (y / screenH * ROWS);
        col = Math.min(col, COLS - 1);
        row = Math.min(row, ROWS - 1);
        return row * COLS + col;
    }
    
    /**
     * cell → x,y (центр ячейки)
     */
    public static double[] cellToXy(int cell, double screenW, double screenH) {
        int row = cell / COLS;
        int col = cell % COLS;
        double x = (col + 0.5) * screenW / COLS;
        double y = (row + 0.5) * screenH / ROWS;
        return new double[]{x, y};
    }
    
    /**
     * cell → [row, col]
     */
    public static int[] cellToRowCol(int cell) {
        return new int[]{cell / COLS, cell % COLS};
    }
}