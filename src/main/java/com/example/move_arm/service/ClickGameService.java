package com.example.move_arm.service;

import java.util.Collections;
import java.util.List;

import com.example.move_arm.database.ClickDao;
import com.example.move_arm.database.GameResultDao;
import com.example.move_arm.model.ClickData;
import com.example.move_arm.model.GameResult;
import com.example.move_arm.model.Statistics;
import com.example.move_arm.model.TrajectoryDifficulty;

public class ClickGameService {

    private final GameResultDao gameResultDao = new GameResultDao();
    private final ClickDao clickDao = new ClickDao();

    public int saveClicks(int userId, int gameTypeId, int radius, int seed, TrajectoryDifficulty difficulty, List<ClickData> clicks) {

        if (clicks == null) clicks = Collections.emptyList();

        GameResult result = new GameResult();

        result.setUserId(userId);
        result.setGameTypeId(gameTypeId);
        result.setRadius(radius);
        result.setSeed(seed);
        result.setDifficulty(difficulty);
        result.setScore(clicks.size());

        long durationMs = 0;

        if (clicks.size() >= 2) {
            long first = clicks.get(0).getClickTimeNs();
            long last = clicks.get(clicks.size() - 1).getClickTimeNs();
            durationMs = (last - first) / 1_000_000L;
        }

        result.setDurationMs(durationMs);

        result.setHitRate(Statistics.getHitRatePercent(clicks));
        result.setAvgIntervalMs(Statistics.getAverageClickIntervalMs(clicks));
        result.setAvgDistancePx(Statistics.getAverageCursorDistance(clicks));
        result.setAvgSpeed(Statistics.getAverageSpeedPxPerMs(clicks));

        int resultId = gameResultDao.insert(result);

        if (!clicks.isEmpty()) {
            clickDao.insertClicks(resultId, clicks);
        }

        return resultId;
    }
}
