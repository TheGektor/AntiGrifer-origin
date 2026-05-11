package ru.antigrief.paper.task;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import ru.antigrief.core.context.PlayerContextManager;

/**
 * Каждые 60 секунд начисляет плейтайм всем онлайн-игрокам и сохраняет их данные.
 * Защищает от потери прогресса при краше сервера (максимум потеря 60 сек).
 */
public class PlaytimeTracker extends BukkitRunnable {
    private static final long TICK_SECONDS = 60L;

    private final PlayerContextManager contextManager;

    public PlaytimeTracker(PlayerContextManager contextManager) {
        this.contextManager = contextManager;
    }

    /** Запускает задачу. Период — раз в 60 секунд (1200 тиков). */
    public static PlaytimeTracker start(Plugin plugin, PlayerContextManager contextManager) {
        PlaytimeTracker tracker = new PlaytimeTracker(contextManager);
        tracker.runTaskTimer(plugin, 1200L, 1200L);
        return tracker;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            var ctx = contextManager.getContext(player.getUniqueId());
            ctx.addPlaytime(TICK_SECONDS);
            contextManager.savePlayer(player.getUniqueId());
        }
    }
}
