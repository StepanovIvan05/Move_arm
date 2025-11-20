package com.example.move_arm.service;

import com.example.move_arm.model.settings.HoverGameSettings;
// import com.example.move_arm.model.settings.ClickGameSettings; // В будущем

public class SettingsService {

    private static final SettingsService INSTANCE = new SettingsService();

    // Имитация базы данных (кэш настроек)
    private HoverGameSettings hoverSettings;
    
    // private ClickGameSettings clickSettings; // В будущем

    private SettingsService() {
        // При первом запуске создаем дефолтные настройки
        // В будущем здесь будет: hoverRepository.findById(1).orElse(new HoverGameSettings());
        this.hoverSettings = new HoverGameSettings();
    }

    public static SettingsService getInstance() {
        return INSTANCE;
    }

    // === Методы доступа ===

    /**
     * Получить настройки для игры Hover (Move Arm).
     * Возвращает конкретную сущность, а не абстракцию.
     */
    public HoverGameSettings getHoverSettings() {
        return hoverSettings;
    }

    // Метод для сохранения (в будущем commit transaction)
    public void saveHoverSettings(HoverGameSettings newSettings) {
        this.hoverSettings = newSettings;
        // repo.save(newSettings);
        System.out.println("SettingsService: Настройки Hover обновлены");
    }
}