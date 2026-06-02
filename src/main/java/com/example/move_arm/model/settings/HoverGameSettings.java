package com.example.move_arm.model.settings;

import com.example.move_arm.model.TrajectoryDifficulty;

public class HoverGameSettings extends BaseSettings{
    private TrajectoryDifficulty difficulty = TrajectoryDifficulty.MEDIUM;

    public HoverGameSettings() {}

    public TrajectoryDifficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(TrajectoryDifficulty difficulty) {
        this.difficulty = difficulty == null ? TrajectoryDifficulty.MEDIUM : difficulty;
    }
}
