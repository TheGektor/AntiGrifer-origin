package ru.antigrief.paper;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.antigrief.core.debug.DebugService;
import ru.antigrief.core.context.PlayerContextManager;
import ru.antigrief.api.PlayerContext;

import java.util.UUID;

public class AntiGriferCommand implements CommandExecutor {
    private final DebugService debugService;
    private final PlayerContextManager contextManager;

    public AntiGriferCommand(DebugService debugService, PlayerContextManager contextManager) {
        this.debugService = debugService;
        this.contextManager = contextManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("antigrifer.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§bAntiGrifer v3 §7- Use /antigrifer debug <player> or /antigrifer module <action>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "debug":
                if (args.length < 2) {
                    if (sender instanceof Player player) {
                        debugService.startSession(player.getUniqueId(), player::sendMessage);
                        sender.sendMessage("§aDebug mode enabled for you.");
                    }
                    return true;
                }
                // Report theoretical player
                // In a real plugin, we'd lookup by name
                break;
            case "module":
                sender.sendMessage("§eModule management: reload logic not fully implemented in this demo.");
                break;
            default:
                sender.sendMessage("§cUnknown subcommand.");
        }

        return true;
    }
}
