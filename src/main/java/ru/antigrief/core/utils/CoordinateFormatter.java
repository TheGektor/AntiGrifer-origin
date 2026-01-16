package ru.antigrief.core.utils;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Утилита для форматирования координат.
 *
 * @author Antag0nis1
 */
public class CoordinateFormatter {

    /**
     * Форматирует локацию в строку с MiniMessage.
     * Пример: "world: 100, 64, -200" с цветами.
     */
    public static String format(@NotNull Location location) {
        if (location.getWorld() == null) return "Invalid Location";
        
        return String.format(
                "<gray>%s</gray><dark_gray>:</dark_gray> <green>%d</green><gray>,</gray> <green>%d</green><gray>,</gray> <green>%d</green>",
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    /**
     * Форматирует локацию в простую строку (для логов).
     */
    public static String formatPlain(@NotNull Location location) {
        if (location.getWorld() == null) return "Invalid Location";
        
        return String.format(
                "%s: %d, %d, %d",
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }
}
