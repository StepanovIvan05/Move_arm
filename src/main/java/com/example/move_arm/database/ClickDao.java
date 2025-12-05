package com.example.move_arm.database;

import com.example.move_arm.model.ClickData;
import javafx.geometry.Point2D;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ClickDao {
    private final DatabaseManager db = DatabaseManager.getInstance();

    public void insertClicks(int resultId, List<ClickData> clicks) {
        String sql = "INSERT INTO clicks(result_id, click_index, time_ns, cursor_x, cursor_y, center_x, center_y, radius) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = 0;
            for (ClickData cd : clicks) {
                ps.setInt(1, resultId);
                ps.setInt(2, idx++);
                ps.setLong(3, cd.getClickTimeNs());
                ps.setDouble(4, cd.getCursor().getX());
                ps.setDouble(5, cd.getCursor().getY());
                ps.setDouble(6, cd.getCenter().getX());
                ps.setDouble(7, cd.getCenter().getY());
                ps.setDouble(8, cd.getRadius());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public List<ClickData> readClicksForResult(int resultId) {
        List<ClickData> out = new ArrayList<>();
        String sql = "SELECT click_index, time_ns, cursor_x, cursor_y, center_x, center_y, radius FROM clicks WHERE result_id = ? ORDER BY click_index ASC";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, resultId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                long timeNs = rs.getLong("time_ns");
                double cursorX = rs.getDouble("cursor_x");
                double cursorY = rs.getDouble("cursor_y");
                double centerX = rs.getDouble("center_x");
                double centerY = rs.getDouble("center_y");
                double radius = rs.getDouble("radius");
                out.add(new ClickData(timeNs, cursorX, cursorY, centerX, centerY, radius));
            }
        } catch (Exception e) { throw new RuntimeException(e); }
        return out;
    }

    public int getMaxClicksForUserAndRadius(int userId, double radius) {
        String sql = "SELECT COUNT(*) AS clicks " +
                "FROM clicks c " +
                "JOIN game_results g ON c.result_id = g.id " +
                "WHERE g.user_id = ? AND c.radius = ? " +
                "GROUP BY c.result_id " +
                "ORDER BY clicks DESC " +
                "LIMIT 1";

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDouble(2, radius);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("clicks");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
