package com.example.move_arm.database;

import com.example.move_arm.model.GameType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class GameTypeDao {
    private final DatabaseManager db = DatabaseManager.getInstance();

    public Optional<GameType> findByName(String name) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, name, description FROM game_types WHERE name = ?")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(new GameType(rs.getInt("id"), rs.getString("name"), rs.getString("description")));
            }
        } catch (Exception e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    public int getIdOrCreate(String name, String description) {
        var opt = findByName(name);
        if (opt.isPresent()) return opt.get().getId();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO game_types(name, description) VALUES(?, ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.executeUpdate();
            var keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (Exception e) { throw new RuntimeException(e); }
        throw new RuntimeException("Не удалось создать game_type");
    }
}
