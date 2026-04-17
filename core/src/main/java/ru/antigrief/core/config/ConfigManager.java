package ru.antigrief.core.config;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final Map<String, Object> values = new HashMap<>();

    public void set(String path, Object value) {
        values.put(path, value);
    }

    public <T> T get(String path, T defaultValue) {
        return (T) values.getOrDefault(path, defaultValue);
    }

    public int getInt(String path, int defaultValue) {
        return get(path, defaultValue);
    }

    public String getString(String path, String defaultValue) {
        return get(path, defaultValue);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        return get(path, defaultValue);
    }
}
