package com.example.move_arm.service;

import com.example.move_arm.model.GeneratorType;

public class GeneratorFactory {

    private GeneratorFactory() {
    }

    public static PointGenerator createGenerator(GeneratorType type) {
        return switch (type) {
            case RANDOM -> LevelGeneratorService.getInstance();
            case ADAPTIVE -> VectorTrajectoryGenerator.getInstance();
        };
    }
}
