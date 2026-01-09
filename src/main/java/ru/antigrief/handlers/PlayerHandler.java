package ru.antigrief.handlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import ru.antigrief.AntiGriefSystem;
import ru.antigrief.data.PlayerData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;

public class PlayerHandler implements Listener {

    private final AntiGriefSystem plugin;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerHandler(AntiGriefSystem plugin) {
        this.plugin = plugin;
        startPlaytimeTask();
    }

    private void startPlaytimeTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PlayerData data = cache.get(player.getUniqueId());
                    if (data != null) {
                        // Add 1 minute (60000ms) roughly, or just track diff?
                        // Simplest: This task runs every 1200 ticks (1 minute).
                        // So we add 1 minute.
                        data.addPlaytime(60000);
                        checkTrust(player, data);
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 1200L, 1200L);
    }

    private void checkTrust(Player player, PlayerData data) {
        if (!data.isTrusted()) {
            if (data.getPlaytime() >= plugin.getConfigManager().getTrustedPlaytimeNeeded()) {
                data.setTrusted(true);
                // Sync to main thread for API calls or messages
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Component message = plugin.getLocaleManager().getPrefix()
                                .append(plugin.getLocaleManager().getComponent("promoted-message"));
                        player.sendMessage(message);
                        // Notify Discord
                        plugin.getDiscordManager().sendWebhook("promoted", java.util.Map.of(
                                "player", player.getName()));
                    }
                }.runTask(plugin);
                plugin.getDatabaseManager().savePlayer(data);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        new BukkitRunnable() {
            @Override
            public void run() {
                PlayerData data = plugin.getDatabaseManager().loadPlayer(uuid);
                cache.put(uuid, data);
            }
        }.runTaskAsynchronously(plugin);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        // Remove from cache and save
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getDatabaseManager().savePlayer(data);
                }
            }.runTaskAsynchronously(plugin);
        }
    }

    public void saveAll() {
        for (PlayerData data : cache.values()) {
            plugin.getDatabaseManager().savePlayer(data);
        }
    }

    public PlayerData getData(UUID uuid) {
        return cache.get(uuid);
    }

    // For manual trust update often used by commands
    public void updateAndSave(UUID uuid, PlayerData data) {
        cache.put(uuid, data);
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getDatabaseManager().savePlayer(data);
            }
        }.runTaskAsynchronously(plugin);
    }
}
