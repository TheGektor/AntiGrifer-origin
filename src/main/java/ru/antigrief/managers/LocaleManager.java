package ru.antigrief.managers;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.antigrief.AntiGriefSystem;

import java.io.File;

public class LocaleManager {

    private final AntiGriefSystem plugin;
    private YamlConfiguration messages;

    public LocaleManager(AntiGriefSystem plugin) {
        this.plugin = plugin;
        loadLocale();
    }

    public void loadLocale() {
        File infoFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!infoFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(infoFile);
    }

    public String getMessage(String key) {
        String msg = messages.getString(key);
        if (msg == null) {
            return "Missing message: " + key;
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
