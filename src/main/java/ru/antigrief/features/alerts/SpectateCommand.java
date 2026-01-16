package ru.antigrief.features.alerts;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import ru.antigrief.AntiGriefSystem;

public class SpectateCommand implements CommandExecutor {

    public SpectateCommand(AntiGriefSystem plugin) {
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Usage: /ags internal spectate <target>
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        Player moderator = (Player) sender;
        if (!moderator.hasPermission("ags.alerts")) {
            moderator.sendMessage(Component.text("У вас нет прав!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            moderator.sendMessage(Component.text("Использование: /ags internal spectate <игрок>", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            moderator.sendMessage(Component.text("Игрок не найден.", NamedTextColor.RED));
            return true;
        }

        // Action
        moderator.setGameMode(GameMode.SPECTATOR);
        moderator.teleport(target);
        moderator.sendMessage(Component.text("Вы перешли в режим наблюдения за " + target.getName(), NamedTextColor.GREEN));

        return true;
    }
}
