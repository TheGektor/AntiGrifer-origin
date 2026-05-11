package ru.antigrief.paper.listener;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import ru.antigrief.core.alert.AlertSystem;
import ru.antigrief.core.alert.AlertTracker;
import ru.antigrief.core.context.PlayerContextManager;
import ru.antigrief.core.trust.TrustConfig;
import ru.antigrief.core.trust.TrustTier;
import ru.antigrief.paper.tracking.BlockPlaceTracker;

import java.util.Set;
import java.util.UUID;

/**
 * Блокирует КОСВЕННЫЙ редстоун-гриф:
 * — взрыв TNT поставленного недоверенным игроком
 * — диспенсер выбрасывающий лаву/огонь, поставленный недоверенным игроком
 */
public class RedstoneActivationListener implements Listener {

    private static final Set<Material> DANGEROUS_DISPENSE = Set.of(
            Material.LAVA_BUCKET,
            Material.FIRE_CHARGE,
            Material.TNT,
            Material.FLINT_AND_STEEL
    );

    private static final Set<Material> TRACKED_BLOCKS = Set.of(
            Material.TNT,
            Material.DISPENSER,
            Material.DROPPER
    );

    private final BlockPlaceTracker placeTracker;
    private final PlayerContextManager contextManager;
    private final TrustConfig trustConfig;
    private final AlertSystem alertSystem;
    private final AlertTracker alertTracker;

    public RedstoneActivationListener(BlockPlaceTracker placeTracker,
                                      PlayerContextManager contextManager,
                                      TrustConfig trustConfig,
                                      AlertSystem alertSystem,
                                      AlertTracker alertTracker) {
        this.placeTracker = placeTracker;
        this.contextManager = contextManager;
        this.trustConfig = trustConfig;
        this.alertSystem = alertSystem;
        this.alertTracker = alertTracker;
    }

    /** Отслеживаем постановку опасных блоков (TNT, диспенсер, дроппер). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDangerousBlockPlace(BlockPlaceEvent event) {
        if (TRACKED_BLOCKS.contains(event.getBlock().getType())) {
            placeTracker.track(event.getBlock().getLocation(), event.getPlayer().getUniqueId());
        }
    }

    /** Удаляем запись при разрушении блока. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        placeTracker.forget(event.getBlock().getLocation());
    }

    /**
     * Блокируем взрыв TNT если его поставил недоверенный игрок.
     * entity.getSource() — игрок поджёгший TNT; если null — проверяем BlockPlaceTracker.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed tnt)) return;

        UUID placerUUID = resolveTntPlacer(tnt);
        if (placerUUID == null) return;

        var ctx = contextManager.getContext(placerUUID);
        TrustTier tier = TrustTier.resolve(ctx, trustConfig);

        if (tier.getLevel() < TrustTier.TIER_2.getLevel()) {
            // Отменяем разрушение блоков (взрыв отменён полностью)
            event.blockList().clear();

            Player placer = findOnlinePlayer(placerUUID, tnt);
            String playerName = placer != null ? placer.getName() : placerUUID.toString().substring(0, 8);

            alertTracker.report(placerUUID, "Взрыв TNT (косвенный)");
            alertSystem.dispatch("Недоверенный игрок активировал TNT (взрыв отменён)",
                    AlertSystem.AlertLevel.HIGH, playerName);
        }
    }

    /**
     * Блокируем диспенсер если он выбрасывает опасный предмет
     * и был поставлен недоверенным игроком.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event) {
        ItemStack item = event.getItem();
        if (!DANGEROUS_DISPENSE.contains(item.getType())) return;

        UUID placerUUID = placeTracker.getPlacedBy(event.getBlock().getLocation());
        if (placerUUID == null) return;

        var ctx = contextManager.getContext(placerUUID);
        TrustTier tier = TrustTier.resolve(ctx, trustConfig);

        if (tier.getLevel() < TrustTier.TIER_1.getLevel()) {
            event.setCancelled(true);
            Player placer = event.getBlock().getWorld().getPlayers().stream()
                    .filter(p -> p.getUniqueId().equals(placerUUID))
                    .findFirst().orElse(null);
            String playerName = placer != null ? placer.getName() : placerUUID.toString();

            alertTracker.report(placerUUID, "Диспенсер с " + item.getType().name());
            alertSystem.dispatch("Недоверенный игрок: диспенсер пытался выдать " + item.getType().name(),
                    AlertSystem.AlertLevel.HIGH, playerName);
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private java.util.UUID resolveTntPlacer(TNTPrimed tnt) {
        // Попытка 1: игрок поджёгший TNT напрямую
        Entity source = tnt.getSource();
        if (source instanceof Player p) return p.getUniqueId();

        // Попытка 2: BlockPlaceTracker — кто поставил блок TNT на этом месте
        return placeTracker.getPlacedBy(tnt.getLocation().getBlock().getLocation());
    }

    private Player findOnlinePlayer(java.util.UUID uuid, org.bukkit.entity.Entity near) {
        return near.getWorld().getPlayers().stream()
                .filter(p -> p.getUniqueId().equals(uuid))
                .findFirst().orElse(null);
    }
}
