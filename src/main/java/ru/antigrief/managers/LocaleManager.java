package ru.antigrief.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.antigrief.AntiGriefSystem;

import java.io.File;

public class LocaleManager {

    private final AntiGriefSystem plugin;
    private YamlConfiguration messages;
    private final MiniMessage miniMessage;

    public LocaleManager(AntiGriefSystem plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        loadLocale();
    }

    public void loadLocale() {
        String lang = plugin.getConfigManager().getLanguage();
        String fileName = "messages_" + lang + ".yml";
        File file = new File(plugin.getDataFolder(), fileName);

        // Try to save resource if it doesn't exist
        if (!file.exists()) {
            try {
                plugin.saveResource(fileName, false);
            } catch (IllegalArgumentException e) {
                // If resource doesn't exist in jar (e.g. untranslated lang), validation?
                // Fallback to en or log warning.
                plugin.getLogger().warning("Language file " + fileName + " not found in JAR. Creating empty.");
            }
        }
        
        // If still doesn't exist (e.g. custom lang not in jar), user might have created it?
        // Or if saveResource failed.
        if (file.exists()) {
            messages = YamlConfiguration.loadConfiguration(file);
        } else {
            // Fallback to default English if file still missing
             File enFile = new File(plugin.getDataFolder(), "messages_en.yml");
             if (!enFile.exists()) plugin.saveResource("messages_en.yml", false);
             messages = YamlConfiguration.loadConfiguration(enFile);
        }
    }

    public Component getComponent(String key, TagResolver... resolvers) {
        String msg = messages.getString(key);
        if (msg == null) {
            return Component.text("Missing message: " + key);
        }
        return miniMessage.deserialize(msg, resolvers);
    }

    // Helper used for prefix concatenation usually, but with Components we should
    // use append.
    // However, if the user's messages.yml just has prefix inside the messages, we
    // don't need this.
    // If they have a separate prefix key, we can have a helper.
    public Component getPrefix() {
        return getComponent("prefix");
    }
}
