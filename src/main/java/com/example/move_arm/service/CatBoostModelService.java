package com.example.move_arm.service;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.example.move_arm.util.AppLogger;

import ai.catboost.CatBoostModel;
import ai.catboost.CatBoostPredictions;

/**
 * Сервис для управления нативной моделью CatBoost через JNI.
 * Загружает бинарный файл .cbm из ресурсов и выполняет потокобезопасный инференс.
 */
public class CatBoostModelService {

    private static CatBoostModelService instance;
    private CatBoostModel model;
    private boolean isModelReady = false;

    private CatBoostModelService() {
        try {
            // Относительный путь: поднимаемся из пакета service и заходим в соседнюю папку model
            String modelResourcePath = "../model/aim_catboost_model.cbm";
            InputStream modelStream = CatBoostModelService.class.getResourceAsStream(modelResourcePath);

            if (modelStream == null) {
                // Резервный абсолютный путь от корня ресурсов, если относительный не подтянется
                modelResourcePath = "/com/example/move_arm/model/aim_catboost_model.cbm";
                modelStream = CatBoostModelService.class.getResourceAsStream(modelResourcePath);
            }

            if (modelStream == null) {
                AppLogger.error("CatBoostModelService: Файл модели 'aim_catboost_model.cbm' не найден в ресурсах проекта!");
                return;
            }

            // Создаем временный файл на диске, так как нативный C++ движок CatBoost работает только с путями файловой системы
            File tempModelFile = File.createTempFile("aim_catboost_model", ".cbm");
            tempModelFile.deleteOnExit(); 

            // Копируем байты из JAR/ресурсов во временный файл
            Files.copy(modelStream, tempModelFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            modelStream.close();

            String absolutePath = tempModelFile.getAbsolutePath();
            AppLogger.info("CatBoostModelService: Модель успешно извлечена во временный файл: " + absolutePath);

            // Инициализируем нативный контекст модели
            this.model = CatBoostModel.loadModel(absolutePath);
            this.isModelReady = true;
            AppLogger.info("CatBoostModelService: Модель CatBoost успешно загружена и готова к инференсу.");

        } catch (Throwable t) {
            AppLogger.error("CatBoostModelService: Критическая ошибка при инициализации JNI CatBoost", t);
            this.isModelReady = false;
        }
    }

    /**
     * Возвращает синглтон-экземпляр сервиса.
     */
    public static synchronized CatBoostModelService getInstance() {
        if (instance == null) {
            instance = new CatBoostModelService();
        }
        return instance;
    }

    /**
     * Проверка готовности модели (загружена ли она без ошибок).
     */
    public boolean isModelReady() {
        return isModelReady;
    }

    /**
     * Выполняет инференс модели по переданным признакам.
     * * @param catFeatures Массив строковых категориальных признаков [t1_cell, t2_cell, t3_cell, previous_hit_cell]
     * @param numFeatures Массив вещественных признаков согласно feature_columns из Python
     * @return Предсказанное время отклика (TTK) в миллисекундах
     */
    public double predict(String[] catFeatures, float[] numFeatures) {
        if (!isModelReady) {
            return 450.0; // Безопасный дефолт (средний ТТК игрока), если модель не готова
        }
        try {
            // Нативная библиотека требует двумерные массивы для батч-предсказаний.
            // Оборачиваем наши одномерные массивы в матрицы размера [1][N]
            float[][] numFeaturesMatrix = new float[][]{ numFeatures };
            String[][] catFeaturesMatrix = new String[][]{ catFeatures };

            // Вызов метода predict. Порядок аргументов в Java SDK: (вещественные, категориальные)
            CatBoostPredictions predictions = model.predict(numFeaturesMatrix, catFeaturesMatrix);
            
            // Забираем значение для нулевого (единственного) элемента в батче
            return predictions.get(0, 0);

        } catch (Exception e) {
            AppLogger.error("CatBoostModelService: Ошибка при расчете предсказания внутри нативного кода", e);
            return 450.0; // Защита от вылета игры: возвращаем средний темп
        }
    }
}