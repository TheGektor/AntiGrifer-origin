package ru.antigrief.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import ru.antigrief.AntiGriefSystem;

public class DatabaseManager {

    private final AntiGriefSystem plugin;
    private HikariDataSource dataSource;

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

            String dbPath = new java.io.File(plugin.getDataFolder(), "database.db").getAbsolutePath();
            
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + dbPath);
            config.setDriverClassName("org.sqlite.JDBC");
            
            // SQLite specific configurations for Hikari
            config.setPoolName("AntiGriefSystemPool");
            config.setMaximumPoolSize(10);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            
            dataSource = new HikariDataSource(config);
            
            createTables();
            plugin.getLogger().info("Database connected successfully using HikariCP.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Database connection failed", e);
            return false;
        }
    }

    private void createTables() {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS players (uuid VARCHAR(36) PRIMARY KEY, trusted BOOLEAN, playtime LONG)")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create tables", e);
        }
    }

    public void closeConnection() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public PlayerData loadPlayer(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PlayerData(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getLong("playtime"),
                            rs.getBoolean("trusted"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player data for UUID: " + uuid, e);
        }
        // Return default data if not found or error
        return new PlayerData(uuid, 0, false);
    }

    public void savePlayer(PlayerData data) {
         try (Connection conn = getConnection();
              PreparedStatement ps = conn.prepareStatement(
                 "INSERT OR REPLACE INTO players (uuid, trusted, playtime) VALUES (?, ?, ?)")) {
             ps.setString(1, data.getUuid().toString());
             ps.setBoolean(2, data.isTrusted());
             ps.setLong(3, data.getPlaytime());
             ps.executeUpdate();
         } catch (SQLException e) {
             plugin.getLogger().log(Level.SEVERE, "Failed to save player data for UUID: " + data.getUuid(), e);
         }
    }
    
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("HikariDataSource is null or closed");
        }
        return dataSource.getConnection();
    }
}
