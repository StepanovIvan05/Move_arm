package com.example.move_arm;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

public class HoldTarget extends Pane {

    private final double radius;

    private final Circle baseCircle;   // цветной круг (хитбокс)
    private final Arc progressArc;     // белая дуга (индикатор)
    private final Timeline holdTimeline;

    private boolean completed = false;

    public HoldTarget(
            double radius,
            Color circleColor,
            double holdDurationSeconds,
            Runnable onHoldComplete
    ) {
        this.radius = radius;

        setPrefSize(radius * 2, radius * 2);

        // 🎯 Основной круг
        baseCircle = new Circle(radius, radius, radius);
        baseCircle.setFill(circleColor);
        baseCircle.setPickOnBounds(true);

        // ⚪ Дуга прогресса (изначально НЕТ)
        progressArc = new Arc();
        progressArc.setCenterX(radius);
        progressArc.setCenterY(radius);
        progressArc.setRadiusX(radius * 0.9);
        progressArc.setRadiusY(radius * 0.9);
        progressArc.setStartAngle(90);
        progressArc.setLength(0); // ❗ изначально пусто

        progressArc.setType(ArcType.OPEN);
        progressArc.setFill(Color.TRANSPARENT);
        progressArc.setStroke(Color.WHITE);
        progressArc.setStrokeWidth(radius * 0.22);
        progressArc.setStrokeLineCap(StrokeLineCap.ROUND);
        progressArc.setMouseTransparent(true);

        getChildren().addAll(baseCircle, progressArc);

        // ⏱ Таймер удержания: 0 → -360
        holdTimeline = new Timeline(
                new KeyFrame(
                        Duration.seconds(holdDurationSeconds),
                        new KeyValue(
                                progressArc.lengthProperty(),
                                -360,
                                Interpolator.EASE_BOTH
                        )
                )
        );
        holdTimeline.setCycleCount(1);

        holdTimeline.setOnFinished(e -> {
            if (completed) return;
            completed = true;
            baseCircle.setMouseTransparent(true);
            if (onHoldComplete != null) {
                onHoldComplete.run();
            }
        });

        // 🖱 Наведение
        baseCircle.setOnMouseEntered(e -> {
            if (!completed && holdTimeline.getStatus() != Timeline.Status.RUNNING) {
                holdTimeline.playFromStart();
            }
        });

        baseCircle.setOnMouseExited(e -> {
            if (completed) return;
            holdTimeline.stop();
            progressArc.setLength(0); // ❗ снова исчезает
        });
    }

    public double getRadius() {
        return radius;
    }

    public double getCenterX() {
        return getLayoutX() + radius;
    }

    public double getCenterY() {
        return getLayoutY() + radius;
    }
}
