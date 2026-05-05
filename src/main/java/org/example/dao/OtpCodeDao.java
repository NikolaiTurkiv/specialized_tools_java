package org.example.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.example.db.ConnectionFactory;
import org.example.model.DeliveryChannel;
import org.example.model.OtpCode;
import org.example.model.OtpStatus;

public class OtpCodeDao {
    private final ConnectionFactory connectionFactory;

    public OtpCodeDao(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public OtpCode create(
            long userId,
            String operationId,
            String code,
            DeliveryChannel deliveryChannel,
            String destination,
            Instant expiresAt
    ) {
        String sql = """
                INSERT INTO otp_service.otp_codes
                (user_id, operation_id, code, status, delivery_channel, destination, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, userId);
            statement.setString(2, operationId);
            statement.setString(3, code);
            statement.setString(4, OtpStatus.ACTIVE.name());
            statement.setString(5, deliveryChannel.name());
            statement.setString(6, destination);
            statement.setTimestamp(7, Timestamp.from(expiresAt));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1)).orElseThrow();
                }
            }
            throw new IllegalStateException("OTP was created without generated id");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create OTP", e);
        }
    }

    public Optional<OtpCode> findActive(long userId, String operationId, String code) {
        String sql = """
                SELECT * FROM otp_service.otp_codes
                WHERE user_id = ? AND operation_id = ? AND code = ? AND status = 'ACTIVE'
                ORDER BY id DESC
                LIMIT 1
                """;
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, operationId);
            statement.setString(3, code);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find OTP", e);
        }
    }

    public void markUsed(long otpId) {
        String sql = """
                UPDATE otp_service.otp_codes
                SET status = 'USED', used_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, otpId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to mark OTP as used", e);
        }
    }

    public int expireDueCodes() {
        String sql = """
                UPDATE otp_service.otp_codes
                SET status = 'EXPIRED'
                WHERE status = 'ACTIVE' AND expires_at < CURRENT_TIMESTAMP
                """;
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to expire OTP codes", e);
        }
    }

    private Optional<OtpCode> findById(long id) {
        String sql = "SELECT * FROM otp_service.otp_codes WHERE id = ?";
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
            throw new IllegalStateException("Failed to load OTP by id", e);
        }
    }

    private OtpCode map(ResultSet resultSet) throws SQLException {
        Timestamp usedAt = resultSet.getTimestamp("used_at");
        return new OtpCode(
                resultSet.getLong("id"),
                resultSet.getLong("user_id"),
                resultSet.getString("operation_id"),
                resultSet.getString("code"),
                OtpStatus.valueOf(resultSet.getString("status")),
                DeliveryChannel.valueOf(resultSet.getString("delivery_channel")),
                resultSet.getString("destination"),
                resultSet.getTimestamp("expires_at").toInstant(),
                resultSet.getTimestamp("created_at").toInstant(),
                usedAt == null ? null : usedAt.toInstant()
        );
    }
}
