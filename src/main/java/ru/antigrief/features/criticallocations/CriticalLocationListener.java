package ru.antigrief.features.criticallocations;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import ru.antigrief.AntiGriefSystem;

public class CriticalLocationListener implements Listener {

    private final AntiGriefSystem plugin;
    private final CriticalLocationManager manager;

    public CriticalLocationListener(AntiGriefSystem plugin, CriticalLocationManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private void handleViolation(Player player, CriticalLocation loc, String action, String item) {
        if (player.hasPermission("antigrief.bypass.critical")) return;

        // Kick Player
        player.kick(Component.text("Попытка гриферства важной локации!", NamedTextColor.RED));

        // Discord Alert
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("action", action);
        placeholders.put("location", loc.getName());
        placeholders.put("item", item);
        placeholders.put("coords", String.format("%d, %d, %d", 
                player.getLocation().getBlockX(), 
                player.getLocation().getBlockY(), 
                player.getLocation().getBlockZ()));
        
        String roleId = plugin.getConfig().getString("critical-locations.discord-role-id", "");
        placeholders.put("role_id", roleId);

        plugin.getDiscordManager().sendWebhook("critical-location-alert", placeholders);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        // Standard place restriction check
        if (player.getGameMode() == GameMode.CREATIVE && player.hasPermission("antigrief.bypass.critical")) return;

        Material type = event.getBlock().getType();
        
        if (!plugin.getConfigManager().getRestrictedItems().contains(type)) {
            return; 
        }

        CriticalLocation loc = manager.getCriticalLocation(event.getBlock().getLocation());
        
        if (loc != null) {
            if (manager.isPlayerTrusted(loc.getName(), player.getUniqueId())) {
                return; // Player is trusted in this specific region!
            }
            
            event.setCancelled(true);
            handleViolation(player, loc, "Restricted Place", type.toString());
        }
    }
    
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        org.bukkit.Location target = event.getBlock().getLocation();

        // Standard break restriction check
        if (player.getGameMode() == GameMode.CREATIVE && player.hasPermission("antigrief.bypass.critical")) return;
        Material type = event.getBlock().getType();

        if (!plugin.getConfigManager().getRestrictedItems().contains(type)) {
            return;
        }

        CriticalLocation loc = manager.getCriticalLocation(target);

        if (loc != null) {
            if (manager.isPlayerTrusted(loc.getName(), player.getUniqueId())) {
                return; // Player is trusted in this specific region!
            }
            
            event.setCancelled(true);
            handleViolation(player, loc, "Restricted Break", type.toString());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() == null) return;
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE && player.hasPermission("antigrief.bypass.critical")) return;

        Material type = event.getItem().getType();

        if (!plugin.getConfigManager().getRestrictedItems().contains(type)) {
             return; 
        }

        org.bukkit.Location target = event.getClickedBlock() != null 
                ? event.getClickedBlock().getLocation() 
                : player.getLocation();

        CriticalLocation loc = manager.getCriticalLocation(target);

        if (loc != null) {
            if (manager.isPlayerTrusted(loc.getName(), player.getUniqueId())) {
                return; // Player is trusted in this specific region!
            }
            
            event.setCancelled(true);
            handleViolation(player, loc, "Restricted Interact", type.toString());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> manager.getCriticalLocation(block.getLocation()) != null);
    }
}
