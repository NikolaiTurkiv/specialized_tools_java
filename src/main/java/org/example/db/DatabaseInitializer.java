package org.example.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    private final ConnectionFactory connectionFactory;

    public DatabaseInitializer(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void initialize() {
        String[] statements = {
                "CREATE SCHEMA IF NOT EXISTS otp_service",
                """
                CREATE TABLE IF NOT EXISTS otp_service.users (
                    id BIGSERIAL PRIMARY KEY,
                    username VARCHAR(100) NOT NULL UNIQUE,
                    password_hash VARCHAR(100) NOT NULL,
                    role VARCHAR(20) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS otp_service.otp_config (
                    id INTEGER PRIMARY KEY,
                    code_length INTEGER NOT NULL,
                    ttl_seconds INTEGER NOT NULL,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT single_config_row CHECK (id = 1),
                    CONSTRAINT code_length_positive CHECK (code_length BETWEEN 4 AND 12),
                    CONSTRAINT ttl_seconds_positive CHECK (ttl_seconds BETWEEN 30 AND 3600)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS otp_service.otp_codes (
                    id BIGSERIAL PRIMARY KEY,
                    user_id BIGINT NOT NULL REFERENCES otp_service.users(id) ON DELETE CASCADE,
                    operation_id VARCHAR(255) NOT NULL,
                    code VARCHAR(20) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    delivery_channel VARCHAR(20) NOT NULL,
                    destination VARCHAR(255),
                    expires_at TIMESTAMP NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    used_at TIMESTAMP,
                    CONSTRAINT otp_status_allowed CHECK (status IN ('ACTIVE', 'EXPIRED', 'USED'))
                )
                """,
                """
                CREATE INDEX IF NOT EXISTS idx_otp_codes_user_operation
                ON otp_service.otp_codes(user_id, operation_id)
                """,
                """
                INSERT INTO otp_service.otp_config (id, code_length, ttl_seconds, updated_at)
                VALUES (1, 6, 300, CURRENT_TIMESTAMP)
                ON CONFLICT (id) DO NOTHING
                """
        };

        try (Connection connection = connectionFactory.getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize database schema", e);
        }
    }
}
