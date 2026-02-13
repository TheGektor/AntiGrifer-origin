package ru.antigrief.features.criticallocations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import ru.antigrief.AntiGriefSystem;
import ru.antigrief.data.DatabaseManager;

public class CriticalLocationManager {

    private final AntiGriefSystem plugin;
    private final DatabaseManager databaseManager;
    private final Map<String, CriticalLocation> locations = new HashMap<>();
    
    // Temporary storage for player selections
    private final Map<UUID, Location> pos1Selections = new HashMap<>();
    private final Map<UUID, Location> pos2Selections = new HashMap<>();

    public CriticalLocationManager(AntiGriefSystem plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        initializeTable();
        loadLocations();
    }

    private void initializeTable() {
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS critical_locations (" +
                    "name TEXT PRIMARY KEY, " +
                    "world TEXT NOT NULL, " +
                    "min_x INTEGER, min_y INTEGER, min_z INTEGER, " +
                    "max_x INTEGER, max_y INTEGER, max_z INTEGER)");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not initialize critical_locations table", e);
        }
    }

    private void loadLocations() {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM critical_locations")) {

                while (rs.next()) {
                    String name = rs.getString("name");
                    String worldName = rs.getString("world");
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) continue;

                    CriticalLocation loc = new CriticalLocation(
                            name,
                            world,
                            rs.getInt("min_x"),
                            rs.getInt("min_y"),
                            rs.getInt("min_z"),
                            rs.getInt("max_x"),
                            rs.getInt("max_y"),
                            rs.getInt("max_z")
                    );
                    synchronized (locations) {
                        locations.put(name, loc);
                    }
                }
                plugin.getLogger().info("Loaded " + locations.size() + " critical locations.");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error loading critical locations", e);
            }
        });
    }

    public void setPos1(Player player) {
        pos1Selections.put(player.getUniqueId(), player.getLocation().getBlock().getLocation());
    }

    public void setPos1(Player player, Location loc) {
        pos1Selections.put(player.getUniqueId(), loc);
    }

    public void setPos2(Player player) {
        pos2Selections.put(player.getUniqueId(), player.getLocation().getBlock().getLocation());
    }

    public void setPos2(Player player, Location loc) {
        pos2Selections.put(player.getUniqueId(), loc);
    }

    public Location getPos1(Player player) {
        return pos1Selections.get(player.getUniqueId());
    }

    public Location getPos2(Player player) {
        return pos2Selections.get(player.getUniqueId());
    }

    public boolean createLocation(String name, Player creator) {
        Location p1 = getPos1(creator);
        Location p2 = getPos2(creator);

        if (p1 == null || p2 == null) return false;
        if (!p1.getWorld().equals(p2.getWorld())) return false;

        CriticalLocation loc = new CriticalLocation(name, p1, p2);

        // Save to DB Async
        CompletableFuture.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT OR REPLACE INTO critical_locations VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, loc.getName());
                ps.setString(2, loc.getWorld().getName());
                ps.setInt(3, loc.getMinX());
                ps.setInt(4, loc.getMinY());
                ps.setInt(5, loc.getMinZ());
                ps.setInt(6, loc.getMaxX());
                ps.setInt(7, loc.getMaxY());
                ps.setInt(8, loc.getMaxZ());
                ps.executeUpdate();
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
        CompletableFuture.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM critical_locations WHERE name = ?")) {
                ps.setString(1, name);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error deleting critical location " + name, e);
            }
        });
    }

    public CriticalLocation getCriticalLocation(Location location) {
        if (location == null || location.getWorld() == null) return null;

        synchronized (locations) {
            for (CriticalLocation loc : locations.values()) {
                // Detailed Debug log
                /*
                plugin.getLogger().info(String.format("[DEBUG] Checking region '%s' in world '%s' bounds [%d,%d,%d] to [%d,%d,%d] against loc [%s, %d,%d,%d]", 
                    loc.getName(), 
                    loc.getWorld().getName(),
                    loc.getMinX(), loc.getMinY(), loc.getMinZ(),
                    loc.getMaxX(), loc.getMaxY(), loc.getMaxZ(),
                    location.getWorld().getName(),
                    location.getBlockX(), location.getBlockY(), location.getBlockZ()
                ));
                */
                
                if (loc.contains(location)) {
                    // plugin.getLogger().info("[DEBUG] Location MATCHED region: " + loc.getName());
                    return loc;
                }
            }
        }
        return null;
    }

    public Map<String, CriticalLocation> getAllLocations() {
        synchronized (locations) {
            return new HashMap<>(locations);
        }
    }
}
