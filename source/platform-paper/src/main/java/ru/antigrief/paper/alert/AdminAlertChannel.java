package ru.antigrief.paper.alert;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.antigrief.core.alert.AlertSystem;
import ru.antigrief.core.config.ConfigManager;

public class AdminAlertChannel implements AlertSystem.AlertChannel {
    private final ConfigManager config;

    public AdminAlertChannel(ConfigManager config) {
        this.config = config;
    }

    @Override
    public void send(String message, AlertSystem.AlertLevel level, String suspectName) {
        String alertMsgTemplate = config.getString("messages.admin_alert", "&6[AntiGrifer] &eПодозрение на гриф: &f%player% &7(%reason%)");
        String tpButtonText = config.getString("messages.admin_tp_button", "&b[ТЕЛЕПОРТ]");
        String tpHoverTemplate = config.getString("messages.admin_tp_hover", "&fНажмите, чтобы телепортироваться к %player%");

        String formattedAlert = alertMsgTemplate
                .replace("%player%", suspectName != null ? suspectName : "Неизвестно")
                .replace("%reason%", message);

        Component mainMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(formattedAlert);

        if (suspectName != null) {
            Component tpButton = LegacyComponentSerializer.legacyAmpersand().deserialize(" " + tpButtonText)
                    .clickEvent(ClickEvent.runCommand("/tp " + suspectName))
                    .hoverEvent(HoverEvent.showText(LegacyComponentSerializer.legacyAmpersand().deserialize(
                            tpHoverTemplate.replace("%player%", suspectName)
                    )));
            mainMessage = mainMessage.append(tpButton);
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission("antigrifer.admin")) {
                onlinePlayer.sendMessage(mainMessage);
            }
        }
    }
}
