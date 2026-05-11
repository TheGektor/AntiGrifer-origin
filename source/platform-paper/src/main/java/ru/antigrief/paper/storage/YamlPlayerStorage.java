package ru.antigrief.paper.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import ru.antigrief.api.PlayerContext;
import ru.antigrief.core.context.PlayerContextImpl;
import ru.antigrief.core.storage.StorageEngine;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Реальное хранилище данных игроков в YAML-файлах.
 * Каталог: plugins/AntiGrifer/players/<uuid>.yml
 */
public class YamlPlayerStorage implements StorageEngine {
    private final File playersDir;
    private final Logger logger;

    public YamlPlayerStorage(File dataFolder, Logger logger) {
        this.logger = logger;
        this.playersDir = new File(dataFolder, "players");
        if (!playersDir.exists() && !playersDir.mkdirs()) {
            logger.warning("[AntiGrifer] Не удалось создать папку players/");
        }
    }

    private File fileFor(UUID uuid) {
        return new File(playersDir, uuid.toString() + ".yml");
    }

    @Override
    public void savePlayer(PlayerContext context) {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("uuid", context.getUniqueId().toString());
        yml.set("playtime_seconds", context.getPlaytimeSeconds());
        yml.set("first_seen", context.getFirstSeenTimestamp());
        Integer manual = context.getManualTier();
        if (manual != null) yml.set("manual_tier", manual);
        try {
            yml.save(fileFor(context.getUniqueId()));
        } catch (IOException e) {
            logger.warning("[AntiGrifer] Ошибка сохранения " + context.getUniqueId() + ": " + e.getMessage());
        }
    }

    @Override
    public PlayerContext loadPlayer(UUID uuid) {
        PlayerContextImpl ctx = new PlayerContextImpl(uuid);
        File f = fileFor(uuid);
        if (!f.exists()) return ctx;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
        ctx.addPlaytime(yml.getLong("playtime_seconds", 0));
        ctx.setFirstSeenTimestamp(yml.getLong("first_seen", System.currentTimeMillis()));
        if (yml.contains("manual_tier")) {
            ctx.setManualTier(yml.getInt("manual_tier"));
        }
        return ctx;
    }

    @Override
    public void saveAll(Iterable<PlayerContext> contexts) {
        for (PlayerContext ctx : contexts) {
            savePlayer(ctx);
        }
    }

    @Override
    public void close() { /* YAML не требует закрытия */ }
}
