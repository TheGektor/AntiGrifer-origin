package ru.antigrief.core.localization;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import net.kyori.adventure.text.Component;
import ru.antigrief.core.utils.MiniMessageUtils;

/**
 * Менеджер локализации (RU/EN).
 *
 * @author Antag0nis1
 */
public class LanguageManager {

    private final Plugin plugin;
    private final Map<String, String> messages = new HashMap<>();
    private String currentLocale;

    public LanguageManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void loadLanguage(String locale) {
        this.currentLocale = locale;
        this.messages.clear();

        File langFile = new File(plugin.getDataFolder(), "messages/" + locale + ".yml");
        if (!langFile.exists()) {
            saveDefaultLanguage(locale);
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(langFile);
            for (String key : config.getKeys(true)) {
                if (config.isString(key)) {
                    messages.put(key, config.getString(key));
                }
            }
            plugin.getLogger().info("Loaded " + messages.size() + " messages for locale: " + locale);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load language file: " + locale, e);
        }
    }

    private void saveDefaultLanguage(String locale) {
        try {
            String resourcePath = "messages/" + locale + ".yml";
            InputStream resource = plugin.getResource(resourcePath);
            if (resource != null) {
                File outputFile = new File(plugin.getDataFolder(), resourcePath);
                if (!outputFile.getParentFile().exists()) {
                    outputFile.getParentFile().mkdirs();
                }
                plugin.saveResource(resourcePath, false);
            } else {
                plugin.getLogger().warning("Language resource not found: " + resourcePath);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save default language: " + locale, e);
        }
    }

    public Component getMessage(String key) {
        String raw = messages.getOrDefault(key, "<red>Missing message: " + key + "</red>");
        return MiniMessageUtils.parse(raw);
    }
    
    public String getRawMessage(String key) {
        return messages.getOrDefault(key, "Missing message: " + key);
    }
}
