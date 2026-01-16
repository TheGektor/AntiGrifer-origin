package ru.antigrief.features.playtime;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Слушатель входа/выхода для трекера времени.
 *
 * @author Antag0nis1
 */
public class PlaytimeListener implements Listener {

    private final PlaytimeManager playtimeManager;

    public PlaytimeListener(PlaytimeManager playtimeManager) {
        this.playtimeManager = playtimeManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        playtimeManager.startSession(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playtimeManager.endSession(event.getPlayer().getUniqueId());
    }
}
