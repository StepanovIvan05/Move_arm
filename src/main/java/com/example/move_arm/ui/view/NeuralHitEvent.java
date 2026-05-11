package com.example.move_arm.ui.view;

// НОВЫЙ — только для NeuralGameView
public record NeuralHitEvent(
    // Базовые поля (совпадают с TargetHitEvent для удобства)
    double cursorX,
    double cursorY,
    double targetX,
    double targetY,
    int radius,
    long relativeTimeNs,
    
    // RL-специфичные (всегда заполнены, никаких null)
    long spawnTimeNs,         // когда появилась цель
    long lifetimeNs,          // сколько прожила (hitTime - spawnTime)
    int targetIndex,          // порядковый номер в игре
    int cellIndex             // ячейка грида 0-95
) {}