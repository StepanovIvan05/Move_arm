package com.example.move_arm.service;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.example.move_arm.util.AppLogger;

import ai.catboost.CatBoostModel;
import ai.catboost.CatBoostPredictions;

public class CatBoostModelService {

    private static CatBoostModelService instance;
    private CatBoostModel model;
    private boolean isModelReady = false;

    private CatBoostModelService() {
        try {
            // Поднимаемся из пакета service на уровень выше и заходим в соседнюю папку model
            InputStream modelStream = CatBoostModelService.class.getResourceAsStream("../model/game_ttk_model.cbm");
            
            // Альтернативный (абсолютный) вариант от корня ресурсов, если относительный не сработает:
            // InputStream modelStream = CatBoostModelService.class.getResourceAsStream("/com/example/move_arm/model/game_ttk_model.cbm");

            if (modelStream == null) {
                AppLogger.error("CatBoostModelService: Файл модели не найден по пути ресурсов!");
                return;
            }

            // Создаем временный файл, так как нативный CatBoost требует физический путь на диске
            File tempModelFile = File.createTempFile("game_ttk_model", ".cbm");
            tempModelFile.deleteOnExit(); 

            // Копируем стрим во временный файл
            Files.copy(modelStream, tempModelFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            modelStream.close();

            String absolutePath = tempModelFile.getAbsolutePath();
            AppLogger.info("CatBoostModelService: Файл модели успешно извлечен во временный файл: " + absolutePath);

            // Передаем путь нативному C++ движку CatBoost
            this.model = CatBoostModel.loadModel(absolutePath);
            this.isModelReady = true;
            AppLogger.info("CatBoostModelService: Модель успешно инициализирована и готова к работе.");

        } catch (Throwable t) {
            AppLogger.error("CatBoostModelService: Ошибка при загрузке или инициализации модели", t);
            this.isModelReady = false;
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
            return 450.0; 
        }
        try {
            // Важно: CatBoost требует двумерные массивы для инференса батчей, 
            // поэтому оборачиваем одномерные массивы в матрицы [1][N]
            float[][] numFeaturesMatrix = new float[][]{ numFeatures };
            String[][] catFeaturesMatrix = new String[][]{ catFeatures };

            CatBoostPredictions predictions = model.predict(numFeaturesMatrix, catFeaturesMatrix);
            
            // Забираем значение для первого (и единственного) элемента в батче
            return predictions.get(0, 0);
        } catch (Exception e) {
            AppLogger.error("CatBoostModelService: Ошибка инференса", e);
            return 450.0;
        }
    }
}