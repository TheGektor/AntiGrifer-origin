package ru.antigrief.features.criticallocations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import ru.antigrief.AntiGriefSystem;
import ru.antigrief.data.DatabaseManager;

public class CriticalLocationManager {

    private final AntiGriefSystem plugin;
    private final DatabaseManager databaseManager;
    private final Map<String, CriticalLocation> locations = new HashMap<>();
    
    // Store players who have trust permissions for specific regions
    // Format: region_name -> set of UUIDs
    private final Map<String, Set<UUID>> trustedPlayers = new HashMap<>();

    public CriticalLocationManager(AntiGriefSystem plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        initializeTables();
        loadLocations();
    }

    private void initializeTables() {
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Critical Locations Table (New Schema for Polygons)
            stmt.execute("CREATE TABLE IF NOT EXISTS critical_locations_v3 (" +
                    "name TEXT PRIMARY KEY, " +
                    "world TEXT NOT NULL)");

            // Critical Location Blocks Table
            stmt.execute("CREATE TABLE IF NOT EXISTS critical_location_blocks (" +
                    "location_name TEXT NOT NULL, " +
                    "x INTEGER NOT NULL, " +
                    "z INTEGER NOT NULL, " +
                    "PRIMARY KEY (location_name, x, z))");

            // Permissions Table
            stmt.execute("CREATE TABLE IF NOT EXISTS critical_permissions (" +
                    "location_name TEXT NOT NULL, " +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "PRIMARY KEY (location_name, player_uuid))");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not initialize critical tables", e);
        }
    }

    private void loadLocations() {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                // Load Locations
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM critical_locations_v3")) {
                     
                    while (rs.next()) {
                        String name = rs.getString("name");
                        String worldName = rs.getString("world");
                        World world = Bukkit.getWorld(worldName);
                        if (world == null) continue;

                        // Now load all coordinates for this location
                        Set<Long> coordinates = new HashSet<>();
                        try (PreparedStatement coordsStmt = conn.prepareStatement("SELECT x, z FROM critical_location_blocks WHERE location_name = ?")) {
                            coordsStmt.setString(1, name);
                            try (ResultSet coordsRs = coordsStmt.executeQuery()) {
                                while (coordsRs.next()) {
                                    int x = coordsRs.getInt("x");
                                    int z = coordsRs.getInt("z");
                                    coordinates.add(CriticalLocation.packXYZ(x, z));
                                }
                            }
                        }
                        
                        // Avoid holding the lock during query
                        CriticalLocation loc = new CriticalLocation(name, world, coordinates);
                        synchronized (locations) {
                            locations.put(name, loc);
                        }
                    }
                }
                
                // Load Permissions
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM critical_permissions")) {
                    while (rs.next()) {
                        String name = rs.getString("location_name");
                        UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                        synchronized (trustedPlayers) {
                            trustedPlayers.computeIfAbsent(name, k -> new HashSet<>()).add(uuid);
                        }
                    }
                }

                plugin.getLogger().info("Loaded " + locations.size() + " critical locations.");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error loading critical locations", e);
            }
        });
    }

    public boolean createLocation(String name, World world, Set<Long> coordinates) {
        CriticalLocation loc = new CriticalLocation(name, world, coordinates);

        // Save to DB Async
        CompletableFuture.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                conn.setAutoCommit(false); // Use transaction for bulk insert
                try {
                    try (PreparedStatement ps = conn.prepareStatement(
                             "INSERT OR REPLACE INTO critical_locations_v3 (name, world) VALUES (?, ?)")) {
                        ps.setString(1, loc.getName());
                        ps.setString(2, loc.getWorld().getName());
                        ps.executeUpdate();
                    }
                    
                    try (PreparedStatement ps = conn.prepareStatement(
                             "INSERT OR IGNORE INTO critical_location_blocks (location_name, x, z) VALUES (?, ?, ?)")) {
                        
                        int count = 0;
                        for (long packed : coordinates) {
                            ps.setString(1, loc.getName());
                            ps.setInt(2, CriticalLocation.getX(packed));
                            ps.setInt(3, CriticalLocation.getZ(packed));
                            ps.addBatch();
                            
                            if (++count % 1000 == 0) {
                                ps.executeBatch(); // Execute in batches of 1000
                            }
                        }
                        ps.executeBatch(); // Execute remaining
                    }
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error saving critical location " + name, e);
            }
        });

        synchronized (locations) {
            locations.put(name, loc);
        }
        return true;
    }

    public void deleteLocation(String name) {
        synchronized (locations) {
            locations.remove(name);
        }
        synchronized (trustedPlayers) {
            trustedPlayers.remove(name);
        }
        CompletableFuture.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM critical_locations_v3 WHERE name = ?")) {
                    ps.setString(1, name);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM critical_location_blocks WHERE location_name = ?")) {
                    ps.setString(1, name);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM critical_permissions WHERE location_name = ?")) {
                    ps.setString(1, name);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error deleting critical location " + name, e);
            }
        });
    }

    public CriticalLocation getCriticalLocation(Location location) {
        if (location == null || location.getWorld() == null) return null;

        synchronized (locations) {
            for (CriticalLocation loc : locations.values()) {
                if (loc.contains(location)) {
                    return loc;
                }
            }
        }
        return null;
    }
    
    public CriticalLocation getLocationByName(String name) {
        synchronized (locations) {
            return locations.get(name);
        }
    }

    public Map<String, CriticalLocation> getAllLocations() {
        synchronized (locations) {
            return new HashMap<>(locations);
        }
    }

    public void setPlayerTrust(String regionName, UUID uuid, boolean trusted) {
        synchronized (trustedPlayers) {
            Set<UUID> players = trustedPlayers.computeIfAbsent(regionName, k -> new HashSet<>());
            if (trusted) {
                players.add(uuid);
            } else {
                players.remove(uuid);
            }
        }
        
        CompletableFuture.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                if (trusted) {
                    try (PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO critical_permissions (location_name, player_uuid) VALUES (?, ?)")) {
                        ps.setString(1, regionName);
                        ps.setString(2, uuid.toString());
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM critical_permissions WHERE location_name = ? AND player_uuid = ?")) {
                        ps.setString(1, regionName);
                        ps.setString(2, uuid.toString());
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error updating critical permissions for " + regionName, e);
            }
        });
    }

    public boolean isPlayerTrusted(String regionName, UUID uuid) {
        synchronized (trustedPlayers) {
            Set<UUID> players = trustedPlayers.get(regionName);
            return players != null && players.contains(uuid);
        }
    }
}
