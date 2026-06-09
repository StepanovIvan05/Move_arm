package com.example.move_arm.comtroller;

import com.example.move_arm.model.GeneratorType;
import com.example.move_arm.model.TrajectoryDifficulty;
import com.example.move_arm.model.settings.BaseSettings;
import com.example.move_arm.model.settings.HoverGameSettings;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class HoverSettingsController extends BaseSettingsController {

    @FXML private ComboBox<TrajectoryDifficulty> difficultyComboBox;
    @FXML private ComboBox<GeneratorType> generatorTypeComboBox;

    @FXML private StackPane generatorOptionsContainer;
    @FXML private VBox seedContainer;
    @FXML private VBox adaptiveContainer;

    private final HoverGeneratorOptionsBinder generatorOptionsBinder = new HoverGeneratorOptionsBinder();

    @Override
    protected BaseSettings getGameSettings() {
        return settingsService.getHoverSettings();
    }

    @Override
    protected void initializeSpecific() {
        HoverGameSettings hoverSettings = (HoverGameSettings) settings;

        generatorOptionsBinder
                .register(GeneratorType.RANDOM, seedContainer)
                .register(GeneratorType.ADAPTIVE, adaptiveContainer);

        // Тип генератора
        generatorTypeComboBox.getItems().setAll(generatorOptionsBinder.getSupportedTypes());
        generatorTypeComboBox.setValue(generatorOptionsBinder.normalizeType(hoverSettings.getGeneratorType()));

        // Сложность
        difficultyComboBox.getItems().setAll(TrajectoryDifficulty.values());
        difficultyComboBox.setValue(hoverSettings.getDifficulty());

        // Установка начального состояния контейнера
        generatorOptionsBinder.showOptionsFor(generatorTypeComboBox.getValue());

        // Слушатель переключения
        generatorTypeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            generatorOptionsBinder.showOptionsFor(newVal);
        });
    }

    @Override
    protected void saveSpecificSettings() {
        HoverGameSettings hoverSettings = (HoverGameSettings) settings;
        GeneratorType genType = generatorTypeComboBox.getValue();

        hoverSettings.setGeneratorType(genType);

        if (generatorOptionsBinder.usesDifficulty(genType)) {
            hoverSettings.setDifficulty(difficultyComboBox.getValue());
        } else if (generatorOptionsBinder.usesSeed(genType)) {
            hoverSettings.setSeed(seedComboBox.getValue());
        }
    }
}
