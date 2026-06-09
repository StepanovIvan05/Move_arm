package com.example.move_arm.service;

import com.example.move_arm.model.settings.HoverGameSettings;

public class GeneratorFactory {

    private GeneratorFactory() {
    }

    public static PointGenerator createGenerator(HoverGameSettings settings) {
        PointGenerator generator = switch (settings.getGeneratorType()) {
            case RANDOM -> {
                LevelGeneratorService service = LevelGeneratorService.getInstance();
                service.initialize(settings.getSeed());
                yield service;
            }
            case ADAPTIVE -> {
                VectorTrajectoryGenerator service = VectorTrajectoryGenerator.getInstance();
                service.setDifficulty(settings.getDifficulty());
                yield service;
            }
        };
        
        generator.reset();
        return generator;
    }
}
