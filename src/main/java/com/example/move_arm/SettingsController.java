package com.example.move_arm;

import com.example.move_arm.model.settings.HoverGameSettings;
import com.example.move_arm.service.SettingsService;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.shape.Circle;

public class SettingsController {

    @FXML private Slider radiusSlider;
    @FXML private Label radiusValueLabel;
    @FXML private Circle previewCircle; // Наш круг из FXML

    private SceneManager sceneManager;
    private HoverGameSettings settings;

    public void setSceneManager(SceneManager manager) {
        this.sceneManager = manager;
    }

    @FXML
    public void initialize() {
        settings = SettingsService.getInstance().getHoverSettings();

        // Устанавливаем начальное значение слайдера
        radiusSlider.setValue(settings.getMinRadius());
        updateLabel((int) settings.getMinRadius());

        // === ЛОГИКА ПРЕДПРОСМОТРА ===

        // 1. Связываем радиус круга со значением слайдера
        // (Круг будет менять размер всегда, даже когда невидим)
        previewCircle.radiusProperty().bind(radiusSlider.valueProperty());

        // 2. Обновляем текст цифрами
        radiusSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateLabel(newVal.intValue());
        });

        // 3. ПОКАЗАТЬ круг, когда начали тянуть слайдер
        radiusSlider.setOnMousePressed(event -> {
            previewCircle.setVisible(true);
        });

        // 4. СКРЫТЬ круг, когда отпустили мышь
        radiusSlider.setOnMouseReleased(event -> {
            previewCircle.setVisible(false);
        });
        
        // Опционально: Скрывать, если курсор ушел со слайдера (на всякий случай)
        radiusSlider.setOnMouseExited(event -> {
            // Можно раскомментировать, если хотите, чтобы круг исчезал, если мышь соскочила,
            // но обычно Released достаточно.
            // previewCircle.setVisible(false); 
        });
    }

    private void updateLabel(int value) {
        radiusValueLabel.setText(value + " px");
    }

    @FXML
    private void handleSaveAndExit() {
        double newRadius = radiusSlider.getValue();
        settings.setRadius(newRadius);

        SettingsService.getInstance().saveHoverSettings(settings);
        AppLogger.info("SettingsController: Радиус сохранен: " + newRadius);
        
        sceneManager.startNewGame();
    }
}