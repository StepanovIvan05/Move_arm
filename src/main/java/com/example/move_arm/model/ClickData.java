// model/ClickData.java
package com.example.move_arm.model;

import javafx.geometry.Point2D;

public class ClickData {
    private final long clickTimeNs;
    private final Point2D cursor;
    private final Point2D center;
    private final int radius;

    public ClickData(long clickTimeNs, double cursorX, double cursorY,
                     double centerX, double centerY, int radius) {
        this.clickTimeNs = clickTimeNs;
        this.cursor = new Point2D(cursorX, cursorY);
        this.center = new Point2D(centerX, centerY);
        this.radius = radius;
    }

    // Только геттеры — НИКАКИХ ВЫЧИСЛЕНИЙ!
    public long getClickTimeNs() { return clickTimeNs; }
    public Point2D getCursor() { return cursor; }
    public Point2D getCenter() { return center; }
    public int getRadius() { return radius; }

    @Override
    public String toString() {
        return String.format("t=%.3fs | Курсор: %s | Центр: %s | R=%s",
            clickTimeNs / 1_000_000_000.0, cursor, center, radius);
    }
}