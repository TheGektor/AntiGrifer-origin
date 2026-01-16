package ru.antigrief.core.version;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Менеджер версий плагина.
 *
 * @author Antag0nis1
 */
public class VersionManager {

    private final Plugin plugin;
    private final VersionInfo currentVersion;
    private VersionInfo latestVersion;
    private UpdateType updateStatus = UpdateType.NONE;

    public enum UpdateType {
        MAJOR,      // Глобальные изменения (2.0.0)
        MINOR,      // Новые фичи (1.2.0)
        PATCH,      // Баг-фиксы (1.0.1)
        NONE        // Обновлений нет
    }

    public VersionManager(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.currentVersion = VersionInfo.parse(plugin.getDescription().getVersion());
        
        if (this.currentVersion == null) {
            plugin.getLogger().warning("Could not parse current plugin version: " + plugin.getDescription().getVersion());
        } else {
            plugin.getLogger().info("Loaded version manager. Current version: " + currentVersion);
        }
    }

    /**
     * Сравнивает текущую версию с новой и определяет тип обновления.
     */
    public void checkForUpdates(@NotNull VersionInfo remoteVersion) {
        this.latestVersion = remoteVersion;
        
        if (remoteVersion.isNewerThan(currentVersion)) {
            if (remoteVersion.getMajor() > currentVersion.getMajor()) {
                updateStatus = UpdateType.MAJOR;
            } else if (remoteVersion.getMinor() > currentVersion.getMinor()) {
                updateStatus = UpdateType.MINOR;
            } else {
                updateStatus = UpdateType.PATCH;
            }
            
            plugin.getLogger().info("Found new update: " + remoteVersion + " (" + updateStatus + ")");
        } else {
            updateStatus = UpdateType.NONE;
        }
    }

    public @Nullable VersionInfo getCurrentVersion() {
        return currentVersion;
    }

    public @Nullable VersionInfo getLatestVersion() {
        return latestVersion;
    }

    public UpdateType getUpdateStatus() {
        return updateStatus;
    }
}
