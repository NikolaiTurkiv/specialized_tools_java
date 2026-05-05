package org.example.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.example.db.ConnectionFactory;
import org.example.model.OtpConfig;

public class OtpConfigDao {
    private final ConnectionFactory connectionFactory;

    public OtpConfigDao(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public OtpConfig get() {
        String sql = "SELECT * FROM otp_service.otp_config WHERE id = 1";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new IllegalStateException("OTP config row is missing");
            }
            return new OtpConfig(
                    resultSet.getInt("id"),
                    resultSet.getInt("code_length"),
                    resultSet.getInt("ttl_seconds"),
                    resultSet.getTimestamp("updated_at").toInstant()
            );
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load OTP config", e);
        }
    }

    public OtpConfig update(int codeLength, int ttlSeconds) {
        String sql = """
                UPDATE otp_service.otp_config
                SET code_length = ?, ttl_seconds = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = 1
                """;
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, codeLength);
            statement.setInt(2, ttlSeconds);
            statement.executeUpdate();
            return get();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update OTP config", e);
        }
    }
}
