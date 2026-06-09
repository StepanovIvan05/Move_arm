package com.example.move_arm.model.settings;

import com.example.move_arm.model.GeneratorType;
import com.example.move_arm.model.TrajectoryDifficulty;

public class HoverGameSettings extends BaseSettings{
    private TrajectoryDifficulty difficulty = TrajectoryDifficulty.MEDIUM;
    private GeneratorType generatorType = GeneratorType.ADAPTIVE;

    public HoverGameSettings() {}

    public TrajectoryDifficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(TrajectoryDifficulty difficulty) {
        this.difficulty = difficulty == null ? TrajectoryDifficulty.MEDIUM : difficulty;
    }

    public GeneratorType getGeneratorType() {
        return generatorType;
    }

    public void setGeneratorType(GeneratorType generatorType) {
        this.generatorType = generatorType == null ? GeneratorType.ADAPTIVE : generatorType;
    }
}
