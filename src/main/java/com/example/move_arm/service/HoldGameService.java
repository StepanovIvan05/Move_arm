package com.example.move_arm.service;

import java.util.List;

import com.example.move_arm.database.GameResultDao;
import com.example.move_arm.database.HoldAttemptDao;
import com.example.move_arm.model.GameResult;
import com.example.move_arm.model.HoldAttempt;
import com.example.move_arm.model.Statistics;
import com.example.move_arm.model.GeneratorType;

public class HoldGameService {

    private final GameResultDao gameResultDao = new GameResultDao();
    private final HoldAttemptDao holdAttemptDao = new HoldAttemptDao();

    public int saveHoldResults(int userId, int gameTypeId, int radius, int seed, List<HoldAttempt> attempts) {

        if (attempts == null || attempts.isEmpty()) return -1;

        long successCount = attempts.stream().filter(HoldAttempt::isSuccess).count();

        GameResult result = new GameResult();

        result.setUserId(userId);
        result.setGameTypeId(gameTypeId);
        result.setRadius(radius);
        result.setScore((int) successCount);
        result.setGeneratorType(GeneratorType.RANDOM);
        result.setSeed(seed);

        long first = attempts.get(0).getStartTimeNs();
        long last = attempts.get(attempts.size() - 1).getEndTimeNs();

        result.setDurationMs((last - first) / 1_000_000L);

        result.setHitRate(Statistics.getHoldSuccessRatePercent(attempts));
        result.setAvgIntervalMs(Statistics.getAverageHoldIntervalMs(attempts));

        int resultId = gameResultDao.insert(result);

        holdAttemptDao.insertHoldAttempts(resultId, attempts);

        return resultId;
    }
}