package com.example.move_arm;

import com.example.move_arm.model.AnimationType;
import com.example.move_arm.model.settings.HoverGameSettings;
import com.example.move_arm.service.AnimationService;
import com.example.move_arm.service.SettingsService;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class SettingsController {

    @FXML private Slider radiusSlider;
    @FXML private Label radiusValueLabel;
    @FXML private ComboBox<AnimationType> animationTypeComboBox;

    @FXML private Pane previewRoot;
    @FXML private Circle previewCircle;

    private SceneManager sceneManager;
    private HoverGameSettings settings;

    public void setSceneManager(SceneManager manager) {
        this.sceneManager = manager;
    }

    @FXML
    public void initialize() {
        settings = SettingsService.getInstance().getHoverSettings();

        // =========================
        // 🔘 РАДИУС (ТОЛЬКО ДИСКРЕТНЫЙ)
        // =========================
        radiusSlider.setMin(20);
        radiusSlider.setMax(100);
        radiusSlider.setMajorTickUnit(10);
        radiusSlider.setMinorTickCount(0);
        radiusSlider.setSnapToTicks(true);

        radiusSlider.setValue(settings.getRadius());
        previewCircle.setRadius(settings.getRadius());
        updateRadiusLabel(settings.getRadius());

        radiusSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int snapped = ((int) Math.round(newVal.doubleValue() / 10)) * 10;
            radiusSlider.setValue(snapped);
            previewCircle.setRadius(snapped);
            updateRadiusLabel(snapped);
            centerCircle();
        });

        // =========================
        // 🎬 АНИМАЦИИ
        // =========================
        animationTypeComboBox.getItems().setAll(AnimationType.values());
        animationTypeComboBox.setValue(settings.getAnimationType());

        // =========================
        // 🎯 ЦЕНТРИРОВАНИЕ PREVIEW
        // =========================
        previewRoot.layoutBoundsProperty().addListener((obs, o, n) -> centerCircle());
        centerCircle();
    }

    // =========================
    // ▶ ПРОИГРЫВАНИЕ АНИМАЦИИ
    // =========================
    @FXML
    private void handlePlayAnimation() {
        AnimationType type = animationTypeComboBox.getValue();
        if (type == null) return;

        // Гарантируем корректное состояние
        previewCircle.setOpacity(1);
        previewCircle.setScaleX(1);
        previewCircle.setScaleY(1);
        previewCircle.setRadius(radiusSlider.getValue());
        centerCircle();

        // ❗ Используем ТОТ ЖЕ круг, как в игре
        AnimationService.playAnimationByType(
                type,
                previewRoot,
                previewCircle,
                this::schedulePreviewRestore
        );
    }

    // =========================
    // ⏳ ВОЗВРАТ КРУГА С ЗАДЕРЖКОЙ
    // =========================
    private void schedulePreviewRestore() {

        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(e -> {

            // Если круг был удалён анимацией — возвращаем
            if (!previewRoot.getChildren().contains(previewCircle)) {
                previewRoot.getChildren().add(previewCircle);
            }

            previewCircle.setRadius(radiusSlider.getValue());
            previewCircle.setOpacity(1);
            previewCircle.setScaleX(1);
            previewCircle.setScaleY(1);

            centerCircle();
        });

        delay.play();
    }

    // =========================
    // 💾 СОХРАНЕНИЕ И ВЫХОД
    // =========================
    @FXML
    private void handleSaveAndExit() {
        settings.setRadius((int) radiusSlider.getValue());
        settings.setAnimationType(animationTypeComboBox.getValue());

        SettingsService.getInstance().saveHoverSettings(settings);
        sceneManager.showMenu();
    }

    // =========================
    // 🧭 ВСПОМОГАТЕЛЬНОЕ
    // =========================
    private void centerCircle() {
        previewCircle.setCenterX(previewRoot.getWidth() / 2);
        previewCircle.setCenterY(previewRoot.getHeight() / 2);
    }

    private void updateRadiusLabel(int value) {
        radiusValueLabel.setText(value + " px");
    }
}
