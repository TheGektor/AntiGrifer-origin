package ru.antigrief.features.criticallocations;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.GameMode;
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
        placeholders.put("coords", String.format("%d, %d, %d", 
                player.getLocation().getBlockX(), 
                player.getLocation().getBlockY(), 
                player.getLocation().getBlockZ()));
        placeholders.put("role_id", plugin.getConfig().getString("critical-locations.discord-role-id", ""));

        plugin.getDiscordManager().sendWebhook("critical-location-alert", placeholders);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE && player.hasPermission("antigrief.bypass.critical")) return;

        CriticalLocation loc = manager.getCriticalLocation(event.getBlock().getLocation());
        if (loc != null) {
            event.setCancelled(true);
            handleViolation(player, loc, "Break Block", event.getBlock().getType().toString());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE && player.hasPermission("antigrief.bypass.critical")) return;

        CriticalLocation loc = manager.getCriticalLocation(event.getBlock().getLocation());
        if (loc != null) {
            event.setCancelled(true);
            handleViolation(player, loc, "Place Block", event.getBlock().getType().toString());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE && player.hasPermission("antigrief.bypass.critical")) return;

        // Only check physical interactions that modify the world (like bucket empty, flushing, etc)
        // Or specific dangerous items. For now, we block all interactions to be safe/strict as requested.
        // Actually, simple interaction like opening a chest might not be "griefing" but usually critical areas are protected.
        // Let's block if it's not a safe interaction.

        CriticalLocation loc = manager.getCriticalLocation(event.getClickedBlock().getLocation());
        if (loc != null) {
            switch (event.getAction()) {
                case RIGHT_CLICK_BLOCK:
                case PHYSICAL:
                    event.setCancelled(true);
                    handleViolation(player, loc, "Interact", event.getMaterial().toString());
                    break;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        // Prevent explosion damage to critical area
        event.blockList().removeIf(block -> manager.getCriticalLocation(block.getLocation()) != null);
    }
}
