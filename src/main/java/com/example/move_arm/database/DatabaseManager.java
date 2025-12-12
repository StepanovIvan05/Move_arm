package com.example.move_arm.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.PreparedStatement;

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
