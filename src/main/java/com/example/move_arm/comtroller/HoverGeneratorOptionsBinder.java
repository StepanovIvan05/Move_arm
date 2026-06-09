package com.example.move_arm.comtroller;

import com.example.move_arm.model.GeneratorType;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class HoverGeneratorOptionsBinder {

    private final Map<GeneratorType, Region> optionContainers = new EnumMap<>(GeneratorType.class);

    HoverGeneratorOptionsBinder register(GeneratorType type, Region container) {
        optionContainers.put(type, container);
        hide(container);
        return this;
    }

    void showOptionsFor(GeneratorType type) {
        optionContainers.values().forEach(HoverGeneratorOptionsBinder::hide);

        Region selectedContainer = optionContainers.get(type);
        if (selectedContainer != null) {
            selectedContainer.setVisible(true);
            selectedContainer.setManaged(true);
        }
    }

    List<GeneratorType> getSupportedTypes() {
        return new ArrayList<>(optionContainers.keySet());
    }

    GeneratorType normalizeType(GeneratorType type) {
        if (optionContainers.containsKey(type)) {
            return type;
        }

        return optionContainers.keySet().stream().findFirst().orElse(null);
    }

    boolean usesSeed(GeneratorType type) {
        return type == GeneratorType.RANDOM;
    }

    boolean usesDifficulty(GeneratorType type) {
        return type == GeneratorType.ADAPTIVE;
    }

    private static void hide(Region container) {
        container.setVisible(false);
        container.setManaged(false);
    }
}
