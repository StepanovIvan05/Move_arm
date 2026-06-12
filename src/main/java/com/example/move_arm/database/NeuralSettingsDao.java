package com.example.move_arm.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.example.move_arm.model.AnimationType;
import com.example.move_arm.model.settings.NeuralGameSettings;

public class NeuralSettingsDao {

    private final DatabaseManager db = DatabaseManager.getInstance();

    public boolean exists(long userId) {
        String sql = "SELECT 1 FROM neural_settings WHERE user_id = ? LIMIT 1";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            return ps.executeQuery().next();
        } catch (Exception e) {
            return false;
        }
    }

    public NeuralGameSettings load(long userId) {

        String sql = "SELECT * FROM neural_settings WHERE user_id = ?";

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                NeuralGameSettings s = new NeuralGameSettings();

                s.setDurationSeconds(rs.getInt("duration_seconds"));
                s.setRadius(rs.getInt("radius"));
                s.setMaxCirclesCount(rs.getInt("max_circles_count"));

                return s;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new NeuralGameSettings(); // дефолт
    }

    public void save(long userId, NeuralGameSettings settings) {

        String sql = """
            INSERT INTO neural_settings(
                user_id,
                duration_seconds,
                radius,
                max_circles_count
            ) VALUES (?, ?, ?, ?)
            ON CONFLICT(user_id) DO UPDATE SET
                duration_seconds=excluded.duration_seconds,
                radius=excluded.radius,
                max_circles_count=excluded.max_circles_count
            """;

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setInt(2, settings.getDurationSeconds());
            ps.setInt(3, settings.getRadius());
            ps.setInt(4, settings.getMaxCirclesCount());
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
