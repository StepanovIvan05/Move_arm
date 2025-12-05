package com.example.move_arm.database;

import com.example.move_arm.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {
    private final DatabaseManager db = DatabaseManager.getInstance();

    public Optional<User> findByUsername(String username) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, username FROM users WHERE username = ?")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(new User(rs.getInt("id"), rs.getString("username")));
            }
        } catch (Exception e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    public Optional<User> findById(int id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, username FROM users WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(new User(rs.getInt("id"), rs.getString("username")));
        } catch (Exception e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    public User createUser(String username) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO users(username) VALUES(?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                return new User(keys.getInt(1), username);
            }
        } catch (Exception e) { throw new RuntimeException(e); }
        throw new RuntimeException("Не удалось создать пользователя");
    }

    public List<User> listAll() {
        List<User> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, username FROM users ORDER BY username")) {
            var rs = ps.executeQuery();
            while (rs.next()) out.add(new User(rs.getInt("id"), rs.getString("username")));
        } catch (Exception e) { throw new RuntimeException(e); }
        return out;
    }

    public void deleteByUsername(String username) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE username = ?")) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
