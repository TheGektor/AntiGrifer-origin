package ru.antigrief.core.config;

import java.util.HashMap;
import java.util.List;
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

    public List<String> getStringList(String path, List<String> defaultValue) {
        return get(path, defaultValue);
    }

    public ru.antigrief.core.trust.TrustConfig getTrustConfig() {
        ru.antigrief.core.trust.TrustConfig config = new ru.antigrief.core.trust.TrustConfig();
        config.setTier1Minutes(getInt("trust.tier1_minutes", 60));
        config.setTier2Minutes(getInt("trust.tier2_minutes", 120));
        config.setTier3Minutes(getInt("trust.tier3_minutes", 240));
        
        config.setFireMaterials(getStringList("trust.fire_materials", java.util.Collections.emptyList()));
        config.setExplosiveMaterials(getStringList("trust.explosive_materials", java.util.Collections.emptyList()));
        config.setRedstoneMaterials(getStringList("trust.redstone_materials", java.util.Collections.emptyList()));
        
        return config;
    }
}
