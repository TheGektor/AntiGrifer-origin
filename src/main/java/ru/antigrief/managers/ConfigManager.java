package ru.antigrief.managers;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import ru.antigrief.AntiGriefSystem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class ConfigManager {

    private final AntiGriefSystem plugin;
    private long trustedPlaytimeNeeded;
    private Set<Material> restrictedItems;
    private String discordWebhookUrl;

    public ConfigManager(AntiGriefSystem plugin) {
        this.plugin = plugin;
        this.restrictedItems = new HashSet<>();
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // Default 6 hours (360 minutes) if missing.
        long minutes = config.getLong("trusted-playtime-needed-minutes", 360);
        // Convert to milliseconds
        this.trustedPlaytimeNeeded = minutes * 60 * 1000L;

        this.discordWebhookUrl = config.getString("discord-webhook-url", "");

        List<String> items = config.getStringList("restricted-items");
        restrictedItems.clear();
        for (String itemName : items) {
            try {
                Material mat = Material.valueOf(itemName.toUpperCase());
                restrictedItems.add(mat);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().log(Level.WARNING, "Invalid material in config: " + itemName);
            }
        }
    }

    public long getTrustedPlaytimeNeeded() {
        return trustedPlaytimeNeeded;
    }

    public Set<Material> getRestrictedItems() {
        return restrictedItems;
    }

    public String getDiscordWebhookUrl() {
        return discordWebhookUrl;
    }
}
