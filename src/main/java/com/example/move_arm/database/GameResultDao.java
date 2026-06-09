package com.example.move_arm.database;

import com.example.move_arm.model.GameResult;
import com.example.move_arm.model.GeneratorType;
import com.example.move_arm.model.TrajectoryDifficulty;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class GameResultDao {
    private final DatabaseManager db = DatabaseManager.getInstance();

    public int insert(GameResult r) {
        String sql = """
            INSERT INTO game_results(user_id, game_type_id, radius, generator_type, seed, difficulty, score, duration_ms, timestamp, hit_rate, avg_interval_ms, avg_distance_px, avg_speed)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getUserId());
            ps.setInt(2, r.getGameTypeId());
            ps.setInt(3, r.getRadius());
            ps.setString(4, r.getGeneratorType().name());
            ps.setInt(5, r.getSeed());
            ps.setString(6, r.getDifficulty().name());
            ps.setInt(7, r.getScore());
            ps.setLong(8, r.getDurationMs());
            ps.setLong(9, r.getTimestamp());
            ps.setDouble(10, r.getHitRate());
            ps.setDouble(11, r.getAvgIntervalMs());
            ps.setDouble(12, r.getAvgDistancePx());
            ps.setDouble(13, r.getAvgSpeed());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                r.setId(id);
                return id;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Не удалось вставить GameResult");
    }

    public Optional<GameResult> findById(int id) {
        String sql = "SELECT * FROM game_results WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                GameResult r = mapRow(rs);
                return Optional.of(r);
            }
        } catch (Exception e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    public List<GameResult> findByUserId(int userId) {
        List<GameResult> out = new ArrayList<>();
        String sql = "SELECT * FROM game_results WHERE user_id = ? ORDER BY timestamp DESC";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(mapRow(rs));
        } catch (Exception e) { throw new RuntimeException(e); }
        return out;
    }

    public List<GameResult> findAll() {
        List<GameResult> out = new ArrayList<>();
        String sql = "SELECT * FROM game_results ORDER BY timestamp DESC";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(mapRow(rs));
        } catch (Exception e) { throw new RuntimeException(e); }
        return out;
    }

    public void deleteByUserId(int userId) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM game_results WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public List<Integer> findListScoreByUserGameTypeAndRadiusSeedDifficulty(
            int userId,
            int gameTypeId,
            int radius,
            int seed,
            TrajectoryDifficulty difficulty) {
        List<Integer> out = new ArrayList<>();
        String sql = "SELECT score FROM game_results WHERE user_id = ? AND game_type_id = ? AND radius = ? AND seed = ? AND difficulty = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, gameTypeId);
            ps.setInt(3, radius);
            ps.setInt(4, seed);
            ps.setString(5, normalizeDifficulty(difficulty).name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(rs.getInt("score"));
        } catch (Exception e) { throw new RuntimeException(e); }
        return out;
    }

    public List<Integer> findListScoresByGeneratorSettings(
            int userId,
            int gameTypeId,
            int radius,
            GeneratorType generatorType,
            int seed,
            TrajectoryDifficulty difficulty) {
        List<Integer> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = prepareGeneratorSettingsStatement(
                     c,
                     "SELECT score FROM game_results",
                     userId,
                     gameTypeId,
                     radius,
                     generatorType,
                     seed,
                     difficulty
             )) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(rs.getInt("score"));
        } catch (Exception e) { throw new RuntimeException(e); }
        return out;
    }

    public List<Double> findListAvgTimesByUserGameTypeAndRadiusSeedDifficulty(
            int userId,
            int gameTypeId,
            int radius,
            int seed,
            TrajectoryDifficulty difficulty) {
        List<Double> out = new ArrayList<>();
        String sql = "SELECT avg_interval_ms FROM game_results WHERE user_id = ? AND game_type_id = ? AND radius = ? AND seed = ? AND difficulty = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, gameTypeId);
            ps.setInt(3, radius);
            ps.setInt(4, seed);
            ps.setString(5, normalizeDifficulty(difficulty).name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(rs.getDouble("avg_interval_ms"));
        } catch (Exception e) { throw new RuntimeException(e); }
        return out;
    }

    public List<Double> findListAvgTimesByGeneratorSettings(
            int userId,
            int gameTypeId,
            int radius,
            GeneratorType generatorType,
            int seed,
            TrajectoryDifficulty difficulty) {
        List<Double> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = prepareGeneratorSettingsStatement(
                     c,
                     "SELECT avg_interval_ms FROM game_results",
                     userId,
                     gameTypeId,
                     radius,
                     generatorType,
                     seed,
                     difficulty
             )) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(rs.getDouble("avg_interval_ms"));
        } catch (Exception e) { throw new RuntimeException(e); }
        return out;
    }

    public int findRecordScoreByUserGameTypeAndRadiusSeedDifficulty(
            int userId,
            int gameTypeId,
            int radius,
            int seed,
            TrajectoryDifficulty difficulty) {
        int out = 0;
        List<Integer> scores = findListScoreByUserGameTypeAndRadiusSeedDifficulty(userId, gameTypeId, radius, seed, difficulty);
        if (scores != null && !scores.isEmpty()) {
            out = Collections.max(scores);
        }
        return out;
    }

    public int findRecordScoreByGeneratorSettings(
            int userId,
            int gameTypeId,
            int radius,
            GeneratorType generatorType,
            int seed,
            TrajectoryDifficulty difficulty) {
        int out = 0;
        List<Integer> scores = findListScoresByGeneratorSettings(userId, gameTypeId, radius, generatorType, seed, difficulty);
        if (scores != null && !scores.isEmpty()) {
            out = Collections.max(scores);
        }
        return out;
    }


    private GameResult mapRow(ResultSet rs) throws SQLException {
        GameResult r = new GameResult();
        r.setId(rs.getInt("id"));
        r.setUserId(rs.getInt("user_id"));
        r.setGameTypeId(rs.getInt("game_type_id"));
        r.setRadius(rs.getInt("radius"));
        r.setGeneratorType(parseGeneratorType(rs.getString("generator_type")));
        r.setSeed(rs.getInt("seed"));
        r.setDifficulty(parseDifficulty(rs.getString("difficulty")));
        r.setScore(rs.getInt("score"));
        r.setDurationMs(rs.getLong("duration_ms"));
        r.setTimestamp(rs.getLong("timestamp"));
        r.setHitRate(rs.getDouble("hit_rate"));
        r.setAvgIntervalMs(rs.getDouble("avg_interval_ms"));
        r.setAvgDistancePx(rs.getDouble("avg_distance_px"));
        r.setAvgSpeed(rs.getDouble("avg_speed"));
        return r;
    }

    private PreparedStatement prepareGeneratorSettingsStatement(
            Connection c,
            String selectSql,
            int userId,
            int gameTypeId,
            int radius,
            GeneratorType generatorType,
            int seed,
            TrajectoryDifficulty difficulty) throws SQLException {
        GeneratorType normalizedType = normalizeGeneratorType(generatorType);
        String optionFilter = normalizedType == GeneratorType.RANDOM
                ? " AND seed = ?"
                : " AND difficulty = ?";
        String sql = selectSql + " WHERE user_id = ? AND game_type_id = ? AND radius = ? AND generator_type = ?" + optionFilter;
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setInt(2, gameTypeId);
        ps.setInt(3, radius);
        ps.setString(4, normalizedType.name());

        if (normalizedType == GeneratorType.RANDOM) {
            ps.setInt(5, seed);
        } else {
            ps.setString(5, normalizeDifficulty(difficulty).name());
        }

        return ps;
    }

    private GeneratorType normalizeGeneratorType(GeneratorType generatorType) {
        return generatorType == null ? GeneratorType.ADAPTIVE : generatorType;
    }

    private TrajectoryDifficulty normalizeDifficulty(TrajectoryDifficulty difficulty) {
        return difficulty == null ? TrajectoryDifficulty.MEDIUM : difficulty;
    }

    private TrajectoryDifficulty parseDifficulty(String value) {
        try {
            return value == null ? TrajectoryDifficulty.MEDIUM : TrajectoryDifficulty.valueOf(value);
        } catch (IllegalArgumentException e) {
            return TrajectoryDifficulty.MEDIUM;
        }
    }

    private GeneratorType parseGeneratorType(String value) {
        try {
            return value == null ? GeneratorType.ADAPTIVE : GeneratorType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return GeneratorType.ADAPTIVE;
        }
    }
}
