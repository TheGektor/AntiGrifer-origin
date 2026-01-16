package ru.antigrief.features.playtime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.plugin.Plugin;

import ru.antigrief.core.database.DatabaseManager;

/**
 * Менеджер отслеживания игрового времени.
 *
 * @author Antag0nis1
 */
public class PlaytimeManager {

    private final Plugin plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, Long> sessionStartTimes = new HashMap<>();

    public PlaytimeManager(Plugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public void startSession(UUID uuid) {
        sessionStartTimes.put(uuid, System.currentTimeMillis());
    }

    public void endSession(UUID uuid) {
        Long startTime = sessionStartTimes.remove(uuid);
        if (startTime != null) {
            long sessionDuration = System.currentTimeMillis() - startTime;
            savePlaytime(uuid, sessionDuration);
        }
    }

    private void savePlaytime(UUID uuid, long durationMillis) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE players SET playtime = playtime + ? WHERE uuid = ?")) {
                
                // Проверяем, существует ли запись (хотя init должен был создать при входе, но для надежности)
                // В данном примере полагаемся на то, что запись создается в другом месте или используем UPSERT
                // Для SQLite UPSERT: INSERT INTO... ON CONFLICT...
                // Но для простоты пока UPDATE, предполагая что игрок уже есть.
                // Улучшим до UPSERT:

                ps.close(); // закрываем предыдущий statement

                try (PreparedStatement upsert = conn.prepareStatement(
                        "INSERT INTO players (uuid, playtime) VALUES (?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET playtime = playtime + ?")) {
                    upsert.setString(1, uuid.toString());
                    upsert.setLong(2, durationMillis);
                    upsert.setLong(3, durationMillis);
                    upsert.executeUpdate();
                }

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save playtime for " + uuid, e);
            }
        });
    }
}
