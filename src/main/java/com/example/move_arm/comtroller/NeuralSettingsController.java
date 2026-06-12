package com.example.move_arm.comtroller;

import com.example.move_arm.model.AnimationType;
import com.example.move_arm.model.settings.BaseSettings;
import com.example.move_arm.model.settings.NeuralGameSettings;

public class NeuralSettingsController extends BaseSettingsController {

    @Override
    protected BaseSettings getGameSettings() {
        return settingsService.getNeuralSettings();
    }

    @Override
    protected void initializeSpecific() {
        // нет специфических настроек
    }

    @Override
    protected void saveSpecificSettings() {
        // нечего сохранять
    }
}
