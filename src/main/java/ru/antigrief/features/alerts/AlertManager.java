package ru.antigrief.features.alerts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ru.antigrief.core.utils.CoordinateFormatter;
import ru.antigrief.core.utils.MiniMessageUtils;

import java.util.UUID;

/**
 * Менеджер оповещений о грифе.
 *
 * @author Antag0nis1
 */
public class AlertManager {

    private final Plugin plugin;
    private final DiscordAlertService discordService;

    public AlertManager(Plugin plugin) {
        this.plugin = plugin;
        this.discordService = new DiscordAlertService(plugin);
    }

    /**
     * Отправляет алерт о подозрительном действии.
     *
     * @param player Игрок, совершивший действие
     * @param action Описание действия (например, "Placed TNT")
     * @param location Локация события
     */
    public void sendAlert(Player player, String action, Location location) {
        // 1. Отправка в Discord
        discordService.sendAlert(player, action, location);

        // 2. Формирование сообщения для администраторов
        String coords = CoordinateFormatter.format(location);
        String alertMessage = String.format(
                "<red><b>GRIEF ALERT</b></red> <gray>|</gray> <yellow>%s</yellow> <gray>-></gray> <red>%s</red> <gray>at</gray> %s",
                player.getName(),
                action,
                coords
        );

        Component component = MiniMessageUtils.parse(alertMessage);
        
        // Добавляем интерактивность: Клик -> ТП + Спектатор
        Component interactiveParams = MiniMessageUtils.parse(" <dark_gray>[</dark_gray><green><b>TP & SPECTATE</b></green><dark_gray>]</dark_gray>")
                .hoverEvent(HoverEvent.showText(MiniMessageUtils.parse("<gray>Click to teleport and switch to Spectator mode</gray>")))
                .clickEvent(ClickEvent.callback(audience -> {
                    if (audience instanceof Player admin) {
                        dispatchTeleport(admin, player, location);
                    }
                }));

        Component finalMessage = component.append(interactiveParams);

        // 3. Рассылка онлайн администраторам
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission("ags.alerts.view")) {
                MiniMessageUtils.sendMessage(onlinePlayer, finalMessage);
            }
        }
    }

    private void dispatchTeleport(Player admin, Player target, Location location) {
        // Переключаем в режим наблюдения
        if (admin.getGameMode() != GameMode.SPECTATOR) {
            admin.setGameMode(GameMode.SPECTATOR);
            MiniMessageUtils.sendMessage(admin, "<green>Switched to Spectator mode.</green>");
        }

        // Телепортируем
        if (target != null && target.isOnline()) {
            admin.teleport(target);
            MiniMessageUtils.sendMessage(admin, "<gray>Teleported to <yellow>" + target.getName() + "</yellow>.</gray>");
        } else {
            admin.teleport(location);
            MiniMessageUtils.sendMessage(admin, "<gray>Teleported to event location (Target offline).</gray>");
        }
    }
}
