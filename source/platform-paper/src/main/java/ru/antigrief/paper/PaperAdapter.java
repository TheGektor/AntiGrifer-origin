package ru.antigrief.paper;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.antigrief.api.CheckResult;
import ru.antigrief.common.action.ActionInfo;
import ru.antigrief.common.action.ActionType;
import ru.antigrief.common.data.PlatformLocation;
import ru.antigrief.common.data.PlatformPlayer;
import ru.antigrief.core.pipeline.ActionPipeline;

import java.util.HashMap;

public class PaperAdapter implements Listener {
    private final ActionPipeline pipeline;

    public PaperAdapter(ActionPipeline pipeline) {
        this.pipeline = pipeline;
    }

    private PlatformPlayer toPlatformPlayer(org.bukkit.entity.Player player) {
        return new PlatformPlayer(player.getUniqueId(), player.getName());
    }

    private PlatformLocation toPlatformLocation(org.bukkit.Location loc) {
        String worldName = (loc.getWorld() != null) ? loc.getWorld().getName() : "unknown";
        return new PlatformLocation(worldName, loc.getX(), loc.getY(), loc.getZ());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ActionType type = ActionType.BLOCK_PLACE;
        String material = event.getBlock().getType().name();
        
        if (material.contains("TNT") || material.contains("CRYSTAL")) {
            type = ActionType.EXPLOSIVE_PLACE;
        }

        ActionInfo info = new ActionInfo(
            toPlatformPlayer(event.getPlayer()),
            type,
            toPlatformLocation(event.getBlock().getLocation()),
            material,
            new HashMap<>()
        );
        CheckResult result = pipeline.process(info);
        if (result.isCancelAction()) {
            event.setCancelled(true);
            if (result.getMessage() != null) {
                event.getPlayer().sendMessage(result.getMessage().replace("&", "§"));
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        ActionInfo info = new ActionInfo(
            toPlatformPlayer(event.getPlayer()),
            ActionType.BLOCK_BREAK,
            toPlatformLocation(event.getBlock().getLocation()),
            event.getBlock().getType().name(),
            new HashMap<>()
        );
        CheckResult result = pipeline.process(info);
        if (result.isCancelAction()) {
            event.setCancelled(true);
            if (result.getMessage() != null) {
                event.getPlayer().sendMessage("\u00a7c" + result.getMessage());
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        ActionInfo info = new ActionInfo(
            toPlatformPlayer(event.getPlayer()),
            ActionType.JOIN,
            toPlatformLocation(event.getPlayer().getLocation()),
            null,
            new HashMap<>()
        );
        pipeline.process(info);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        ActionInfo info = new ActionInfo(
            toPlatformPlayer(event.getPlayer()),
            ActionType.QUIT,
            toPlatformLocation(event.getPlayer().getLocation()),
            null,
            new HashMap<>()
        );
        pipeline.process(info);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        
        ActionType type = null;
        String material = event.getClickedBlock().getType().name();
        
        // Redstone interaction check
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.PHYSICAL) {
            if (isRedstoneRelated(event.getClickedBlock().getType())) {
                type = ActionType.REDSTONE_INTERACT;
            }
        }
        
        // Fire use check (flint and steel / fire charge)
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getItem() != null && (event.getItem().getType() == Material.FLINT_AND_STEEL || event.getItem().getType() == Material.FIRE_CHARGE)) {
                type = ActionType.FIRE_USE;
                material = event.getItem().getType().name();
            }
        }

        if (type != null) {
            ActionInfo info = new ActionInfo(
                toPlatformPlayer(event.getPlayer()),
                type,
                toPlatformLocation(event.getClickedBlock().getLocation()),
                material,
                new HashMap<>()
            );
            CheckResult result = pipeline.process(info);
            if (result.isCancelAction()) {
                event.setCancelled(true);
                if (result.getMessage() != null) {
                    event.getPlayer().sendMessage(result.getMessage().replace("&", "§"));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.getPlayer() == null) return;
        
        ActionInfo info = new ActionInfo(
            toPlatformPlayer(event.getPlayer()),
            ActionType.FIRE_USE,
            toPlatformLocation(event.getBlock().getLocation()),
            "BLOCK_IGNITE_" + event.getCause().name(),
            new HashMap<>()
        );
        CheckResult result = pipeline.process(info);
        if (result.isCancelAction()) {
            event.setCancelled(true);
        }
    }

    private boolean isRedstoneRelated(Material material) {
        String name = material.name();
        return name.contains("BUTTON") || name.contains("LEVER") || name.contains("PRESSURE_PLATE") 
            || name.equals("DISPENSER") || name.equals("DROPPER") || name.equals("TNT");
    }
}
