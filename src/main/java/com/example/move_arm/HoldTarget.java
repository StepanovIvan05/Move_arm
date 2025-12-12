package com.example.move_arm;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class HoldTarget extends StackPane {

    private final Circle hitbox;
    private final Arc visualArc;
    private final Timeline holdTimer;
    private boolean isCompleted = false;
    private final double radius;

    public HoldTarget(double radius, Color color, double holdDurationSeconds, Runnable onHoldComplete) {
        this.radius = radius;
        
        // Визуальная дуга (анимация по часовой стрелке)
        visualArc = new Arc(0, 0, radius, radius, 90, -360);
        visualArc.setType(ArcType.ROUND);
        visualArc.setFill(color);
        visualArc.setStroke(Color.WHITE);
        visualArc.setStrokeWidth(2);
        
        // Хитбокс
        hitbox = new Circle(radius);
        hitbox.setFill(Color.TRANSPARENT);
        hitbox.setStroke(Color.TRANSPARENT);
        hitbox.setPickOnBounds(true);

        getChildren().addAll(visualArc, hitbox);

        // Таймер с плавной анимацией
        holdTimer = new Timeline();
        holdTimer.setCycleCount(1);
        
        KeyValue kv = new KeyValue(
            visualArc.lengthProperty(), 
            0, 
            Interpolator.EASE_BOTH
        );
        KeyFrame kf = new KeyFrame(Duration.seconds(holdDurationSeconds), kv);
        holdTimer.getKeyFrames().add(kf);

        // Завершение удержания
        holdTimer.setOnFinished(e -> {
            if (!isCompleted) {
                isCompleted = true;
                hitbox.setMouseTransparent(true);
                if (onHoldComplete != null) {
                    onHoldComplete.run();
                }
            }
        });

        // Обработчики мыши
        hitbox.setOnMouseEntered(e -> {
            if (!isCompleted && holdTimer.getStatus() != javafx.animation.Animation.Status.RUNNING) {
                holdTimer.playFromStart();
            }
        });

        hitbox.setOnMouseExited(e -> {
            if (!isCompleted) {
                holdTimer.stop();
                visualArc.setLength(-360);
            }
        });
    }
    
    public double getRadius() {
        return radius;
    }
}