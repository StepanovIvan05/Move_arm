package com.example.move_arm.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_DIR =
            System.getProperty("user.home") + "/.movearm";

    private static final String DB_FILE = DB_DIR + "/movearm.db";

    private static final String URL = "jdbc:sqlite:" + DB_FILE;

    private static DatabaseManager instance;

    private DatabaseManager() {
        initDbFile();
        initSchema();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    private void initDbFile() {
        try {
            Path dir = Path.of(DB_DIR);
            if (!Files.exists(dir)) Files.createDirectories(dir);

            Path file = Path.of(DB_FILE);
            if (!Files.exists(file)) Files.createFile(file);

        } catch (Exception e) {
            throw new RuntimeException("Не удалось инициализировать DB file", e);
        }
    }

    private void initSchema() {
        try (Connection c = getConnection(); Statement s = c.createStatement()) {

            s.execute("""
                CREATE TABLE IF NOT EXISTS users (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  username TEXT NOT NULL UNIQUE
                );
                """);

            s.execute("""
                CREATE TABLE IF NOT EXISTS game_types (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  name TEXT NOT NULL UNIQUE,
                  description TEXT
                );
                """);

            s.execute("""
                CREATE TABLE IF NOT EXISTS game_results (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  user_id INTEGER,
                  game_type_id INTEGER,
                  radius INTEGER,
                  seed INTEGER,
                  score INTEGER,
                  duration_ms INTEGER,
                  timestamp INTEGER,
                  hit_rate REAL,
                  avg_interval_ms REAL,
                  avg_distance_px REAL,
                  avg_speed REAL,
                  FOREIGN KEY(user_id) REFERENCES users(id),
                  FOREIGN KEY(game_type_id) REFERENCES game_types(id)
                );
                """);

            s.execute("""
                CREATE TABLE IF NOT EXISTS clicks (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  result_id INTEGER,
                  click_index INTEGER,
                  time_ns INTEGER,
                  cursor_x REAL,
                  cursor_y REAL,
                  center_x REAL,
                  center_y REAL,
                  radius INTEGER,
                  FOREIGN KEY(result_id) REFERENCES game_results(id)
                );
                """);

            s.execute("""
                CREATE TABLE IF NOT EXISTS app_meta (
                  key TEXT PRIMARY KEY,
                  value TEXT
                );
                """);

            s.execute("""
                INSERT OR IGNORE INTO game_types(name, description)
                VALUES ('hover', 'Move Arm hover game');
                """);

            s.execute("""
                INSERT OR IGNORE INTO game_types(name, description)
                VALUES ('hold', 'Move Arm hold game');
                """);
            s.execute("""
                CREATE TABLE IF NOT EXISTS holds(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                result_id INTEGER NOT NULL,
                attempt_index INTEGER NOT NULL,   -- Порядковый номер контакта в игре (1, 2, 3...)
                start_time_ns INTEGER NOT NULL,
                end_time_ns INTEGER NOT NULL,
                actual_hold_ms INTEGER NOT NULL,
                success INTEGER NOT NULL,         -- 1 (успех), 0 (срыв)
                target_center_x REAL NOT NULL,    -- Координата X центра круга
                target_center_y REAL NOT NULL,    -- Координата Y центра круга
                FOREIGN KEY(result_id) REFERENCES game_results(id)
                );
                """);
            
            s.execute("""
                    CREATE TABLE IF NOT EXISTS global_settings (
                    user_id INTEGER PRIMARY KEY,
                    animation_type TEXT NOT NULL,
                    FOREIGN KEY(user_id) REFERENCES users(id)
                    );
                    """);
            s.execute("""
                    CREATE TABLE IF NOT EXISTS hover_settings (
                    user_id INTEGER PRIMARY KEY,
                    duration_seconds INTEGER NOT NULL,
                    radius INTEGER NOT NULL,
                    seed INTEGER NOT NULL,
                    max_circles_count INTEGER NOT NULL,
                    FOREIGN KEY(user_id) REFERENCES users(id)
                    );
                    """);
            s.execute("""
                    CREATE TABLE IF NOT EXISTS hold_settings (
                    user_id INTEGER PRIMARY KEY,
                    radius INTEGER NOT NULL,
                    hold_time_ms INTEGER NOT NULL,
                    FOREIGN KEY(user_id) REFERENCES users(id)
                    );
                    """);
            s.execute("""
                CREATE TABLE IF NOT EXISTS target_triplets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    timestamp INTEGER,
                    triplet_index INTEGER NOT NULL,
                    spawn_ns INTEGER NOT NULL,
                    t1_cell INTEGER NOT NULL,
                    t2_cell INTEGER NOT NULL,
                    t3_cell INTEGER NOT NULL,
                    hit_target_index INTEGER NOT NULL,
                    hit_ttk_ns INTEGER NOT NULL,
                    radius INTEGER NOT NULL,
                    centroid_row REAL,
                    centroid_col REAL,
                    t1_angle REAL,
                    t2_angle REAL,
                    t3_angle REAL,
                    hit_to_miss1_dist REAL,
                    hit_to_miss2_dist REAL,
                    miss1_to_miss2_dist REAL,
                    spread REAL,
                    screen_width INTEGER,
                    screen_height INTEGER,
                    previous_hit_cell INTEGER DEFAULT -1
                );
                """);
                s.execute("""
                    INSERT OR IGNORE INTO game_types(name, description)
                    VALUES ('neural', 'Neural RL training mode');
                """);

        } catch (Exception e) {
            throw new RuntimeException("Не удалось инициализировать схему БД", e);
        }
    }

    public Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL);
    }

    // --- Метаданные приложения ---
    public void setAppProperty(String key, String value) {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO app_meta(key, value) VALUES(?, ?) " +
                             "ON CONFLICT(key) DO UPDATE SET value = excluded.value")) {

            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getAppProperty(String key) {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT value FROM app_meta WHERE key = ?")) {

            ps.setString(1, key);
            var rs = ps.executeQuery();
            if (rs.next()) return rs.getString(1);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
