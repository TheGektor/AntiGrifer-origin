package ru.antigrief.features.criticallocations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CriticalLocationCommand implements CommandExecutor, TabCompleter {

    private final CriticalLocationManager manager;

    public CriticalLocationCommand(CriticalLocationManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("antigrief.admin.critical")) {
            player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "pos1":
                setPos(player, 1);
                break;
            case "pos2":
                setPos(player, 2);
                break;
            case "create":
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /critical create <name>", NamedTextColor.RED));
                    return true;
                }
                String name = args[1];
                if (manager.createLocation(name, player)) {
                    player.sendMessage(Component.text("Critical location '" + name + "' created successfully.", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Failed to create location. Make sure both positions are set and in the same world.", NamedTextColor.RED));
                }
                break;
            case "remove":
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /critical remove <name>", NamedTextColor.RED));
                    return true;
                }
                manager.deleteLocation(args[1]);
                player.sendMessage(Component.text("Critical location '" + args[1] + "' deleted.", NamedTextColor.GREEN));
                break;
            case "list":
                player.sendMessage(Component.text("Critical Locations:", NamedTextColor.GOLD));
                for (Map.Entry<String, CriticalLocation> entry : manager.getAllLocations().entrySet()) {
                    CriticalLocation loc = entry.getValue();
                    player.sendMessage(Component.text("- " + entry.getKey() + " (" + loc.getWorld().getName() + ")", NamedTextColor.YELLOW));
                }
                break;
            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("Critical Locations Commands:", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/critical pos1 - Set first corner", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/critical pos2 - Set second corner", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/critical create <name> - Create location", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/critical remove <name> - Remove location", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/critical list - List locations", NamedTextColor.YELLOW));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (!sender.hasPermission("antigrief.admin.critical")) return suggestions;

        if (args.length == 1) {
            suggestions.add("pos1");
            suggestions.add("pos2");
            suggestions.add("create");
            suggestions.add("remove");
            suggestions.add("list");
            return filter(suggestions, args[0]);
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("remove")) {
                suggestions.addAll(manager.getAllLocations().keySet());
            } else if (sub.equals("create")) {
                suggestions.add("<name>");
            }
            return filter(suggestions, args[1]);
        }

        return suggestions;
    }

    private void setPos(Player player, int posId) {
        org.bukkit.block.Block targetBlock = player.getTargetBlockExact(5);
        org.bukkit.Location loc;
        
        if (targetBlock != null && !targetBlock.getType().isAir()) {
            loc = targetBlock.getLocation();
        } else {
            loc = player.getLocation().getBlock().getLocation();
        }

        if (posId == 1) {
            manager.setPos1(player, loc);
            player.sendMessage(Component.text("Position 1 set to " + 
                loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(), NamedTextColor.GREEN));
        } else {
            manager.setPos2(player, loc);
            player.sendMessage(Component.text("Position 2 set to " + 
                loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(), NamedTextColor.GREEN));
        }
    }

    private List<String> filter(List<String> list, String partial) {
        String p = partial.toLowerCase();
        return list.stream().filter(s -> s.toLowerCase().startsWith(p)).collect(Collectors.toList());
    }
}
