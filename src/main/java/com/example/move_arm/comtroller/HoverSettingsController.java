package com.example.move_arm.comtroller;

import com.example.move_arm.model.GeneratorType;
import com.example.move_arm.model.TrajectoryDifficulty;
import com.example.move_arm.model.settings.BaseSettings;
import com.example.move_arm.model.settings.HoverGameSettings;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

public class HoverSettingsController extends BaseSettingsController {
    @FXML private ComboBox<TrajectoryDifficulty> difficultyComboBox;
    @FXML private ComboBox<GeneratorType> generatorTypeComboBox;

    @Override
    protected BaseSettings getGameSettings() {
        return settingsService.getHoverSettings();
    }

    @Override
    protected void initializeSpecific() {
        HoverGameSettings hoverSettings = (HoverGameSettings) settings;
        difficultyComboBox.getItems().setAll(TrajectoryDifficulty.values());
        difficultyComboBox.setValue(hoverSettings.getDifficulty());
        
        generatorTypeComboBox.getItems().setAll(GeneratorType.values());
        generatorTypeComboBox.setValue(hoverSettings.getGeneratorType());
    }

    @Override
    protected void saveSpecificSettings() {
        ((HoverGameSettings) settings).setDifficulty(difficultyComboBox.getValue());
        ((HoverGameSettings) settings).setGeneratorType(generatorTypeComboBox.getValue());
    }
}
