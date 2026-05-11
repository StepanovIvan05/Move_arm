package com.example.move_arm.service;

import com.example.move_arm.database.DatabaseManager;
import com.example.move_arm.model.GameType;

public class GameTypeService {

    private final DatabaseManager dbManager = DatabaseManager.getInstance();
    private GameType currentGameType;

    public GameType getCurrentGameType() {
        return currentGameType;
    }

    public void setCurrentGameType(GameType type) {
        if (type == null) return;

        this.currentGameType = type;

        try {
            dbManager.setAppProperty("last_game_type_id", String.valueOf(type.getId()));
        } catch (Exception ignored) {}
    }

    public int getCurrentGameTypeId() {
        return currentGameType.getId();
    }

    public String getCurrentGameTypeString() {
        return currentGameType.getName();
    }
    
    public void setCurrentGameTypeString(String typeName) {
        if (typeName == null) return;
        
        // Создаём "виртуальный" GameType для neural
        if ("neural".equalsIgnoreCase(typeName)) {
            this.currentGameType = new GameType(-1, "neural", "Neural RL Training Mode");
            try {
                dbManager.setAppProperty("last_game_type_id", "-1");
                dbManager.setAppProperty("last_game_type_name", "neural");
            } catch (Exception ignored) {}
            return;
        }
    }

}