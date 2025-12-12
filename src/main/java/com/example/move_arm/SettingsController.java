package com.example.move_arm;

import com.example.move_arm.model.AnimationType;
import com.example.move_arm.model.settings.HoverGameSettings;
import com.example.move_arm.service.SettingsService;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.shape.Circle;

public class SettingsController {

    @FXML private Slider radiusSlider;
    @FXML private Label radiusValueLabel;
    @FXML private Circle previewCircle;
    @FXML private ComboBox<AnimationType> animationTypeComboBox; // Добавляем ComboBox для анимаций

    private SceneManager sceneManager;
    private HoverGameSettings settings;

    public void setSceneManager(SceneManager manager) {
        this.sceneManager = manager;
    }

    @FXML
    public void initialize() {
        settings = SettingsService.getInstance().getHoverSettings();

        // Устанавливаем диапазон и шаг слайдера
        radiusSlider.setMin(20);
        radiusSlider.setMax(100);
        radiusSlider.setBlockIncrement(10);
        radiusSlider.setMajorTickUnit(10);
        radiusSlider.setMinorTickCount(0);
        radiusSlider.setSnapToTicks(true);

        // === СУЩЕСТВУЮЩАЯ ЛОГИКА ДЛЯ РАДИУСА ===
        radiusSlider.setValue(settings.getMinRadius());
        updateLabel((int) settings.getMinRadius());

        previewCircle.radiusProperty().bind(radiusSlider.valueProperty());

        radiusSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int rounded = ((int) Math.round(newVal.doubleValue() / 10)) * 10;
            radiusSlider.setValue(rounded); // "прилипание" к шагам 20,30,...
            updateLabel(rounded);
        });

        radiusSlider.setOnMousePressed(event -> {
            previewCircle.setVisible(true);
        });

        radiusSlider.setOnMouseReleased(event -> {
            previewCircle.setVisible(false);
        });

        // === НОВАЯ ЛОГИКА ДЛЯ ВЫБОРА АНИМАЦИИ ===
        
        // Заполняем ComboBox всеми доступными типами анимаций
        animationTypeComboBox.getItems().setAll(AnimationType.values());
        
        // Устанавливаем текущее значение из настроек
        animationTypeComboBox.setValue(settings.getAnimationType());
        
        // Опционально: можно добавить подсказку при выборе
        animationTypeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // Можно добавить логику для показа предпросмотра анимации
                AppLogger.info("SettingsController: Выбрана анимация: " + newVal.getDisplayName());
            }
        });
    }

    private void updateLabel(int value) {
        radiusValueLabel.setText(value + " px");
    }

    @FXML
    private void handleSaveAndExit() {
        // Сохраняем радиус (существующая логика)
        int newRadius = ((int) Math.round(radiusSlider.getValue() / 10)) * 10;
        settings.setRadius(newRadius);
        
        // Сохраняем выбранный тип анимации (новая логика)
        AnimationType selectedAnimation = animationTypeComboBox.getValue();
        if (selectedAnimation != null) {
            settings.setAnimationType(selectedAnimation);
        }

        SettingsService.getInstance().saveHoverSettings(settings);
        AppLogger.info("SettingsController: Настройки сохранены - радиус: " + newRadius + 
                      ", анимация: " + (selectedAnimation != null ? selectedAnimation.getDisplayName() : "не выбрана"));
        
        sceneManager.showMenu();
    }
}