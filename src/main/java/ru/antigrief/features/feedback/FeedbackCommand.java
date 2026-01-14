package ru.antigrief.features.feedback;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.stream.Collectors;

public class FeedbackCommand implements CommandExecutor {

    private final FeedbackManager feedbackManager;

    public FeedbackCommand(FeedbackManager feedbackManager) {
        this.feedbackManager = feedbackManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        MiniMessage mm = MiniMessage.miniMessage();

        if (args.length == 0) {
            player.sendMessage(mm.deserialize("<red>Usage: /feedback <send|history> [message]"));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("send")) {
            if (!player.hasPermission("plugin.feedback.send")) {
                player.sendMessage(mm.deserialize("<red>You do not have permission to send feedback."));
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(mm.deserialize("<red>Usage: /feedback send <message>"));
                return true;
            }

            String message = Arrays.stream(args).skip(1).collect(Collectors.joining(" "));
            InetSocketAddress address = player.getAddress();
            String ip = (address != null) ? address.getHostString() : "Unknown";

            feedbackManager.sendFeedback(player.getName(), player.getUniqueId(), message, ip);
            
            player.sendMessage(mm.deserialize("<green>Feedback sent successfully! Thank you for your report."));
            return true;
        }

        if (sub.equals("history")) {
            if (!player.hasPermission("plugin.feedback.history")) {
                 player.sendMessage(mm.deserialize("<red>You do not have permission to view history."));
                 return true;
            }
            
            player.sendMessage(mm.deserialize("<yellow>Loading history..."));
            feedbackManager.getHistory(player.getUniqueId()).thenAccept(history -> {
                if (history.isEmpty()) {
                    player.sendMessage(mm.deserialize("<gray>No feedback history found."));
                } else {
                    player.sendMessage(mm.deserialize("<gold><bold>=== Your Feedback History ===</bold>"));
                    for (String entry : history) {
                        player.sendMessage(mm.deserialize("<white>" + entry));
                    }
                }
            });
            return true;
        }

        player.sendMessage(mm.deserialize("<red>Unknown subcommand. Usage: /feedback <send|history>"));
        return true;
    }

}
