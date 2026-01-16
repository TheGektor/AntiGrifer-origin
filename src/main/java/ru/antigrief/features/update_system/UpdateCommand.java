package ru.antigrief.features.update_system;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import ru.antigrief.core.utils.MiniMessageUtils;
import ru.antigrief.core.version.VersionInfo;
import ru.antigrief.core.version.VersionManager;

/**
 * Команда управления обновлениями.
 * /ags update check
 * /ags update info
 *
 * @author Antag0nis1
 */
public class UpdateCommand implements CommandExecutor {

    private final UpdateManager updateManager;

    public UpdateCommand(UpdateManager updateManager) {
        this.updateManager = updateManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("ags.admin")) {
            MiniMessageUtils.sendMessage(sender, "<red>No permission.</red>");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            sendInfo(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("check")) {
            MiniMessageUtils.sendMessage(sender, "<gray>Checking for updates...</gray>");
            updateManager.checkUpdates().thenAccept(info -> {
                if (info != null) {
                    sendInfo(sender);
                } else {
                    MiniMessageUtils.sendMessage(sender, "<red>Failed to check updates.</red>");
                }
            });
            return true;
        }

        return false;
    }

    private void sendInfo(CommandSender sender) {
        VersionManager vm = updateManager.getVersionManager();
        VersionInfo current = vm.getCurrentVersion();
        VersionInfo latest = vm.getLatestVersion();

        MiniMessageUtils.sendMessage(sender, "<gradient:#00AA00:#55FF55><b>AntiGriefSystem Update Info</b></gradient>");
        MiniMessageUtils.sendMessage(sender, "<gray>Current version: <white>" + (current != null ? current.getFullVersion() : "Unknown"));

        if (latest != null && vm.getUpdateStatus() != VersionManager.UpdateType.NONE) {
            MiniMessageUtils.sendMessage(sender, "<gray>New version available: <green>" + latest.getFullVersion());
            MiniMessageUtils.sendMessage(sender, "<gray>Type: <yellow>" + vm.getUpdateStatus());
            if (latest.getChangelog() != null && !latest.getChangelog().isEmpty()) {
                 MiniMessageUtils.sendMessage(sender, "<gray>Changelog: <italic>" + latest.getChangelog());
            }
        } else {
            MiniMessageUtils.sendMessage(sender, "<green>You are running the latest version.</green>");
        }
    }
}
