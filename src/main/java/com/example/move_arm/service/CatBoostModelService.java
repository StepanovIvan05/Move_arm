package com.example.move_arm.service;

import java.io.InputStream;

import com.example.move_arm.util.AppLogger;

import ai.catboost.CatBoostModel;
import ai.catboost.CatBoostPredictions;

public class CatBoostModelService {

    private static CatBoostModelService instance;
    private CatBoostModel model;
    private boolean isModelReady = false;

    private CatBoostModelService() {
        try {
            // Файл должен лежать в src/main/resources/models/game_ttk_model.cbm
            InputStream modelStream = getClass().getClassLoader().getResourceAsStream("models/game_ttk_model.cbm");
            if (modelStream == null) {
                AppLogger.error("CatBoostModelService: Файл game_ttk_model.cbm не найден в resources!");
                return;
            }
            this.model = CatBoostModel.loadModel(modelStream);
            this.isModelReady = true;
            AppLogger.info("CatBoostModelService: Модель успешно инициализирована.");
        } catch (Exception e) {
            AppLogger.error("CatBoostModelService: Ошибка при загрузке модели", e);
        }
    }

    public static synchronized CatBoostModelService getInstance() {
        if (instance == null) {
            instance = new CatBoostModelService();
        }
        return instance;
    }

    public boolean isModelReady() {
        return isModelReady;
    }

    /**
     * Прямой инференс модели по сырым массивам фичей.
     */
    public double predict(String[] catFeatures, float[] numFeatures) {
        if (!isModelReady) {
            return 450.0; // Заглушка (дефолтный TTK), если модель не загрузилась
        }
        try {
            CatBoostPredictions predictions = model.predict(numFeatures, catFeatures);
            return predictions.get(0, 0);
        } catch (Exception e) {
            AppLogger.error("CatBoostModelService: Ошибка инференса", e);
            return 450.0;
        }
    }
}