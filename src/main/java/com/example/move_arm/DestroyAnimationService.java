package com.example.move_arm;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class DestroyAnimationService {

    // =========================
    // ✅ ПРОСТОЕ ИСЧЕЗНОВЕНИЕ
    // =========================
    public static void playSimple(Pane root, Circle circle, Runnable onFinish) {

        // Полностью блокируем события
        circle.setMouseTransparent(true);
        circle.setOnMouseEntered(null);

        FadeTransition fade = new FadeTransition(Duration.millis(180), circle);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(180), circle);
        scale.setFromX(1);
        scale.setFromY(1);
        scale.setToX(0.1);
        scale.setToY(0.1);

        ParallelTransition anim = new ParallelTransition(fade, scale);

        anim.setOnFinished(e -> {
            root.getChildren().remove(circle);
            if (onFinish != null) onFinish.run();
        });

        anim.play();
    }

    // =========================
    // ✅ СТАБИЛЬНЫЙ "ВЗРЫВ"
    // =========================
    public static void playExplosion(Pane root, Circle circle, Runnable onFinish) {

        circle.setMouseTransparent(true);
        circle.setOnMouseEntered(null);

        double cx = circle.getCenterX();
        double cy = circle.getCenterY();
        Paint color = circle.getFill();

        root.getChildren().remove(circle);

        for (int i = 0; i < 12; i++) {
            Circle particle = new Circle(3, color);
            particle.setCenterX(cx);
            particle.setCenterY(cy);
            particle.setMouseTransparent(true);

            root.getChildren().add(particle);

            double angle = Math.random() * Math.PI * 2;
            double distance = 60 + Math.random() * 40;

            TranslateTransition move = new TranslateTransition(Duration.millis(300), particle);
            move.setByX(Math.cos(angle) * distance);
            move.setByY(Math.sin(angle) * distance);

            FadeTransition fade = new FadeTransition(Duration.millis(300), particle);
            fade.setFromValue(1);
            fade.setToValue(0);

            ParallelTransition anim = new ParallelTransition(move, fade);
            anim.setOnFinished(e -> root.getChildren().remove(particle));
            anim.play();
        }

        // ✅ Callback вызывается сразу — без тайминговых ловушек
        if (onFinish != null) onFinish.run();
    }

    public static void playCrazyExplosion(Pane root, Circle circle, Runnable onFinish) {

        // Полностью отключаем события
        circle.setOnMouseEntered(null);
        circle.setOnMouseClicked(null);
        circle.setMouseTransparent(true);

        double cx = circle.getCenterX();
        double cy = circle.getCenterY();
        double baseRadius = circle.getRadius();
        Paint color = circle.getFill();

        root.getChildren().remove(circle);

        // ==========================
        // ⚡ ВСПЫШКА
        // ==========================
        Circle flash = new Circle(baseRadius, color);
        flash.setCenterX(cx);
        flash.setCenterY(cy);
        flash.setMouseTransparent(true);
        root.getChildren().add(flash);

        ScaleTransition flashScale = new ScaleTransition(Duration.millis(120), flash);
        flashScale.setFromX(1);
        flashScale.setFromY(1);
        flashScale.setToX(2.2);
        flashScale.setToY(2.2);

        FadeTransition flashFade = new FadeTransition(Duration.millis(120), flash);
        flashFade.setFromValue(1);
        flashFade.setToValue(0);

        ParallelTransition flashAnim = new ParallelTransition(flashScale, flashFade);
        flashAnim.setOnFinished(e -> root.getChildren().remove(flash));
        flashAnim.play();

        // ==========================
        // 💥 ОСКОЛКИ (16 ШТУК)
        // ==========================
        for (int i = 0; i < 16; i++) {
            Circle particle = new Circle(3 + Math.random() * 3, color);
            particle.setCenterX(cx);
            particle.setCenterY(cy);
            particle.setMouseTransparent(true);

            root.getChildren().add(particle);

            double angle = Math.random() * Math.PI * 2;
            double distance = 80 + Math.random() * 60;

            TranslateTransition move = new TranslateTransition(Duration.millis(450), particle);
            move.setByX(Math.cos(angle) * distance);
            move.setByY(Math.sin(angle) * distance);

            FadeTransition fade = new FadeTransition(Duration.millis(450), particle);
            fade.setFromValue(1);
            fade.setToValue(0);

            ScaleTransition scale = new ScaleTransition(Duration.millis(450), particle);
            scale.setFromX(1);
            scale.setFromY(1);
            scale.setToX(0.2);
            scale.setToY(0.2);

            ParallelTransition anim = new ParallelTransition(move, fade, scale);
            anim.setOnFinished(e -> root.getChildren().remove(particle));
            anim.play();
        }

        if (onFinish != null) onFinish.run();
    }
}
