package ru.antigrief.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.antigrief.AntiGriefSystem;

import java.io.File;

public class LocaleManager {

    private final AntiGriefSystem plugin;
    private YamlConfiguration locale;
    private final MiniMessage miniMessage;

    public LocaleManager(AntiGriefSystem plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public void loadLocale() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        locale = YamlConfiguration.loadConfiguration(file);
    }

    public Component getComponent(String key, TagResolver... placeholders) {
        String msg = locale.getString(key);
        if (msg == null) {
            return miniMessage.deserialize("<red>Message not found: " + key);
        }
        return miniMessage.deserialize(msg, placeholders);
    }

    public Component getPrefix() {
        String prefix = locale.getString("prefix", "<gray>[<red>AGS<gray>] ");
        return miniMessage.deserialize(prefix);
    }
}
