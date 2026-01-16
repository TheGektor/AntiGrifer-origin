package ru.antigrief.data;

import ru.antigrief.AntiGriefSystem;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final AntiGriefSystem plugin;
    private Connection connection;

    public DatabaseManager(AntiGriefSystem plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        try {
            // Ensure data folder exists
            if (!plugin.getDataFolder().exists()) {
                if (!plugin.getDataFolder().mkdirs()) {
                    plugin.getLogger().severe("Failed to create plugin directory!");
                    return false;
                }
            }

            // Drivers are loaded automatically via libraries in plugin.yml
            String dbPath = new java.io.File(plugin.getDataFolder(), "database.db").getAbsolutePath();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            createTables();
            plugin.getLogger().info("Database connected successfully.");
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Database connection failed (SQLException)", e);
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Database connection failed (General)", e);
            return false;
        }
    }

    private void createTables() {
        try (PreparedStatement ps = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS players (uuid VARCHAR(36) PRIMARY KEY, trusted BOOLEAN, playtime LONG)")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to create tables: " + e.getMessage());
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void ensureConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String dbPath = new java.io.File(plugin.getDataFolder(), "database.db").getAbsolutePath();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        }
    }

    public PlayerData loadPlayer(UUID uuid) {
        try {
            ensureConnection();
            try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM players WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return new PlayerData(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getLong("playtime"),
                            rs.getBoolean("trusted"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Return default data if not found or error
        return new PlayerData(uuid, 0, false);
    }

    public void savePlayer(PlayerData data) {
         try {
            ensureConnection();
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR REPLACE INTO players (uuid, trusted, playtime) VALUES (?, ?, ?)")) {
                ps.setString(1, data.getUuid().toString());
                ps.setBoolean(2, data.isTrusted());
                ps.setLong(3, data.getPlaytime());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public Connection getConnection() {
        return connection;
    }
}
