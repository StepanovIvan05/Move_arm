package com.example.move_arm.comtroller;

import com.example.move_arm.model.settings.BaseSettings;
import com.example.move_arm.model.settings.HoldGameSettings;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

public class HoldSettingsController extends BaseSettingsController {

    @FXML private Slider holdTimeSlider;
    @FXML private Label holdTimeValueLabel;


    private HoldGameSettings holdSettings;

    @Override
    protected BaseSettings getGameSettings() {
        holdSettings = settingsService.getHoldSettings();
        return holdSettings;
    }

    @Override
    protected void initializeSpecific() {
        
        // Время удержания: 100–2000 мс, шаг 100
        holdTimeSlider.setMin(100);
        holdTimeSlider.setMax(2000);
        holdTimeSlider.setMajorTickUnit(100);
        holdTimeSlider.setMinorTickCount(0);
        holdTimeSlider.setSnapToTicks(true);
        holdTimeSlider.setShowTickLabels(true);
        holdTimeSlider.setBlockIncrement(100);

        holdTimeSlider.setValue(holdSettings.getHoldTimeMs());
        updateHoldTimeLabel(holdSettings.getHoldTimeMs());

        holdTimeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int snapped = ((int) Math.round(newVal.doubleValue() / 100)) * 100;
            holdTimeSlider.setValue(snapped);
            updateHoldTimeLabel(snapped);
        });
    }

    @Override
    protected void saveSpecificSettings() {
        holdSettings.setHoldTimeMs((int) holdTimeSlider.getValue());
    }

    private void updateHoldTimeLabel(int value) {
        holdTimeValueLabel.setText(value + " мс");
    }
}