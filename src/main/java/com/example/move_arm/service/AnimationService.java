// com.example.move_arm.service.AnimationService.java
package com.example.move_arm.service;

import com.example.move_arm.DestroyAnimationService;
import com.example.move_arm.model.AnimationType;
import com.example.move_arm.model.settings.HoverGameSettings;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

public class AnimationService {

    /**
     * Воспроизводит анимацию уничтожения круга в соответствии с настройками
     */
    public static void playDestructionAnimation(Pane root, Circle circle, Runnable onFinish) {
        HoverGameSettings settings = SettingsService.getInstance().getHoverSettings();
        AnimationType animationType = settings.getAnimationType();
        
        playAnimationByType(animationType, root, circle, onFinish);
    }

    /**
     * Воспроизводит конкретный тип анимации
     */
    public static void playAnimationByType(AnimationType type, Pane root, Circle circle, Runnable onFinish) {
        switch (type) {
            case SIMPLE:
                DestroyAnimationService.playSimple(root, circle, onFinish);
                break;
            case EXPLOSION:
                DestroyAnimationService.playExplosion(root, circle, onFinish);
                break;
            case CRAZY_EXPLOSION:
                DestroyAnimationService.playCrazyExplosion(root, circle, onFinish);
                break;
            case GRAVITY_FALL:
                DestroyAnimationService.playGravityFall(root, circle, onFinish);
                break;
            case AREA_GRAVITY_FALL:
                DestroyAnimationService.playAreaGravityFall(root, circle, onFinish);
                break;
            case COLORFUL_AREA_FALL:
                DestroyAnimationService.playColorfulAreaFall(root, circle, onFinish);
                break;
            case INSANE_EXPLOSION:
                DestroyAnimationService.playInsaneExplosion(root, circle, onFinish);
                break;
            default:
                DestroyAnimationService.playSimple(root, circle, onFinish);
        }
    }
}