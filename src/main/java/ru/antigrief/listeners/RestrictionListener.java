package ru.antigrief.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import ru.antigrief.AntiGriefSystem;
import ru.antigrief.data.PlayerData;
import net.kyori.adventure.text.Component;

public class RestrictionListener implements Listener {

    private final AntiGriefSystem plugin;

    public RestrictionListener(AntiGriefSystem plugin) {
        this.plugin = plugin;
    }

    private boolean checkRestriction(Player player, Material material, String action) {
        if (!plugin.getConfigManager().getRestrictedItems().contains(material)) {
            return false; // Not restricted
        }

        PlayerData data = plugin.getPlayerHandler().getData(player.getUniqueId());
        // If data is null (loading error) treat as untrusted for safety
        if (data == null || !data.isTrusted()) {
            if (!player.hasPermission("ags.bypass")) {
                Component msg = plugin.getLocaleManager().getPrefix()
                        .append(plugin.getLocaleManager().getComponent("restricted-action"));
                player.sendMessage(msg);

                plugin.getDiscordManager().sendWebhook("suspicious-activity", java.util.Map.of(
                        "player", player.getName(),
                        "item", material.name(),
                        "action", action));
                return true; // Restricted
            }
        }
        return false;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // Check item in hand usage
        if (event.getItem() != null) {
            Material mat = event.getItem().getType();
            if (checkRestriction(event.getPlayer(), mat, "использование предмета")) {
                event.setCancelled(true);
                return;
            }
        }

        // Check interaction with blocks (e.g. opening hoppers)
        if (event.getClickedBlock() != null && event.getAction().name().contains("RIGHT_CLICK")) {
            Material mat = event.getClickedBlock().getType();
            if (checkRestriction(event.getPlayer(), mat, "взаимодействие с блоком")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Material mat = event.getBlock().getType();
        if (checkRestriction(event.getPlayer(), mat, "установка")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            ItemStack result = event.getInventory().getResult();
            if (result != null) {
                if (checkRestriction(player, result.getType(), "крафт")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onDispense(org.bukkit.event.block.BlockDispenseEvent event) {
        if (event.getItem() == null)
            return;
        Material mat = event.getItem().getType();

        if (plugin.getConfigManager().getRestrictedItems().contains(mat)) {
            event.setCancelled(true);

            // Notify Discord about mechanism activity
            String locStr = event.getBlock().getLocation().getBlockX() + ", " +
                    event.getBlock().getLocation().getBlockY() + ", " +
                    event.getBlock().getLocation().getBlockZ();

            plugin.getDiscordManager().sendWebhook("mechanism-activity", java.util.Map.of(
                    "location", locStr,
                    "item", mat.name()));
        }
    }
}
