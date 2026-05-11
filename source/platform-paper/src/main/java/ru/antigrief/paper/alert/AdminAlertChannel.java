package ru.antigrief.paper.alert;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.antigrief.core.alert.AlertSystem;
import ru.antigrief.core.alert.AlertTracker;
import ru.antigrief.core.config.ConfigManager;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class AdminAlertChannel implements AlertSystem.AlertChannel {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ConfigManager config;
    private final AlertTracker alertTracker;

    public AdminAlertChannel(ConfigManager config, AlertTracker alertTracker) {
        this.config = config;
        this.alertTracker = alertTracker;
    }

    @Override
    public void send(String message, AlertSystem.AlertLevel level, String suspectName) {
        String time = LocalTime.now().format(TIME_FMT);
        String alertMsgTemplate = config.getString("messages.admin_alert",
                "&6[AntiGrifer] &eПодозрение на гриф: &f%player% &7(%reason%)");
        String tpButtonText  = config.getString("messages.admin_tp_button", "&b[ТЕЛЕПОРТ]");
        String tpHoverTpl    = config.getString("messages.admin_tp_hover",
                "&fНажмите, чтобы телепортироваться к %player%");
        String ackButtonText = config.getString("messages.admin_ack_button", "&a[✔ ПРИНЯТО]");

        String displayName = suspectName != null ? suspectName : "Неизвестно";
        String formatted = alertMsgTemplate
                .replace("%player%", displayName)
                .replace("%reason%", message);

        // Префикс с временем и уровнем
        Component levelColor = levelBadge(level);
        Component timeComp = Component.text("[" + time + "] ", NamedTextColor.DARK_GRAY);
        Component mainMsg = timeComp
                .append(levelColor)
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(formatted));

        if (suspectName != null) {
            // Кнопка телепорта
            Component tpBtn = LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(" " + tpButtonText)
                    .clickEvent(ClickEvent.runCommand("/antigrifer tp " + suspectName))
                    .hoverEvent(HoverEvent.showText(
                            LegacyComponentSerializer.legacyAmpersand().deserialize(
                                    tpHoverTpl.replace("%player%", suspectName))));

            // Кнопка "принято"
            Component ackBtn = LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(" " + ackButtonText)
                    .clickEvent(ClickEvent.runCommand("/antigrifer ack " + suspectName))
                    .hoverEvent(HoverEvent.showText(
                            Component.text("Отметить как обработанный алерт", NamedTextColor.GREEN)));

            mainMsg = mainMsg.append(tpBtn).append(ackBtn);

            // Регистрируем в трекере
            try {
                // suspectName может быть не UUID — ищем игрока
                Player online = Bukkit.getPlayerExact(suspectName);
                if (online != null) {
                    alertTracker.report(online.getUniqueId(), message);
                }
            } catch (Exception ignored) { }
        }

        Component finalMsg = mainMsg;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("antigrifer.admin")) {
                p.sendMessage(finalMsg);
            }
        }
    }

    private Component levelBadge(AlertSystem.AlertLevel level) {
        return switch (level) {
            case LOW      -> Component.text("[LOW] ",      NamedTextColor.GRAY);
            case MEDIUM   -> Component.text("[MEDIUM] ",   NamedTextColor.YELLOW);
            case HIGH     -> Component.text("[HIGH] ",     NamedTextColor.RED);
            case CRITICAL -> Component.text("[CRITICAL] ", NamedTextColor.DARK_RED);
        };
    }
}
