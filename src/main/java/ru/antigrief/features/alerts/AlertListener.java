package ru.antigrief.features.alerts;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

/**
 * Слушатель событий для обнаружения действия гриферов.
 *
 * @author Antag0nis1
 */
public class AlertListener implements Listener {

    private final AlertManager alertManager;

    public AlertListener(AlertManager alertManager) {
        this.alertManager = alertManager;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.TNT) {
            alertManager.sendAlert(
                    event.getPlayer(),
                    "Placed TNT",
                    event.getBlock().getLocation()
            );
        }
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (event.getBucket() == Material.LAVA_BUCKET) {
            alertManager.sendAlert(
                    event.getPlayer(),
                    "Placed Lava",
                    event.getBlockClicked().getRelative(event.getBlockFace()).getLocation()
            );
        }
    }
}
