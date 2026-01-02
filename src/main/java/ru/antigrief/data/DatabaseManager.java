package ru.antigrief.data;

import ru.antigrief.AntiGriefSystem;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final AntiGriefSystem plugin;
    private Connection connection;

    public DatabaseManager(AntiGriefSystem plugin) {
        this.plugin = plugin;
        initializeDatabase();
    }

    private void initializeDatabase() {
        File dataFolder = new File(plugin.getDataFolder(), "database.db");
        if (!dataFolder.getParentFile().exists()) {
            dataFolder.getParentFile().mkdirs();
        }

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS players (" +
                        "uuid TEXT PRIMARY KEY, " +
                        "playtime LONG, " +
                        "trusted BOOLEAN)");
            }
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not initialize database", e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public PlayerData loadPlayer(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long playtime = rs.getLong("playtime");
                boolean trusted = rs.getBoolean("trusted");
                return new PlayerData(uuid, playtime, trusted);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading player " + uuid, e);
        }
        return new PlayerData(uuid, 0, false);
    }

    public void savePlayer(PlayerData data) {
        try (PreparedStatement ps = connection.prepareStatement(
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
