package ru.antigrief.features.criticallocations;

import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;

public class CriticalLocation {

    private final String name;
    private final World world;
    
    // Store X and Z coordinates packed into a long: (x & 0xFFFFFFFFL) | ((z & 0xFFFFFFFFL) << 32)
    private final Set<Long> coordinates;
    
    // Bounding box for fast rejection before checking Set
    private final int minX, minZ, maxX, maxZ;

    public CriticalLocation(String name, World world, Set<Long> coordinates) {
        this.name = name;
        this.world = world;
        this.coordinates = coordinates;
        
        int tempMinX = Integer.MAX_VALUE;
        int tempMinZ = Integer.MAX_VALUE;
        int tempMaxX = Integer.MIN_VALUE;
        int tempMaxZ = Integer.MIN_VALUE;
        
        for (long packed : coordinates) {
            int x = getX(packed);
            int z = getZ(packed);
            if (x < tempMinX) tempMinX = x;
            if (x > tempMaxX) tempMaxX = x;
            if (z < tempMinZ) tempMinZ = z;
            if (z > tempMaxZ) tempMaxZ = z;
        }
        
        this.minX = tempMinX;
        this.minZ = tempMinZ;
        this.maxX = tempMaxX;
        this.maxZ = tempMaxZ;
    }

    public String getName() {
        return name;
    }

    public World getWorld() { 
        return world; 
    }
    
    public Set<Long> getCoordinates() {
        return coordinates;
    }

    public boolean contains(Location loc) {
        if (world == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equals(world.getName())) return false;
        
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        
        // Fast rejection
        if (x < minX || x > maxX || z < minZ || z > maxZ) {
            return false;
        }
        
        return coordinates.contains(packXYZ(x, z));
    }
    
    // Helpers
    public static long packXYZ(int x, int z) {
        return (x & 0xFFFFFFFFL) | ((z & 0xFFFFFFFFL) << 32);
    }
    
    public static int getX(long packed) {
        return (int) (packed & 0xFFFFFFFFL);
    }
    
    public static int getZ(long packed) {
        return (int) (packed >>> 32);
    }
}
