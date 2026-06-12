package com.example.move_arm.comtroller;

import com.example.move_arm.model.AnimationType;
import com.example.move_arm.model.settings.BaseSettings;
import com.example.move_arm.service.AnimationService;
import com.example.move_arm.service.SettingsService;
import com.example.move_arm.ui.SceneManager;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public abstract class BaseSettingsController {

    // ===== Общие FXML-поля (должны присутствовать во всех FXML-наследниках) =====
    @FXML protected Slider radiusSlider;
    @FXML protected Label radiusValueLabel;
    @FXML protected ComboBox<AnimationType> animationTypeComboBox;

    @FXML protected Pane previewRoot;
    @FXML protected Circle previewCircle;

    protected SceneManager sceneManager;
    protected SettingsService settingsService = SettingsService.getInstance();
    protected BaseSettings settings;

    public void setSceneManager(SceneManager manager) {
        this.sceneManager = manager;
    }

    @FXML
    public void initialize() {
        settings = getGameSettings();

        // ---------- Радиус (дискретный, шаг 10) ----------
        radiusSlider.setMin(20);
        radiusSlider.setMax(100);
        radiusSlider.setMajorTickUnit(10);
        radiusSlider.setMinorTickCount(0);
        radiusSlider.setSnapToTicks(true);
        radiusSlider.setShowTickLabels(true);

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

        // ---------- Анимация ----------
        animationTypeComboBox.getItems().setAll(AnimationType.values());
        animationTypeComboBox.setValue(settingsService.getAnimationType());

        // ---------- Preview ----------
        previewRoot.layoutBoundsProperty().addListener((obs, o, n) -> centerCircle());
        centerCircle();

        // ---------- Точки расширения ----------
        initializeSpecific();
    }

    @FXML
    protected void handlePlayAnimation() {
        AnimationType type = animationTypeComboBox.getValue();
        if (type == null) return;

        previewCircle.setOpacity(1);
        previewCircle.setScaleX(1);
        previewCircle.setScaleY(1);
        previewCircle.setRadius(radiusSlider.getValue());
        centerCircle();

        AnimationService.playAnimationByType(
                type,
                previewRoot,
                previewCircle,
                this::schedulePreviewRestore
        );
    }

    @FXML
    protected void handleSaveAndExit() {
        settings.setRadius((int) radiusSlider.getValue());
        settingsService.setAnimationType(animationTypeComboBox.getValue());

        saveSpecificSettings();   // <-- наследник сохраняет своё

        settingsService.saveAll();
        sceneManager.showMenu();
    }

    protected void schedulePreviewRestore() {
        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(e -> {
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

    protected void centerCircle() {
        previewCircle.setCenterX(previewRoot.getWidth() / 2);
        previewCircle.setCenterY(previewRoot.getHeight() / 2);
    }

    protected void updateRadiusLabel(int value) {
        radiusValueLabel.setText(value + " px");
    }

    // ==================== Точки расширения для наследников ====================

    /** Вернуть конкретный объект настроек (Hover/Hold/...) */
    protected abstract BaseSettings getGameSettings();

    /** Инициализация специфичных контролов (слайдеров, полей и т.д.) */
    protected abstract void initializeSpecific();

    /** Сохранить специфичные поля в объект настроек перед записью в БД */
    protected abstract void saveSpecificSettings();
}