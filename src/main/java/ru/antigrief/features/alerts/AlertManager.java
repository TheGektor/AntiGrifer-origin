package ru.antigrief.features.alerts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.antigrief.AntiGriefSystem;

import java.util.Map;

public class AlertManager {

    private final AntiGriefSystem plugin;
    private final MiniMessage miniMessage;

    public AlertManager(AntiGriefSystem plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public void sendAlert(Player suspect, String action, Material material, String detail) {
        String itemName = material != null ? material.name() : (detail != null ? detail : "Unknown");
        
        // 1. In-game broadcast to admins
        Component message = miniMessage.deserialize(
                "<gradient:#FF512F:#DD2476><bold>[AGS]</bold></gradient> <dark_gray>| <white><suspect> <gray>-> <red><action> <yellow><item> <dark_gray>@ <gold><location>",
                Placeholder.component("suspect", Component.text(suspect.getName())),
                Placeholder.unparsed("action", action),
                Placeholder.unparsed("item", itemName),
                Placeholder.unparsed("location", getLocationString(suspect))
        );

        Component spectateButton = miniMessage.deserialize(" <white>[<gradient:#00F260:#0575E6><bold>СЛЕЖКА</bold></gradient><white>]")
                .clickEvent(ClickEvent.runCommand("/ags internal spectate " + suspect.getName()))
                .hoverEvent(HoverEvent.showText(Component.text("Нажмите для слежки", net.kyori.adventure.text.format.NamedTextColor.GREEN)));

        Component finalMessage = message.append(spectateButton);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("ags.alerts")) {
                p.sendMessage(finalMessage);
            }
        }

        // 2. Discord Webhook
        plugin.getDiscordManager().sendWebhook("suspicious-activity", Map.of(
                "player", suspect.getName(),
                "item", itemName,
                "action", action,
                "location", getLocationString(suspect)
        ));
    }

    private String getLocationString(Player player) {
        return player.getLocation().getBlockX() + ", " +
               player.getLocation().getBlockY() + ", " +
               player.getLocation().getBlockZ();
    }
}
