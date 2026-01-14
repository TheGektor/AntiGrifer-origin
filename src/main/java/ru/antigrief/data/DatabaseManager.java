package ru.antigrief.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ru.antigrief.AntiGriefSystem;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final AntiGriefSystem plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(AntiGriefSystem plugin) {
        this.plugin = plugin;
        initializeDatabase();
    }

    private void initializeDatabase() {
        File dbFile = new File(plugin.getDataFolder(), "database.db");
        if (!dbFile.getParentFile().exists()) {
            boolean created = dbFile.getParentFile().mkdirs();
            if (!created && !dbFile.getParentFile().exists()) {
                 plugin.getLogger().severe("Could not create database directory!");
                 return;
            }
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(10);
        config.setPoolName("AntiGriefPool");
        config.setConnectionTestQuery("SELECT 1");

        try {
            this.dataSource = new HikariDataSource(config);
            try (Connection conn = dataSource.getConnection();
                 Statement statement = conn.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS players (" +
                        "uuid TEXT PRIMARY KEY, " +
                        "playtime LONG, " +
                        "trusted BOOLEAN)");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not initialize database", e);
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) throw new SQLException("DataSource is not initialized.");
        return dataSource.getConnection();
    }

    public PlayerData loadPlayer(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long playtime = rs.getLong("playtime");
                    boolean trusted = rs.getBoolean("trusted");
                    return new PlayerData(uuid, playtime, trusted);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading player " + uuid, e);
        }
        return new PlayerData(uuid, 0, false);
    }

    public void savePlayer(PlayerData data) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO players (uuid, playtime, trusted) VALUES (?, ?, ?)")) {
            ps.setString(1, data.getUuid().toString());
            ps.setLong(2, data.getPlaytime());
            ps.setBoolean(3, data.isTrusted());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving player " + data.getUuid(), e);
        }
    }
}
