// com.example.move_arm.model.AnimationType.java
package com.example.move_arm.model;

public enum AnimationType {
    SIMPLE("Простое исчезновение"),
    EXPLOSION("Взрыв"),
    CRAZY_EXPLOSION("Безумный взрыв"),
    GRAVITY_FALL("Падение с гравитацией"),
    AREA_GRAVITY_FALL("Рассыпание по площади"),
    COLORFUL_AREA_FALL("Цветное рассыпание"),
    INSANE_EXPLOSION("Эпичный взрыв");

    private final String displayName;

    AnimationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}