package ru.antigrief.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;
import ru.antigrief.AntiGriefSystem;
import ru.antigrief.data.PlayerData;

public class RestrictionListener implements Listener {

    private final AntiGriefSystem plugin;

    public RestrictionListener(AntiGriefSystem plugin) {
        this.plugin = plugin;
    }

    private boolean isActionRestricted(Player player, Material material, String action) {
        if (!plugin.getConfigManager().getRestrictedItems().contains(material)) {
            return false; // Item is not restricted
        }

        if (player.hasPermission("ags.bypass")) {
            return false; // Player has admin bypass
        }

        PlayerData data = plugin.getPlayerHandler().getData(player.getUniqueId());
        
        // If data is null or player is NOT trusted, block the action
        if (data == null || !data.isTrusted()) {
            Component msg = plugin.getLocaleManager().getPrefix()
                    .append(plugin.getLocaleManager().getComponent("restricted-action"));
            player.sendMessage(msg);

            plugin.getAlertManager().sendAlert(player, action, material, null);
            return true; // Action restricted
        }
        
        // Trusted players are allowed
        return false;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        // Check item in hand usage
        if (event.getItem() != null) {
            Material mat = event.getItem().getType();
            if (isActionRestricted(event.getPlayer(), mat, "использование предмета")) {
                event.setCancelled(true);
                return;
            }
        }

        // Check interaction with blocks (e.g. opening hoppers, redstone mechanisms)
        if (event.getClickedBlock() != null && event.getAction().name().contains("RIGHT_CLICK")) {
            Material mat = event.getClickedBlock().getType();
            if (isActionRestricted(event.getPlayer(), mat, "взаимодействие с блоком")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Material mat = event.getBlock().getType();
        if (isActionRestricted(event.getPlayer(), mat, "установка блока")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Material mat = event.getBlock().getType();
        if (isActionRestricted(event.getPlayer(), mat, "разрушение блока")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            ItemStack result = event.getInventory().getResult();
            if (result != null) {
                if (isActionRestricted(player, result.getType(), "крафт")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDispense(org.bukkit.event.block.BlockDispenseEvent event) {
        if (event.getItem() == null) return;
        
        Material mat = event.getItem().getType();

        if (plugin.getConfigManager().getRestrictedItems().contains(mat)) {
            event.setCancelled(true);

            String locStr = event.getBlock().getLocation().getBlockX() + ", " +
                    event.getBlock().getLocation().getBlockY() + ", " +
                    event.getBlock().getLocation().getBlockZ();

            plugin.getDiscordManager().sendWebhook("mechanism-activity", java.util.Map.of(
                    "location", locStr,
                    "item", mat.name()));
        }
    }
}
