package org.example.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.example.db.ConnectionFactory;
import org.example.model.User;
import org.example.model.UserRole;

public class UserDao {
    private final ConnectionFactory connectionFactory;

    public UserDao(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public User create(String username, String passwordHash, UserRole role) {
        String sql = """
                INSERT INTO otp_service.users (username, password_hash, role)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.setString(3, role.name());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1)).orElseThrow();
                }
            }
            throw new IllegalStateException("User was created without generated id");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create user", e);
        }
    }

    public boolean adminExists() {
        String sql = "SELECT EXISTS (SELECT 1 FROM otp_service.users WHERE role = 'ADMIN')";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getBoolean(1);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to check admin existence", e);
        }
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM otp_service.users WHERE username = ?";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find user by username", e);
        }
    }

    public Optional<User> findById(long id) {
        String sql = "SELECT * FROM otp_service.users WHERE id = ?";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find user by id", e);
        }
    }

    public List<User> findAllNonAdminUsers() {
        String sql = "SELECT * FROM otp_service.users WHERE role <> 'ADMIN' ORDER BY id";
        List<User> users = new ArrayList<>();
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(map(resultSet));
            }
            return users;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch users", e);
        }
    }

    public boolean deleteById(long id) {
        String sql = "DELETE FROM otp_service.users WHERE id = ?";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete user", e);
        }
    }

    private User map(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getLong("id"),
                resultSet.getString("username"),
                resultSet.getString("password_hash"),
                UserRole.valueOf(resultSet.getString("role")),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }
}
