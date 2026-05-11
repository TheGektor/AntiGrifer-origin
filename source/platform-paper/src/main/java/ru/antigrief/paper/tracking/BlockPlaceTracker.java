package ru.antigrief.paper.tracking;

import org.bukkit.Location;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Трекинг опасных блоков: записывает кто и где поставил TNT, диспенсер и т.п.
 * При взрыве/срабатывании диспенсера можно найти виновника.
 */
public class BlockPlaceTracker {
    private static final int MAX_SIZE = 10_000;

    // LinkedHashMap в режиме LRU: при превышении MAX_SIZE удаляем самый старый
    private final Map<String, UUID> placements = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, UUID> eldest) {
            return size() > MAX_SIZE;
        }
    };

    /** Запоминает что игрок uuid поставил блок в loc. */
    public synchronized void track(Location loc, UUID uuid) {
        placements.put(key(loc), uuid);
    }

    /** Возвращает UUID игрока поставившего блок или null. */
    public synchronized UUID getPlacedBy(Location loc) {
        return placements.get(key(loc));
    }

    /** Убирает запись о блоке (например при его разрушении). */
    public synchronized void forget(Location loc) {
        placements.remove(key(loc));
    }

    private String key(Location loc) {
        String world = loc.getWorld() != null ? loc.getWorld().getName() : "world";
        return world + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }
}
