INSERT INTO otp_service.otp_config (id, code_length, ttl_seconds, updated_at)
VALUES (1, 6, 300, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
