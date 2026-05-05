package org.example.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesLoader {
    private final Properties properties = new Properties();

    public PropertiesLoader(String resourceName) {
        try (InputStream inputStream = PropertiesLoader.class.getClassLoader()
                .getResourceAsStream(resourceName)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + resourceName, e);
        }
    }

    public String get(String key, String defaultValue) {
        String fromSystem = System.getProperty(key);
        if (fromSystem != null && !fromSystem.isBlank()) {
            return fromSystem;
        }
        String fromEnv = System.getenv(key.toUpperCase().replace('.', '_'));
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return properties.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        return Integer.parseInt(get(key, String.valueOf(defaultValue)));
    }

    public long getLong(String key, long defaultValue) {
        return Long.parseLong(get(key, String.valueOf(defaultValue)));
    }
}
