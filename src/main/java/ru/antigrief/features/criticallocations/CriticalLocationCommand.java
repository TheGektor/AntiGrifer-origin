package ru.antigrief.features.criticallocations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CriticalLocationCommand implements CommandExecutor, TabCompleter {

    private final CriticalLocationManager manager;
    
    // Constant for the marker item
    public static final String MARKER_NAME = "§cВыделитель критической зоны";
    public static final Material MARKER_MATERIAL = Material.SPONGE; // Or Bedrock, sponge is safer
    
    private static final int MAX_AREA_SIZE = 50000; // Limit to prevent server crashes

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
            player.sendMessage(Component.text("Нет прав.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give":
                giveMarker(player);
                break;
            case "create":
                if (args.length < 2) {
                    player.sendMessage(Component.text("Использование: /critical create <имя>", NamedTextColor.RED));
                    return true;
                }
                handleCreate(player, args[1]);
                break;
            case "remove":
                if (args.length < 2) {
                    player.sendMessage(Component.text("Использование: /critical remove <имя>", NamedTextColor.RED));
                    return true;
                }
                manager.deleteLocation(args[1]);
                player.sendMessage(Component.text("Критическая локация '" + args[1] + "' удалена.", NamedTextColor.GREEN));
                break;
            case "list":
                player.sendMessage(Component.text("Критические локации (используйте /critical info <имя> для подробностей):", NamedTextColor.GOLD));
                for (Map.Entry<String, CriticalLocation> entry : manager.getAllLocations().entrySet()) {
                    CriticalLocation loc = entry.getValue();
                    player.sendMessage(Component.text("- " + entry.getKey() + " (" + loc.getWorld().getName() + ", блоков: " + loc.getCoordinates().size() + ")", NamedTextColor.YELLOW));
                }
                break;
            case "trust":
                if (args.length < 3) {
                    player.sendMessage(Component.text("Использование: /critical trust <игрок> <регион>", NamedTextColor.RED));
                    return true;
                }
                handleTrust(player, args[1], args[2], true);
                break;
            case "untrust":
                if (args.length < 3) {
                    player.sendMessage(Component.text("Использование: /critical untrust <игрок> <регион>", NamedTextColor.RED));
                    return true;
                }
                handleTrust(player, args[1], args[2], false);
                break;
            default:
                sendHelp(player);
                break;
        }

        return true;
    }
    
    private void handleCreate(Player player, String name) {
        if (manager.getLocationByName(name) != null) {
            player.sendMessage(Component.text("Локация с таким именем уже существует!", NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("Начат поиск границ, подождите...", NamedTextColor.YELLOW));

        World world = player.getWorld();
        int startX = player.getLocation().getBlockX();
        int startZ = player.getLocation().getBlockZ();
        
        Set<Long> enclosedPoints = new HashSet<>();
        Queue<Long> queue = new LinkedList<>();
        
        long startNode = CriticalLocation.packXYZ(startX, startZ);
        queue.add(startNode);
        enclosedPoints.add(startNode); // Add to visited/result
        
        boolean enclosed = true;
        
        // 4-way direction (X, Z)
        int[] dx = {1, -1, 0, 0};
        int[] dz = {0, 0, 1, -1};



        while (!queue.isEmpty()) {
            long current = queue.poll();
            int cx = CriticalLocation.getX(current);
            int cz = CriticalLocation.getZ(current);

            for (int i = 0; i < 4; i++) {
                int nx = cx + dx[i];
                int nz = cz + dz[i];
                long neighbor = CriticalLocation.packXYZ(nx, nz);

                if (!enclosedPoints.contains(neighbor)) {
                    // Check if it's a boundary
                    if (!isBoundary(world, nx, nz)) {
                        enclosedPoints.add(neighbor);
                        queue.add(neighbor);
                        
                        if (enclosedPoints.size() > MAX_AREA_SIZE) {
                            enclosed = false;
                            break;
                        }
                    }
                }
            }
            if (!enclosed) break;
        }

        if (!enclosed) {
            player.sendMessage(Component.text("Ошибка: Область слишком большая или контур не замкнут!", NamedTextColor.RED));
            return;
        }

        manager.createLocation(name, world, enclosedPoints);
        player.sendMessage(Component.text("Критическая локация '" + name + "' создана! Охвачено блоков (X,Z): " + enclosedPoints.size(), NamedTextColor.GREEN));
    }

    private boolean isBoundary(World world, int x, int z) {
        // Find highest block to optimize, but we must check if specifically the sponge is at the highest block or any Y.
        // Easiest and most flexible: Get highest block Y at (X,Z), scan downwards slightly.
        // But players might build boundaries in caves. 
        // Best approach for performance without chunk trashing:
        // Bukkit 1.21 async chunk access can be tricky. We do this sync for now.
        
        // Check highest non-empty block first for performance
        int highestY = world.getHighestBlockYAt(x, z);
        if (world.getBlockAt(x, highestY, z).getType() == MARKER_MATERIAL) {
            return true;
        }
        
        // If they placed it lower, we have to scan the chunk section or y levels.
        // Let's scan from highestY down to bottom, up to 100 blocks depth to save performance.
        int minY = Math.max(world.getMinHeight(), highestY - 150);
        for (int y = highestY - 1; y >= minY; y--) {
            if (world.getBlockAt(x, y, z).getType() == MARKER_MATERIAL) {
                return true;
            }
        }
        return false;
    }
    
    private void handleTrust(Player admin, String targetName, String regionName, boolean trust) {
        CriticalLocation loc = manager.getLocationByName(regionName);
        if (loc == null) {
            admin.sendMessage(Component.text("Регион '" + regionName + "' не найден.", NamedTextColor.RED));
            return;
        }
        
        // Resolve target UUID
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || target.getUniqueId() == null) {
            admin.sendMessage(Component.text("Игрок не найден.", NamedTextColor.RED));
            return;
        }
        
        UUID uuid = target.getUniqueId();
        manager.setPlayerTrust(regionName, uuid, trust);
        
        if (trust) {
            admin.sendMessage(Component.text("Игрок " + targetName + " добавлен в доверенные для зоны " + regionName, NamedTextColor.GREEN));
        } else {
            admin.sendMessage(Component.text("Игрок " + targetName + " удален из доверенных для зоны " + regionName, NamedTextColor.YELLOW));
        }
    }
    
    private void giveMarker(Player player) {
        ItemStack item = new ItemStack(MARKER_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(MARKER_NAME));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Установите этот блок, чтобы создать", NamedTextColor.GRAY));
            lore.add(Component.text("замкнутый контур критической локации.", NamedTextColor.GRAY));
            lore.add(Component.text("Встаньте внутрь и напишите /critical create", NamedTextColor.GRAY));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        player.getInventory().addItem(item);
        player.sendMessage(Component.text("Выдан маркер критической локации. Постройте замкнутый фигуру.", NamedTextColor.GREEN));
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("Команды Критических Локаций:", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/critical give - Получить блок для выделения", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/critical create <имя> - Создать зону (встаньте внутрь контура)", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/critical remove <имя> - Удалить локацию по имени", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/critical list - Список локаций", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/critical trust <игрок> <регион> - Выдать права игроку", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/critical untrust <игрок> <регион> - Забрать права", NamedTextColor.YELLOW));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (!sender.hasPermission("antigrief.admin.critical")) return suggestions;

        if (args.length == 1) {
            suggestions.add("give");
            suggestions.add("create");
            suggestions.add("remove");
            suggestions.add("list");
            suggestions.add("trust");
            suggestions.add("untrust");
            return filter(suggestions, args[0]);
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("trust") || args[0].equalsIgnoreCase("untrust"))) {
            // Suggest online players
            for (Player p : Bukkit.getOnlinePlayers()) {
                suggestions.add(p.getName());
            }
            return filter(suggestions, args[1]);
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("trust") || args[0].equalsIgnoreCase("untrust"))) {
            suggestions.addAll(manager.getAllLocations().keySet());
            return filter(suggestions, args[2]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            suggestions.addAll(manager.getAllLocations().keySet());
            return filter(suggestions, args[1]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            suggestions.add("<имя>");
            return filter(suggestions, args[1]);
        }

        return suggestions;
    }

    private List<String> filter(List<String> list, String partial) {
        String p = partial.toLowerCase();
        return list.stream().filter(s -> s.toLowerCase().startsWith(p)).collect(Collectors.toList());
    }
}
