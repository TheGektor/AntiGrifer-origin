package ru.antigrief.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.antigrief.api.PlayerContext;
import ru.antigrief.core.alert.AlertTracker;
import ru.antigrief.core.config.ConfigManager;
import ru.antigrief.core.context.PlayerContextManager;
import ru.antigrief.core.debug.DebugService;
import ru.antigrief.core.metrics.MetricsEngine;
import ru.antigrief.core.module.ModuleManager;
import ru.antigrief.core.replay.ReplayEngine;
import ru.antigrief.core.trust.TrustConfig;
import ru.antigrief.core.trust.TrustTier;
import ru.antigrief.paper.discord.DiscordAlertChannel;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class AntiGriferCommand implements CommandExecutor, TabCompleter {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final DebugService debugService;
    private final PlayerContextManager contextManager;
    private final MetricsEngine metricsEngine;
    private final ModuleManager moduleManager;
    private final ReplayEngine replayEngine;
    private final AlertTracker alertTracker;
    private final TrustConfig trustConfig;
    private final ConfigManager configManager;
    /** Может быть null если Discord не настроен. */
    private DiscordAlertChannel discordChannel;

    public AntiGriferCommand(DebugService debugService, PlayerContextManager contextManager,
                              MetricsEngine metricsEngine, ModuleManager moduleManager,
                              ReplayEngine replayEngine, AlertTracker alertTracker,
                              TrustConfig trustConfig, ConfigManager configManager) {
        this.debugService = debugService;
        this.contextManager = contextManager;
        this.metricsEngine = metricsEngine;
        this.moduleManager = moduleManager;
        this.replayEngine = replayEngine;
        this.alertTracker = alertTracker;
        this.trustConfig = trustConfig;
        this.configManager = configManager;
    }

    /** Устанавливает Discord-канал (вызывается из AntiGriferPaperPlugin если Discord настроен). */
    public void setDiscordChannel(DiscordAlertChannel discordChannel) {
        this.discordChannel = discordChannel;
    }

    // ── Tab completion ────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("antigrifer.admin")) return List.of();

        if (args.length == 1) {
            return filterPrefix(args[0], "help", "debug", "module", "metrics",
                    "replay", "reload", "trust", "alerts", "playtime", "ack", "tp");
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "debug", "trust", "playtime", "ack", "tp" -> onlinePlayers(args[1]);
                case "module" -> filterPrefix(args[1], "list", "enable", "disable", "reload");
                case "replay" -> filterPrefix(args[1], "view", "export");
                default -> List.of();
            };
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("trust")) {
            return filterPrefix(args[2], "0", "1", "2", "3", "auto");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("module")) {
            return filterPrefix(args[2], moduleManager.getModuleNames().toArray(new String[0]));
        }
        return List.of();
    }

    private List<String> onlinePlayers(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> filterPrefix(String input, String... options) {
        return Arrays.stream(options)
                .filter(o -> o.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }

    // ── Command router ────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("antigrifer.admin")) {
            sender.sendMessage(Component.text("✖ Нет прав.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) { sendHelp(sender, label); return true; }

        switch (args[0].toLowerCase()) {
            case "help"     -> sendHelp(sender, label);
            case "debug"    -> handleDebug(sender, args);
            case "module"   -> handleModule(sender, args);
            case "metrics"  -> handleMetrics(sender);
            case "replay"   -> handleReplay(sender, args);
            case "reload"   -> handleReload(sender);
            case "trust"    -> handleTrust(sender, args);
            case "alerts"   -> handleAlerts(sender);
            case "playtime" -> handlePlaytime(sender, args);
            case "ack"      -> handleAck(sender, args);
            case "tp"       -> handleTp(sender, args);
            default -> sender.sendMessage(Component.text("✖ Неизвестная команда. /" + label + " help", NamedTextColor.RED));
        }
        return true;
    }

    // ── /antigrifer help ──────────────────────────────────────────────────────

    private void sendHelp(CommandSender sender, String label) {
        String c = "/" + label;
        sender.sendMessage(header("AntiGrifer v3 — Справка"));
        sender.sendMessage(helpRow(c + " debug",    "Отладка игрока",              c + " debug"));
        sender.sendMessage(helpRow(c + " trust",    "Управление уровнем доверия",  c + " trust"));
        sender.sendMessage(helpRow(c + " alerts",   "Список активных алертов",     c + " alerts"));
        sender.sendMessage(helpRow(c + " playtime", "Плейтайм и Tier игрока",      c + " playtime"));
        sender.sendMessage(helpRow(c + " module",   "Управление модулями",         c + " module"));
        sender.sendMessage(helpRow(c + " metrics",  "Статистика плагина",          c + " metrics"));
        sender.sendMessage(helpRow(c + " replay",   "Последние действия игрока",   c + " replay"));
        sender.sendMessage(helpRow(c + " reload",   "Перезагрузить конфиг",        c + " reload"));
        sender.sendMessage(divider());
    }

    private Component helpRow(String displayCmd, String desc, String clickCmd) {
        return Component.text("  ▸ ", NamedTextColor.DARK_AQUA)
                .append(Component.text(displayCmd, NamedTextColor.AQUA).decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.suggestCommand(clickCmd)))
                .append(Component.text(" — " + desc, NamedTextColor.GRAY));
    }

    // ── /antigrifer trust <player> [0|1|2|3|auto] ────────────────────────────

    private void handleTrust(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text(
                    "Использование: /antigrifer trust <игрок> [0|1|2|3|auto]", NamedTextColor.YELLOW));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("✖ Игрок «" + args[1] + "» не в сети.", NamedTextColor.RED));
            return;
        }
        PlayerContext ctx = contextManager.getContext(target.getUniqueId());

        if (args.length == 2) {
            // Показать текущий статус
            TrustTier tier = TrustTier.resolve(ctx, trustConfig);
            sender.sendMessage(header("Доверие: " + target.getName()));
            sender.sendMessage(infoRow("Tier",      tier.getDisplayName()));
            sender.sendMessage(infoRow("Плейтайм", formatPlaytime(ctx.getPlaytimeSeconds())));
            sender.sendMessage(infoRow("Ручной Tier",
                    ctx.getManualTier() != null ? String.valueOf(ctx.getManualTier()) : "авто"));
            sender.sendMessage(divider());
            return;
        }

        String tierArg = args[2].toLowerCase();
        if (tierArg.equals("auto")) {
            ctx.setManualTier(null);
            sender.sendMessage(Component.text("✔ Tier для «" + target.getName() + "» сброшен в авто.", NamedTextColor.GREEN));
        } else {
            try {
                int level = Integer.parseInt(tierArg);
                if (level < 0 || level > 3) throw new NumberFormatException();
                ctx.setManualTier(level);
                TrustTier tier = TrustTier.fromLevel(level);
                contextManager.savePlayer(target.getUniqueId());
                sender.sendMessage(Component.text("✔ Tier для «" + target.getName() + "» установлен: "
                        + tier.getDisplayName(), NamedTextColor.GREEN));
                target.sendMessage(Component.text("§6[AntiGrifer] §eВаш уровень доверия изменён администратором: "
                        + tier.getDisplayName()));
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("✖ Укажите 0, 1, 2, 3 или auto.", NamedTextColor.RED));
            }
        }
    }

    // ── /antigrifer alerts ────────────────────────────────────────────────────

    private void handleAlerts(CommandSender sender) {
        Map<UUID, Long> pending = alertTracker.getPendingAlerts();
        sender.sendMessage(header("Активные алерты (" + pending.size() + ")"));
        if (pending.isEmpty()) {
            sender.sendMessage(Component.text("  ✔ Нет необработанных алертов.", NamedTextColor.GREEN));
            return;
        }
        pending.forEach((uuid, ts) -> {
            Player p = Bukkit.getPlayer(uuid);
            String name = p != null ? p.getName() : uuid.toString().substring(0, 8) + "...";
            String time = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault())
                    .format(DT_FMT);
            String reason = alertTracker.getReason(uuid);

            Component row = Component.text("  [" + time + "] ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(name, NamedTextColor.RED))
                    .append(Component.text(" — " + reason, NamedTextColor.GRAY))
                    .append(Component.text(" [TP]", NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.runCommand("/antigrifer tp " + name))
                            .hoverEvent(HoverEvent.showText(Component.text("Телепортироваться к " + name))))
                    .append(Component.text(" [ACK]", NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.runCommand("/antigrifer ack " + name))
                            .hoverEvent(HoverEvent.showText(Component.text("Отметить как обработанный"))));
            sender.sendMessage(row);
        });
        sender.sendMessage(divider());
    }

    // ── /antigrifer playtime <player> ─────────────────────────────────────────

    private void handlePlaytime(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Использование: /antigrifer playtime <игрок>", NamedTextColor.YELLOW));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("✖ Игрок «" + args[1] + "» не в сети.", NamedTextColor.RED));
            return;
        }
        PlayerContext ctx = contextManager.getContext(target.getUniqueId());
        TrustTier tier = TrustTier.resolve(ctx, trustConfig);

        sender.sendMessage(header("Плейтайм: " + target.getName()));
        sender.sendMessage(infoRow("Плейтайм", formatPlaytime(ctx.getPlaytimeSeconds())));
        sender.sendMessage(infoRow("Tier",      tier.getDisplayName()));
        sender.sendMessage(infoRow("До TIER 1", formatRemaining(ctx, trustConfig, 1)));
        sender.sendMessage(infoRow("До TIER 2", formatRemaining(ctx, trustConfig, 2)));
        sender.sendMessage(infoRow("До TIER 3", formatRemaining(ctx, trustConfig, 3)));
        sender.sendMessage(divider());
    }

    // ── /antigrifer ack <player> ──────────────────────────────────────────────

    private void handleAck(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Использование: /antigrifer ack <игрок>", NamedTextColor.YELLOW));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("✖ Игрок «" + args[1] + "» не в сети.", NamedTextColor.RED));
            return;
        }
        alertTracker.acknowledge(target.getUniqueId());
        // Уведомляем Discord
        if (discordChannel != null) {
            discordChannel.notifyAction(target.getUniqueId(), target.getName(),
                    sender.getName(), "Алерт принят", true);
        }
        sender.sendMessage(Component.text("✔ Алерт для «" + target.getName() + "» закрыт.", NamedTextColor.GREEN));
    }

    // ── /antigrifer tp <player> ───────────────────────────────────────────────

    private void handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player admin)) {
            sender.sendMessage(Component.text("✖ Только для игроков.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Использование: /antigrifer tp <игрок>", NamedTextColor.YELLOW));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("✖ Игрок «" + args[1] + "» не в сети.", NamedTextColor.RED));
            return;
        }
        admin.teleport(target);
        alertTracker.acknowledge(target.getUniqueId());
        // Уведомляем Discord
        if (discordChannel != null) {
            discordChannel.notifyAction(target.getUniqueId(), target.getName(),
                    admin.getName(), "Телепортировался к игроку", false);
        }
        sender.sendMessage(Component.text("✔ Телепорт к «" + target.getName() + "». Алерт закрыт.", NamedTextColor.GREEN));
    }

    // ── /antigrifer debug ─────────────────────────────────────────────────────

    private void handleDebug(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("✖ Только для игроков.", NamedTextColor.RED));
            return;
        }
        if (args.length == 1) {
            debugService.startSession(player.getUniqueId(),
                    msg -> player.sendMessage(Component.text(msg, NamedTextColor.YELLOW)));
            player.sendMessage(Component.text("✔ Режим отладки включён.", NamedTextColor.GREEN));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("✖ Игрок «" + args[1] + "» не найден.", NamedTextColor.RED));
            return;
        }
        PlayerContext ctx = contextManager.getContext(target.getUniqueId());
        TrustTier tier = TrustTier.resolve(ctx, trustConfig);
        sender.sendMessage(header("Отладка: " + target.getName()));
        sender.sendMessage(infoRow("UUID",           target.getUniqueId().toString()));
        sender.sendMessage(infoRow("Tier",           tier.getDisplayName()));
        sender.sendMessage(infoRow("Плейтайм",       formatPlaytime(ctx.getPlaytimeSeconds())));
        sender.sendMessage(infoRow("Violation",      String.valueOf(ctx.getViolationScore())));
        sender.sendMessage(infoRow("Ручной Tier",    ctx.getManualTier() != null ? String.valueOf(ctx.getManualTier()) : "авто"));
        sender.sendMessage(infoRow("Флаги",          ctx.getFlags().isEmpty() ? "нет" : String.join(", ", ctx.getFlags())));
        sender.sendMessage(divider());
    }

    // ── /antigrifer module ────────────────────────────────────────────────────

    private void handleModule(CommandSender sender, String[] args) {
        if (args.length == 1) {
            sender.sendMessage(header("Модули"));
            sender.sendMessage(Component.text("  Субкоманды: ", NamedTextColor.GRAY)
                    .append(clickable("list", "/antigrifer module list"))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(clickable("enable <имя>", "/antigrifer module enable "))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(clickable("disable <имя>", "/antigrifer module disable "))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(clickable("reload <имя>", "/antigrifer module reload ")));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "list" -> {
                sender.sendMessage(header("Список модулей"));
                for (String name : moduleManager.getModuleNames()) {
                    sender.sendMessage(Component.text("  ✔ ", NamedTextColor.GREEN)
                            .append(Component.text(name, NamedTextColor.AQUA)));
                }
            }
            case "enable" -> {
                if (args.length < 3) { sender.sendMessage(Component.text("✖ Укажите имя модуля.", NamedTextColor.RED)); return; }
                moduleManager.enableModule(args[2]);
                sender.sendMessage(Component.text("✔ Модуль «" + args[2] + "» включён.", NamedTextColor.GREEN));
            }
            case "disable" -> {
                if (args.length < 3) { sender.sendMessage(Component.text("✖ Укажите имя модуля.", NamedTextColor.RED)); return; }
                moduleManager.disableModule(args[2]);
                sender.sendMessage(Component.text("✔ Модуль «" + args[2] + "» отключён.", NamedTextColor.YELLOW));
            }
            case "reload" -> {
                if (args.length < 3) { sender.sendMessage(Component.text("✖ Укажите имя модуля.", NamedTextColor.RED)); return; }
                moduleManager.reloadModule(args[2]);
                sender.sendMessage(Component.text("✔ Модуль «" + args[2] + "» перезагружен.", NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("✖ Неизвестная субкоманда.", NamedTextColor.RED));
        }
    }

    // ── /antigrifer metrics ───────────────────────────────────────────────────

    private void handleMetrics(CommandSender sender) {
        sender.sendMessage(header("Метрики AntiGrifer"));
        Map<String, Long> all = metricsEngine.getAllMetrics();
        if (all.isEmpty()) {
            sender.sendMessage(Component.text("  Нет данных.", NamedTextColor.GRAY));
            return;
        }
        all.forEach((k, v) -> sender.sendMessage(infoRow(k, String.valueOf(v))));
        sender.sendMessage(divider());
    }

    // ── /antigrifer replay ────────────────────────────────────────────────────

    private void handleReplay(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(header("Replay"));
            sender.sendMessage(Component.text("  Субкоманды: ", NamedTextColor.GRAY)
                    .append(clickable("view <игрок>", "/antigrifer replay view "))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(clickable("export <игрок>", "/antigrifer replay export ")));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "start", "stop" -> sender.sendMessage(
                    Component.text("ℹ Запись ведётся автоматически для всех игроков.", NamedTextColor.YELLOW));
            case "view" -> {
                if (args.length < 3) { sender.sendMessage(Component.text("✖ Укажите ник.", NamedTextColor.RED)); return; }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) { sender.sendMessage(Component.text("✖ Игрок не в сети.", NamedTextColor.RED)); return; }
                var actions = replayEngine.getLastN(target.getUniqueId(), 15);
                sender.sendMessage(header("Replay: " + target.getName() + " (последние " + actions.size() + ")"));
                for (int i = 0; i < actions.size(); i++) {
                    var a = actions.get(i);
                    sender.sendMessage(Component.text("  [" + i + "] ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(a.getType().name(), NamedTextColor.AQUA))
                            .append(Component.text(" @ ", NamedTextColor.DARK_GRAY))
                            .append(Component.text(a.getLocation().toString(), NamedTextColor.GRAY)));
                }
                sender.sendMessage(divider());
            }
            case "export" -> {
                if (args.length < 3) { sender.sendMessage(Component.text("✖ Укажите ник.", NamedTextColor.RED)); return; }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) { sender.sendMessage(Component.text("✖ Игрок не в сети.", NamedTextColor.RED)); return; }
                var actions = replayEngine.getReplay(target.getUniqueId());
                getLogger().info("REPLAY EXPORT [" + target.getName() + "]: " + actions);
                sender.sendMessage(Component.text("✔ Экспортировано " + actions.size() + " действий в лог сервера.", NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("✖ Неизвестная субкоманда replay.", NamedTextColor.RED));
        }
    }

    private java.util.logging.Logger getLogger() {
        return java.util.logging.Logger.getLogger("AntiGrifer");
    }

    // ── /antigrifer reload ────────────────────────────────────────────────────

    private void handleReload(CommandSender sender) {
        sender.sendMessage(Component.text(
                "✔ Конфигурация перезагружена. (Перезапустите плагин для полного перезапуска)", NamedTextColor.GREEN));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String formatPlaytime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        return h + "ч " + m + "м";
    }

    private String formatRemaining(PlayerContext ctx, TrustConfig cfg, int targetTier) {
        TrustTier current = TrustTier.resolve(ctx, cfg);
        if (current.getLevel() >= targetTier) return "✔ Достигнуто";
        long required = switch (targetTier) {
            case 1 -> cfg.getTier1Minutes() * 60L;
            case 2 -> cfg.getTier2Minutes() * 60L;
            case 3 -> cfg.getTier3Minutes() * 60L;
            default -> -1;
        };
        if (required < 0) return "—";
        long remaining = required - ctx.getPlaytimeSeconds();
        return formatPlaytime(Math.max(0, remaining));
    }

    private Component header(String title) {
        return Component.text("━━━ ", NamedTextColor.DARK_AQUA)
                .append(Component.text(title, NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
                .append(Component.text(" ━━━", NamedTextColor.DARK_AQUA));
    }

    private Component divider() {
        return Component.text("  ──────────────────────────", NamedTextColor.DARK_GRAY);
    }

    private Component infoRow(String key, String value) {
        return Component.text("  " + key + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, NamedTextColor.WHITE));
    }

    private Component clickable(String label, String cmd) {
        return Component.text(label, NamedTextColor.AQUA).decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.suggestCommand(cmd))
                .hoverEvent(HoverEvent.showText(Component.text("Нажмите: " + cmd, NamedTextColor.YELLOW)));
    }
}
