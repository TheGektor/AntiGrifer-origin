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
                player.sendMessage(plugin.getLocaleManager().getMessage("prefix") +
                        plugin.getLocaleManager().getMessage("restricted-action"));

                plugin.getDiscordManager().sendNotification("**Попытка грифа?**",
                        "Игрок **" + player.getName() + "** попытался использовать **" + material.name() + "** ("
                                + action + "), но он не доверенный.");
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
}
