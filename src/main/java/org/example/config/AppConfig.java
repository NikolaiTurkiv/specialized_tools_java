package org.example.config;

public record AppConfig(
        int httpPort,
        String dbUrl,
        String dbUser,
        String dbPassword,
        String tokenSecret,
        long tokenTtlSeconds,
        String otpOutputFile,
        long expirationCheckIntervalSeconds
) {
    public static AppConfig load() {
        PropertiesLoader loader = new PropertiesLoader("application.properties");

        return new AppConfig(
                loader.getInt("app.http.port", 8080),
                loader.get("db.url", "jdbc:postgresql://localhost:5432/postgres"),
                loader.get("db.user", "postgres"),
                loader.get("db.password", ""),
                loader.get("security.token.secret", "change-me-please"),
                loader.getLong("security.token.ttl-seconds", 3600),
                loader.get("otp.output.file", "otp-codes.txt"),
                loader.getLong("otp.expiration-check-interval-seconds", 30)
        );
    }
}
