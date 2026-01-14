package ru.antigrief.features.feedback;

import ru.antigrief.AntiGriefSystem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class FeedbackManager {

    private final AntiGriefSystem plugin;
    private final DiscordService discordService;

    public FeedbackManager(AntiGriefSystem plugin) {
        this.plugin = plugin;
        this.discordService = new DiscordService();
        initializeTable();
    }

    private void initializeTable() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            // Using SQLite
            stmt.execute("CREATE TABLE IF NOT EXISTS feedback_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "server_ip VARCHAR(50), " +
                    "message TEXT NOT NULL, " +
                    "timestamp LONG NOT NULL, " +
                    "status VARCHAR(20) DEFAULT 'SENT')");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not initialize feedback table", e);
        }
    }

    public void sendFeedback(String playerName, UUID uuid, String message, String serverIp) {
        // 1. Send to Discord
        discordService.sendFeedback(playerName, message, serverIp);

        // 2. Save to DB async
        long now = System.currentTimeMillis();
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO feedback_history (uuid, server_ip, message, timestamp, status) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, serverIp);
                ps.setString(3, message);
                ps.setLong(4, now);
                ps.setString(5, "SENT");
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save feedback to DB", e);
            }
        });
    }

    public CompletableFuture<List<String>> getHistory(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
             List<String> history = new ArrayList<>();
             SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
             
             try (Connection conn = plugin.getDatabaseManager().getConnection();
                  PreparedStatement ps = conn.prepareStatement(
                          "SELECT message, timestamp FROM feedback_history WHERE uuid = ? ORDER BY timestamp DESC LIMIT 10")) {
                 ps.setString(1, uuid.toString());
                 try (ResultSet rs = ps.executeQuery()) {
                     while(rs.next()) {
                         String msg = rs.getString("message");
                         long time = rs.getLong("timestamp");
                         String dateStr = sdf.format(new Date(time));
                         history.add(dateStr + ": " + msg);
                     }
                 }
             } catch (SQLException e) {
                 plugin.getLogger().log(Level.SEVERE, "Failed to fetch feedback history", e);
             }
             return history;
        });
    }
}
