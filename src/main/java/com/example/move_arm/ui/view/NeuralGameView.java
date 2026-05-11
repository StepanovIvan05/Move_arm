package com.example.move_arm.ui.view;

import java.util.List;
import java.util.function.Consumer;

import javafx.scene.paint.Color;

/**
 * Расширение GameView для RL-режима.
 * Добавляет грид-координаты и полные данные о жизни цели.
 */
public interface NeuralGameView extends GameView {
    
    /**
     * Спавн цели с грид-индексом (для сбора данных)
     */
    void addTargetWithCell(double x, double y, int radius, Color color, int cellIndex);
    
    /**
     * Получить активные цели с грид-индексами и временем спавна
     */
    List<TargetCell> getActiveTargetsWithCells();
    
    /**
     * Установить обработчик для RL-события (с lifetimeNs)
     */
    void setOnNeuralTargetHit(Consumer<NeuralHitEvent> handler);
    
    /**
     * Записать исчезновение цели по timeout (не сбита)
     */
    void recordTargetTimeout(int cellIndex, long timeoutNs);
    
    /**
     * DTO: цель с грид-индексом и временем спавна
     */
    record TargetCell(double x, double y, int cellIndex, long spawnTimeNs) {}
}