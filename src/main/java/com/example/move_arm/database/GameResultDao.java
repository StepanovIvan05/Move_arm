package com.example.move_arm.database;

import com.example.move_arm.model.GameResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class GameResultDao {
    private final DatabaseManager db = DatabaseManager.getInstance();

    public int insert(GameResult r) {
        String sql = """
            INSERT INTO game_results(user_id, game_type_id, radius, score, duration_ms, timestamp, hit_rate, avg_interval_ms, avg_distance_px, avg_speed)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getUserId());
            ps.setInt(2, r.getGameTypeId());
            ps.setInt(3, r.getRadius());
            ps.setInt(4, r.getScore());
            ps.setLong(5, r.getDurationMs());
            ps.setLong(6, r.getTimestamp());
            ps.setDouble(7, r.getHitRate());
            ps.setDouble(8, r.getAvgIntervalMs());
            ps.setDouble(9, r.getAvgDistancePx());
            ps.setDouble(10, r.getAvgSpeed());
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

    public List<Integer> findListScoreByUserGameTypeAndRadius(int userId, int gameTypeId, int radius){
        List<Integer> out = new ArrayList<>();
        String sql = "SELECT score FROM game_results WHERE user_id = ? AND game_type_id = ? AND radius = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, gameTypeId);
            ps.setInt(3, radius);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(rs.getInt("score"));
        } catch (Exception e) { throw new RuntimeException(e); }
        return out;
    }

    public List<Double> findListAvgTimesByUserGameTypeAndRadius(int userId, int gameTypeId, int radius){
        List<Double> out = new ArrayList<>();
        String sql = "SELECT avg_interval_ms FROM game_results WHERE user_id = ? AND game_type_id = ? AND radius = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, gameTypeId);
            ps.setInt(3, radius);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(rs.getDouble("avg_interval_ms"));
        } catch (Exception e) { throw new RuntimeException(e); }
        return out;
    }

    public  int findRecordScoreByUserGameTypeAndRadius(int userId, int gameTypeId, int radius){
        int out = 0;
        List<Integer> scores = findListScoreByUserGameTypeAndRadius(userId, gameTypeId, radius);
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
        r.setScore(rs.getInt("score"));
        r.setDurationMs(rs.getLong("duration_ms"));
        r.setTimestamp(rs.getLong("timestamp"));
        r.setHitRate(rs.getDouble("hit_rate"));
        r.setAvgIntervalMs(rs.getDouble("avg_interval_ms"));
        r.setAvgDistancePx(rs.getDouble("avg_distance_px"));
        r.setAvgSpeed(rs.getDouble("avg_speed"));
        return r;
    }
}
