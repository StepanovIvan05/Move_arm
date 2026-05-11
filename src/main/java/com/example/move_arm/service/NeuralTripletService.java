package com.example.move_arm.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.example.move_arm.database.DatabaseManager;
import com.example.move_arm.model.TripletRecord;
import com.example.move_arm.util.AppLogger;

public class NeuralTripletService {

    private static final NeuralTripletService INSTANCE = new NeuralTripletService();
    private NeuralTripletService() {}

    public static NeuralTripletService getInstance() {
        return INSTANCE;
    }

    public void saveTriplet(TripletRecord rec) {
        saveTriplet(
            rec.userId, rec.timestamp, rec.tripletIndex, rec.spawnNs,
            rec.t1Cell, rec.t2Cell, rec.t3Cell, rec.hitTargetIndex, rec.hitTtkNs, rec.radius,
            rec.centroidRow, rec.centroidCol, rec.t1Angle, rec.t2Angle, rec.t3Angle,
            rec.hitToMiss1Dist, rec.hitToMiss2Dist, rec.miss1ToMiss2Dist, rec.spread,
            rec.screenWidth, rec.screenHeight, rec.previousHitCell
        );
    }

    public void saveTriplet(
            int userId, long timestamp, int tripletIndex, long spawnNs,
            int t1Cell, int t2Cell, int t3Cell, int hitTargetIndex, long hitTtkNs, int radius,
            double centroidRow, double centroidCol,
            double t1Angle, double t2Angle, double t3Angle,
            double hitToMiss1Dist, double hitToMiss2Dist, double miss1ToMiss2Dist, double spread,
            int screenWidth, int screenHeight, int previousHitCell) {

        String sql = """
            INSERT INTO target_triplets (
                user_id, timestamp, triplet_index, spawn_ns,
                t1_cell, t2_cell, t3_cell, hit_target_index, hit_ttk_ns, radius,
                centroid_row, centroid_col, t1_angle, t2_angle, t3_angle,
                hit_to_miss1_dist, hit_to_miss2_dist, miss1_to_miss2_dist, spread,
                screen_width, screen_height, previous_hit_cell
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            setParams(ps, userId, timestamp, tripletIndex, spawnNs,
                    t1Cell, t2Cell, t3Cell, hitTargetIndex, hitTtkNs, radius,
                    centroidRow, centroidCol, t1Angle, t2Angle, t3Angle,
                    hitToMiss1Dist, hitToMiss2Dist, miss1ToMiss2Dist, spread,
                    screenWidth, screenHeight, previousHitCell);

            ps.executeUpdate();
        } catch (Exception e) {
            AppLogger.error("NeuralTripletService: ошибка сохранения тройки", e);
            throw new RuntimeException("Не удалось сохранить тройку", e);
        }
    }

    public void saveTripletsBatch(List<TripletRecord> records) {
        String sql = """
            INSERT INTO target_triplets (
                user_id, timestamp, triplet_index, spawn_ns,
                t1_cell, t2_cell, t3_cell, hit_target_index, hit_ttk_ns, radius,
                centroid_row, centroid_col, t1_angle, t2_angle, t3_angle,
                hit_to_miss1_dist, hit_to_miss2_dist, miss1_to_miss2_dist, spread,
                screen_width, screen_height, previous_hit_cell
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            for (TripletRecord rec : records) {
                setParams(ps, rec.userId, rec.timestamp, rec.tripletIndex, rec.spawnNs,
                        rec.t1Cell, rec.t2Cell, rec.t3Cell, rec.hitTargetIndex, rec.hitTtkNs, rec.radius,
                        rec.centroidRow, rec.centroidCol, rec.t1Angle, rec.t2Angle, rec.t3Angle,
                        rec.hitToMiss1Dist, rec.hitToMiss2Dist, rec.miss1ToMiss2Dist, rec.spread,
                        rec.screenWidth, rec.screenHeight, rec.previousHitCell);  // ← добавили
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) {
            AppLogger.error("NeuralTripletService: ошибка batch-сохранения", e);
            throw new RuntimeException("Не удалось сохранить тройки", e);
        }
    }

    private void setParams(
            PreparedStatement ps,
            int userId, long timestamp, int tripletIndex, long spawnNs,
            int t1Cell, int t2Cell, int t3Cell, int hitTargetIndex, long hitTtkNs, int radius,
            double centroidRow, double centroidCol,
            double t1Angle, double t2Angle, double t3Angle,
            double hitToMiss1Dist, double hitToMiss2Dist, double miss1ToMiss2Dist, double spread,
            int screenWidth, int screenHeight, int previousHitCell) throws SQLException {

        ps.setInt(1, userId);
        ps.setLong(2, timestamp);
        ps.setInt(3, tripletIndex);
        ps.setLong(4, spawnNs);
        ps.setInt(5, t1Cell);
        ps.setInt(6, t2Cell);
        ps.setInt(7, t3Cell);
        ps.setInt(8, hitTargetIndex);
        ps.setLong(9, hitTtkNs);
        ps.setInt(10, radius);
        ps.setDouble(11, centroidRow);
        ps.setDouble(12, centroidCol);
        ps.setDouble(13, t1Angle);
        ps.setDouble(14, t2Angle);
        ps.setDouble(15, t3Angle);
        ps.setDouble(16, hitToMiss1Dist);
        ps.setDouble(17, hitToMiss2Dist);
        ps.setDouble(18, miss1ToMiss2Dist);
        ps.setDouble(19, spread);
        ps.setInt(20, screenWidth);
        ps.setInt(21, screenHeight);
        ps.setInt(22, previousHitCell);        // ← самое важное
    }
}