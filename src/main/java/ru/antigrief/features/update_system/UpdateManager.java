package ru.antigrief.features.update_system;

import org.bukkit.plugin.Plugin;
import ru.antigrief.core.version.UpdateChecker;
import ru.antigrief.core.version.VersionInfo;
import ru.antigrief.core.version.VersionManager;

import java.util.concurrent.CompletableFuture;

/**
 * Менеджер системы обновлений.
 *
 * @author Antag0nis1
 */
public class UpdateManager {

    private final Plugin plugin;
    private final VersionManager versionManager;
    private final UpdateChecker updateChecker;

    public UpdateManager(Plugin plugin, VersionManager versionManager) {
        this.plugin = plugin;
        this.versionManager = versionManager;
        // TODO: Вынести репозиторий в конфиг
        this.updateChecker = new UpdateChecker(plugin, "Antag0nis1", "PluginName"); 
    }

    public void startupCheck() {
        if (plugin.getConfig().getBoolean("update.check-on-startup", true)) {
            checkUpdates().thenAccept(info -> {
                if (info != null && versionManager.getUpdateStatus() != VersionManager.UpdateType.NONE) {
                    plugin.getLogger().warning("New update available: " + info.getFullVersion());
                }
            });
        }
    }

    public CompletableFuture<VersionInfo> checkUpdates() {
        return updateChecker.checkUpdates().thenApply(info -> {
            if (info != null) {
                versionManager.checkForUpdates(info);
            }
            return info;
        });
    }

    public VersionManager getVersionManager() {
        return versionManager;
    }
}
