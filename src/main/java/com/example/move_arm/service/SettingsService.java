package com.example.move_arm.service;

import com.example.move_arm.database.GlobalSettingsDao;
import com.example.move_arm.database.HoldSettingsDao;
import com.example.move_arm.database.HoverSettingsDao;
import com.example.move_arm.database.NeuralSettingsDao;
import com.example.move_arm.model.AnimationType;
import com.example.move_arm.model.settings.GlobalSettings;
import com.example.move_arm.model.settings.HoldGameSettings;
import com.example.move_arm.model.settings.HoverGameSettings;
import com.example.move_arm.model.settings.NeuralGameSettings;

public class SettingsService {

    private static final SettingsService INSTANCE = new SettingsService();

    private final GlobalSettingsDao globalDao = new GlobalSettingsDao();
    private final HoverSettingsDao hoverDao = new HoverSettingsDao();
    private final HoldSettingsDao holdDao = new HoldSettingsDao();
    private final NeuralSettingsDao neuralDao = new NeuralSettingsDao();

    private GlobalSettings globalSettings;
    private HoverGameSettings hoverSettings;
    private HoldGameSettings holdSettings;
    private NeuralGameSettings neuralSettings;

    private long getCurrentUserId() {
        return UserService.getInstance().getCurrentUser().getId();
    }

    private SettingsService() {
        loadAll();
    }

    public void reload() {
        loadAll();
    }

    public static SettingsService getInstance() {
        return INSTANCE;
    }

    private void loadAll() {
        long userId = getCurrentUserId();
        ensureDefaultsSaved(userId);

        globalSettings = globalDao.load(userId);
        hoverSettings = hoverDao.load(userId);
        holdSettings = holdDao.load(userId);
        neuralSettings = neuralDao.load(userId);
    }

    public void ensureDefaultsSaved(long userId) {
        if (!globalDao.exists(userId)) {
            globalDao.save(userId, new GlobalSettings());
        }

        if (!hoverDao.exists(userId)) {
            hoverDao.save(userId, new HoverGameSettings());
        }

        if (!holdDao.exists(userId)) {
            holdDao.save(userId, new HoldGameSettings());
        }

        if (!neuralDao.exists(userId)) {
            neuralDao.save(userId, new NeuralGameSettings());
        }
    }

    // ===== GET =====

    public HoverGameSettings getHoverSettings() {
        return hoverSettings;
    }

    public HoldGameSettings getHoldSettings() {
        return holdSettings;
    }

    public NeuralGameSettings getNeuralSettings() {
        return neuralSettings;
    }

    public AnimationType getAnimationType() {
        return globalSettings.getAnimationType();
    }
    
    public void setAnimationType(AnimationType type) {
        globalSettings.setAnimationType(type);
    }

    // ===== SAVE =====

    public void saveAll() {
        globalDao.save(getCurrentUserId(), globalSettings);
        hoverDao.save(getCurrentUserId(), hoverSettings);
        holdDao.save(getCurrentUserId(), holdSettings);
        neuralDao.save(getCurrentUserId(), neuralSettings);
    }
}
