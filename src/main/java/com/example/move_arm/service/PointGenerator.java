package com.example.move_arm.service;

import java.util.List;

public interface PointGenerator {
    // Метод возвращает [x, y]
    double[] nextPoint(double width, double height, int radius, List<double[]> activePoints);
    
    // Сброс внутреннего состояния генератора
    void reset();
}