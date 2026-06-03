package com.example.move_arm.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.example.move_arm.model.GeneratorType;
import com.example.move_arm.model.TrajectoryDifficulty;
import com.example.move_arm.model.settings.HoverGameSettings;

public class HoverSettingsDao {

    private final DatabaseManager db = DatabaseManager.getInstance();

    public HoverGameSettings load(long userId) {

        String sql = "SELECT * FROM hover_settings WHERE user_id = ?";

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                HoverGameSettings s = new HoverGameSettings();

                s.setDurationSeconds(rs.getInt("duration_seconds"));
                s.setRadius(rs.getInt("radius"));
                s.setSeed(rs.getInt("seed"));
                s.setDifficulty(parseDifficulty(rs.getString("difficulty")));
                s.setMaxCirclesCount(rs.getInt("max_circles_count"));
                s.setGeneratorType(parseGeneratorType(rs.getString("generator_type")));

                return s;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new HoverGameSettings(); // дефолт
    }

    public void save(long userId, HoverGameSettings settings) {

        String sql = """
            INSERT INTO hover_settings(
                user_id,
                duration_seconds,
                radius,
                seed,
                difficulty,
                max_circles_count,
                generator_type
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(user_id) DO UPDATE SET
                duration_seconds = excluded.duration_seconds,
                radius = excluded.radius,
                seed = excluded.seed,
                difficulty = excluded.difficulty,
                max_circles_count = excluded.max_circles_count,
                generator_type = excluded.generator_type
        """;

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setInt(2, settings.getDurationSeconds());
            ps.setInt(3, settings.getRadius());
            ps.setInt(4, settings.getSeed());
            ps.setString(5, settings.getDifficulty().name());
            ps.setInt(6, settings.getMaxCirclesCount());
            ps.setString(7, settings.getGeneratorType().name());

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean exists(long userId) {

        String sql = "SELECT 1 FROM hover_settings WHERE user_id = ?";

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
