package ru.antigrief.paper;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.Material;
import ru.antigrief.api.CheckResult;
import ru.antigrief.common.action.ActionInfo;
import ru.antigrief.common.action.ActionType;
import ru.antigrief.common.data.PlatformLocation;
import ru.antigrief.common.data.PlatformPlayer;
import ru.antigrief.core.context.PlayerContextManager;
import ru.antigrief.core.pipeline.ActionPipeline;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PaperAdapter implements Listener {
    private final ActionPipeline pipeline;
    private final PlayerContextManager contextManager;

    /** Время входа игрока в сессию (мс) — для подсчёта плейтайма при выходе. */
    private final Map<UUID, Long> sessionStarts = new ConcurrentHashMap<>();

    public PaperAdapter(ActionPipeline pipeline, PlayerContextManager contextManager) {
        this.pipeline = pipeline;
        this.contextManager = contextManager;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PlatformPlayer toPlatformPlayer(org.bukkit.entity.Player player) {
        return new PlatformPlayer(player.getUniqueId(), player.getName());
    }

    private PlatformLocation toPlatformLocation(org.bukkit.Location loc) {
        String w = (loc.getWorld() != null) ? loc.getWorld().getName() : "unknown";
        return new PlatformLocation(w, loc.getX(), loc.getY(), loc.getZ());
    }

    // ── Join / Quit ───────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        // Загружаем данные из YAML (синхронно — быстро)
        contextManager.loadPlayer(uuid);
        sessionStarts.put(uuid, System.currentTimeMillis());

        ActionInfo info = new ActionInfo(
                toPlatformPlayer(event.getPlayer()), ActionType.JOIN,
                toPlatformLocation(event.getPlayer().getLocation()), null, new HashMap<>());
        pipeline.process(info);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Начисляем плейтайм за текущую сессию
        Long start = sessionStarts.remove(uuid);
        if (start != null) {
            long sessionSec = (System.currentTimeMillis() - start) / 1000L;
            contextManager.getContext(uuid).addPlaytime(sessionSec);
        }
        // Сохраняем и убираем из кэша
        contextManager.savePlayer(uuid);
        contextManager.removeContext(uuid);

        ActionInfo info = new ActionInfo(
                toPlatformPlayer(event.getPlayer()), ActionType.QUIT,
                toPlatformLocation(event.getPlayer().getLocation()), null, new HashMap<>());
        pipeline.process(info);
    }

    // ── Block events ──────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        String material = event.getBlock().getType().name();
        ActionType type = ActionType.BLOCK_PLACE;
        if (material.contains("TNT") || material.contains("CRYSTAL")) {
            type = ActionType.EXPLOSIVE_PLACE;
        }

        ActionInfo info = new ActionInfo(
                toPlatformPlayer(event.getPlayer()), type,
                toPlatformLocation(event.getBlock().getLocation()), material, new HashMap<>());
        CheckResult result = pipeline.process(info);
        if (result.isCancelAction()) {
            event.setCancelled(true);
            if (result.getMessage() != null) {
                event.getPlayer().sendMessage(result.getMessage().replace("&", "§"));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        ActionInfo info = new ActionInfo(
                toPlatformPlayer(event.getPlayer()), ActionType.BLOCK_BREAK,
                toPlatformLocation(event.getBlock().getLocation()),
                event.getBlock().getType().name(), new HashMap<>());
        CheckResult result = pipeline.process(info);
        if (result.isCancelAction()) {
            event.setCancelled(true);
            if (result.getMessage() != null) {
                event.getPlayer().sendMessage("§c" + result.getMessage());
            }
        }
    }

    /** Обрабатывает ведро с лавой — это PlayerBucketEmptyEvent, не BlockPlaceEvent. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (event.getBucket() != Material.LAVA_BUCKET) return;

        ActionInfo info = new ActionInfo(
                toPlatformPlayer(event.getPlayer()), ActionType.FIRE_USE,
                toPlatformLocation(event.getBlock().getLocation()), "LAVA_BUCKET", new HashMap<>());
        CheckResult result = pipeline.process(info);
        if (result.isCancelAction()) {
            event.setCancelled(true);
            if (result.getMessage() != null) {
                event.getPlayer().sendMessage(result.getMessage().replace("&", "§"));
            }
        }
    }

    // ── Interact ──────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        ActionType type = null;
        String material = event.getClickedBlock().getType().name();

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.PHYSICAL) {
            if (isRedstoneRelated(event.getClickedBlock().getType())) {
                type = ActionType.REDSTONE_INTERACT;
            }
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getItem() != null) {
            Material held = event.getItem().getType();
            if (held == Material.FLINT_AND_STEEL || held == Material.FIRE_CHARGE) {
                type = ActionType.FIRE_USE;
                material = held.name();
            }
        }

        if (type != null) {
            ActionInfo info = new ActionInfo(
                    toPlatformPlayer(event.getPlayer()), type,
                    toPlatformLocation(event.getClickedBlock().getLocation()), material, new HashMap<>());
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
                toPlatformPlayer(event.getPlayer()), ActionType.FIRE_USE,
                toPlatformLocation(event.getBlock().getLocation()),
                "BLOCK_IGNITE_" + event.getCause().name(), new HashMap<>());
        CheckResult result = pipeline.process(info);
        if (result.isCancelAction()) {
            event.setCancelled(true);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isRedstoneRelated(Material material) {
        String name = material.name();
        return name.contains("BUTTON") || name.contains("LEVER") || name.contains("PRESSURE_PLATE")
                || name.equals("DISPENSER") || name.equals("DROPPER") || name.equals("TNT");
    }
}
