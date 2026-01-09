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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import org.bukkit.command.TabCompleter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final AntiGriefSystem plugin;

    public CommandManager(AntiGriefSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label,
            String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getLocaleManager().getComponent("usage"));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            if (!sender.hasPermission("ags.admin")) {
                sender.sendMessage(plugin.getLocaleManager().getComponent("no-permission"));
                return true;
            }
            plugin.getConfigManager().loadConfig();
            plugin.getLocaleManager().loadLocale();
            sender.sendMessage(plugin.getLocaleManager().getPrefix()
                    .append(plugin.getLocaleManager().getComponent("reload-success")));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getLocaleManager().getComponent("usage"));
            return true;
        }

        String targetName = args[1];
        // Async fetch player
        CompletableFuture.runAsync(() -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            // In modern paper UUIDs are fetched if not in cache, assume we have it or user
            // is online
            if (target.getUniqueId() == null) {
                sender.sendMessage(plugin.getLocaleManager().getComponent("player-not-found"));
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
        Component prefix = plugin.getLocaleManager().getPrefix();

        switch (sub) {
            case "trust":
                if (data.isTrusted()) {
                    sender.sendMessage(prefix.append(plugin.getLocaleManager().getComponent("already-trusted",
                            Placeholder.unparsed("player", targetName))));
                } else {
                    data.setTrusted(true);
                    plugin.getPlayerHandler().updateAndSave(data.getUuid(), data);
                    sender.sendMessage(prefix.append(plugin.getLocaleManager().getComponent("trust-success",
                            Placeholder.unparsed("player", targetName))));
                    plugin.getDiscordManager().sendWebhook("trust-given", java.util.Map.of(
                            "admin", sender.getName(),
                            "player", targetName));
                }
                break;
            case "untrust":
                if (!data.isTrusted()) {
                    sender.sendMessage(prefix.append(plugin.getLocaleManager().getComponent("not-trusted",
                            Placeholder.unparsed("player", targetName))));
                } else {
                    data.setTrusted(false);
                    plugin.getPlayerHandler().updateAndSave(data.getUuid(), data);
                    sender.sendMessage(prefix.append(plugin.getLocaleManager().getComponent("untrust-success",
                            Placeholder.unparsed("player", targetName))));
                    plugin.getDiscordManager().sendWebhook("trust-revoked", java.util.Map.of(
                            "admin", sender.getName(),
                            "player", targetName));
                }
                break;
            case "check":
                long minutes = data.getPlaytime() / 60000;
                if (data.isTrusted()) {
                    sender.sendMessage(prefix.append(plugin.getLocaleManager().getComponent("check-status-trusted",
                            Placeholder.unparsed("player", targetName),
                            Placeholder.unparsed("time", String.valueOf(minutes)))));
                } else {
                    long needed = plugin.getConfigManager().getTrustedPlaytimeNeeded() / 60000;
                    sender.sendMessage(prefix.append(plugin.getLocaleManager().getComponent("check-status-untrusted",
                            Placeholder.unparsed("player", targetName),
                            Placeholder.unparsed("time", String.valueOf(minutes)),
                            Placeholder.unparsed("needed", String.valueOf(needed)))));
                }
                break;
            default:
                sender.sendMessage(plugin.getLocaleManager().getComponent("usage"));
                break;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subcommands = List.of("trust", "untrust", "check", "reload");
            String input = args[0].toLowerCase();
            for (String sub : subcommands) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            // Suggest players for trust/untrust/check
            String sub = args[0].toLowerCase();
            if (List.of("trust", "untrust", "check").contains(sub)) {
                return null; // Return null to let Bukkit suggest online players
            }
        }

        return completions;
    }
}
