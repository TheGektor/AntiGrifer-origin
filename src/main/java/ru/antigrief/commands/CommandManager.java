package ru.antigrief.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ru.antigrief.AntiGriefSystem;
import ru.antigrief.data.PlayerData;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CommandManager implements CommandExecutor {

    private final AntiGriefSystem plugin;

    public CommandManager(AntiGriefSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label,
            String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getLocaleManager().getMessage("usage"));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            if (!sender.hasPermission("ags.admin")) {
                sender.sendMessage(plugin.getLocaleManager().getMessage("no-permission"));
                return true;
            }
            plugin.getConfigManager().loadConfig();
            plugin.getLocaleManager().loadLocale();
            sender.sendMessage(plugin.getLocaleManager().getMessage("prefix") +
                    plugin.getLocaleManager().getMessage("reload-success"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getLocaleManager().getMessage("usage"));
            return true;
        }

        String targetName = args[1];
        // Async fetch player
        CompletableFuture.runAsync(() -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            // In modern paper UUIDs are fetched if not in cache, assume we have it or user
            // is online
            if (target.getUniqueId() == null) {
                sender.sendMessage(plugin.getLocaleManager().getMessage("player-not-found"));
                return;
            }
            UUID uuid = target.getUniqueId();

            // Try to get from cache if online
            PlayerData data = plugin.getPlayerHandler().getData(uuid);
            // If not online, load from DB
            if (data == null) {
                data = plugin.getDatabaseManager().loadPlayer(uuid);
            }

            final PlayerData finalData = data;

            Bukkit.getScheduler().runTask(plugin, () -> {
                handleSubCommand(sender, sub, targetName, finalData);
            });
        });

        return true;
    }

    private void handleSubCommand(CommandSender sender, String sub, String targetName, PlayerData data) {
        String prefix = plugin.getLocaleManager().getMessage("prefix");

        switch (sub) {
            case "trust":
                if (data.isTrusted()) {
                    sender.sendMessage(prefix
                            + plugin.getLocaleManager().getMessage("already-trusted").replace("{player}", targetName));
                } else {
                    data.setTrusted(true);
                    plugin.getPlayerHandler().updateAndSave(data.getUuid(), data);
                    sender.sendMessage(prefix
                            + plugin.getLocaleManager().getMessage("trust-success").replace("{player}", targetName));
                    plugin.getDiscordManager().sendNotification("**Доверие выдано**",
                            "Администратор **" + sender.getName() + "** выдал доверие игроку **" + targetName + "**.");
                }
                break;
            case "untrust":
                if (!data.isTrusted()) {
                    sender.sendMessage(prefix
                            + plugin.getLocaleManager().getMessage("not-trusted").replace("{player}", targetName));
                } else {
                    data.setTrusted(false);
                    plugin.getPlayerHandler().updateAndSave(data.getUuid(), data);
                    sender.sendMessage(prefix
                            + plugin.getLocaleManager().getMessage("untrust-success").replace("{player}", targetName));
                    plugin.getDiscordManager().sendNotification("**Доверие снято**",
                            "Администратор **" + sender.getName() + "** снял доверие с игрока **" + targetName + "**.");
                }
                break;
            case "check":
                long minutes = data.getPlaytime() / 60000;
                if (data.isTrusted()) {
                    sender.sendMessage(prefix + plugin.getLocaleManager().getMessage("check-status-trusted")
                            .replace("{player}", targetName)
                            .replace("{time}", String.valueOf(minutes)));
                } else {
                    long needed = plugin.getConfigManager().getTrustedPlaytimeNeeded() / 60000;
                    sender.sendMessage(prefix + plugin.getLocaleManager().getMessage("check-status-untrusted")
                            .replace("{player}", targetName)
                            .replace("{time}", String.valueOf(minutes))
                            .replace("{needed}", String.valueOf(needed)));
                }
                break;
            default:
                sender.sendMessage(plugin.getLocaleManager().getMessage("usage"));
                break;
        }
    }
}
