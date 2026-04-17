package ru.antigrief.paper;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
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
        return new PlatformLocation(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ActionInfo info = new ActionInfo(
            toPlatformPlayer(event.getPlayer()),
            ActionType.BLOCK_PLACE,
            toPlatformLocation(event.getBlock().getLocation()),
            event.getBlock().getType().name(),
            new HashMap<>()
        );
        CheckResult result = pipeline.process(info);
        if (result.isCancelAction()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c" + result.getMessage());
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
            event.getPlayer().sendMessage("§c" + result.getMessage());
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

    @EventHandler
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
}
