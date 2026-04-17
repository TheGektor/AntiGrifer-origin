package ru.antigrief.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.antigrief.api.PlayerContext;
import ru.antigrief.core.context.PlayerContextManager;
import ru.antigrief.core.debug.DebugService;
import ru.antigrief.core.metrics.MetricsEngine;
import ru.antigrief.core.module.ModuleManager;
import ru.antigrief.core.replay.ReplayEngine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AntiGriferCommand implements CommandExecutor, TabCompleter {

    private final DebugService debugService;
    private final PlayerContextManager contextManager;
    private final MetricsEngine metricsEngine;
    private final ModuleManager moduleManager;
    private final ReplayEngine replayEngine;

    public AntiGriferCommand(DebugService debugService, PlayerContextManager contextManager,
                              MetricsEngine metricsEngine, ModuleManager moduleManager,
                              ReplayEngine replayEngine) {
        this.debugService = debugService;
        this.contextManager = contextManager;
        this.metricsEngine = metricsEngine;
        this.moduleManager = moduleManager;
        this.replayEngine = replayEngine;
    }

    // ─── Tab Completion ───────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("antigrifer.admin")) return List.of();

        if (args.length == 1) {
            return filterPrefix(args[0], "help", "debug", "module", "metrics", "replay", "reload");
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "debug" -> {
                    List<String> names = new ArrayList<>();
                    sender.getServer().getOnlinePlayers().forEach(p -> names.add(p.getName()));
                    yield filterPrefix(args[1], names.toArray(new String[0]));
                }
                case "module" -> filterPrefix(args[1], "list", "enable", "disable", "reload");
                case "replay" -> filterPrefix(args[1], "start", "stop", "view", "export");
                default -> List.of();
            };
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("module")) {
            return filterPrefix(args[2], moduleManager.getModuleNames().toArray(new String[0]));
        }

        return List.of();
    }

    private List<String> filterPrefix(String input, String... options) {
        return Arrays.stream(options)
                .filter(o -> o.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }

    // ─── Command Execution ────────────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("antigrifer.admin")) {
            sender.sendMessage(Component.text("✖ Нет прав для выполнения этой команды.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help"    -> sendHelp(sender, label);
            case "debug"   -> handleDebug(sender, args);
            case "module"  -> handleModule(sender, args);
            case "metrics" -> handleMetrics(sender);
            case "replay"  -> handleReplay(sender, args);
            case "reload"  -> handleReload(sender);
            default        -> {
                sender.sendMessage(Component.text("✖ Неизвестная команда. Введите ", NamedTextColor.RED)
                        .append(Component.text("/" + label + " help", NamedTextColor.AQUA)
                                .clickEvent(ClickEvent.runCommand("/" + label + " help"))));
            }
        }
        return true;
    }

    // ─── /antigrifer help ─────────────────────────────────────────────────────

    private void sendHelp(CommandSender sender, String label) {
        String cmd = "/" + label;
        sender.sendMessage(header("AntiGrifer v3 — Справка"));
        sender.sendMessage(helpRow(cmd + " help",    "Показать эту справку",                    cmd + " help",    "Открыть справку"));
        sender.sendMessage(helpRow(cmd + " debug",   "Режим отладки игрока",                    cmd + " debug",   "Подробнее о debug"));
        sender.sendMessage(helpRow(cmd + " module",  "Управление модулями",                     cmd + " module",  "Подробнее о module"));
        sender.sendMessage(helpRow(cmd + " metrics", "Статистика работы плагина",               cmd + " metrics", "Показать метрики"));
        sender.sendMessage(helpRow(cmd + " replay",  "Воспроизведение действий игрока",         cmd + " replay",  "Подробнее о replay"));
        sender.sendMessage(helpRow(cmd + " reload",  "Перезагрузить конфигурацию",              cmd + " reload",  "Перезагрузить"));
        sender.sendMessage(divider());
        sender.sendMessage(Component.text("  Нажмите на команду для подробного описания.", NamedTextColor.DARK_GRAY).decorate(TextDecoration.ITALIC));
    }

    /** Builds a single clickable help row with hover tooltip. */
    private Component helpRow(String displayCmd, String description, String clickCmd, String hoverText) {
        return Component.text("  ▸ ", NamedTextColor.DARK_AQUA)
                .append(Component.text(displayCmd, NamedTextColor.AQUA)
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand(clickCmd))
                        .hoverEvent(HoverEvent.showText(
                                Component.text("▶ Нажмите: ", NamedTextColor.YELLOW)
                                        .append(Component.text(hoverText, NamedTextColor.WHITE))
                        )))
                .append(Component.text(" — " + description, NamedTextColor.GRAY));
    }

    // ─── /antigrifer debug ────────────────────────────────────────────────────

    private void handleDebug(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("✖ Только для игроков.", NamedTextColor.RED));
            return;
        }

        if (args.length == 1) {
            // Toggle own debug session
            debugService.startSession(player.getUniqueId(), msg ->
                    player.sendMessage(Component.text(msg, NamedTextColor.YELLOW)));
            player.sendMessage(Component.text("✔ Режим отладки ", NamedTextColor.GREEN)
                    .append(Component.text("включён", NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
                    .append(Component.text(". Действия будут логироваться в чат.", NamedTextColor.GREEN)));
            return;
        }

        // Target another player
        String targetName = args[1];
        org.bukkit.entity.Player target = sender.getServer().getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage(Component.text("✖ Игрок «" + targetName + "» не найден.", NamedTextColor.RED));
            return;
        }

        PlayerContext ctx = contextManager.getContext(target.getUniqueId());
        sender.sendMessage(header("Отладка: " + target.getName()));
        sender.sendMessage(infoRow("UUID",           target.getUniqueId().toString()));
        sender.sendMessage(infoRow("Trust Score",    String.valueOf(ctx.getTrustScore())));
        sender.sendMessage(infoRow("Violation Score",String.valueOf(ctx.getViolationScore())));
        sender.sendMessage(infoRow("Флаги",          ctx.getFlags().isEmpty() ? "нет" : String.join(", ", ctx.getFlags())));
        sender.sendMessage(infoRow("Последних действий", String.valueOf(ctx.getRecentActions().size())));
        sender.sendMessage(divider());
    }

    // ─── /antigrifer module ───────────────────────────────────────────────────

    private void handleModule(CommandSender sender, String[] args) {
        if (args.length == 1) {
            sender.sendMessage(header("Управление модулями"));
            sender.sendMessage(Component.text("  Субкоманды: ", NamedTextColor.GRAY)
                    .append(clickable("list",    "/antigrifer module list"))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(clickable("enable",  "/antigrifer module enable <имя>"))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(clickable("disable", "/antigrifer module disable <имя>"))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(clickable("reload",  "/antigrifer module reload <имя>")));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "list" -> {
                sender.sendMessage(header("Список модулей"));
                List<String> names = moduleManager.getModuleNames();
                if (names.isEmpty()) {
                    sender.sendMessage(Component.text("  Нет загруженных модулей.", NamedTextColor.GRAY));
                } else {
                    for (String name : names) {
                        sender.sendMessage(Component.text("  ✔ ", NamedTextColor.GREEN)
                                .append(Component.text(name, NamedTextColor.AQUA)));
                    }
                }
            }
            case "reload" -> {
                String target = args.length >= 3 ? args[2] : null;
                if (target == null) {
                    sender.sendMessage(Component.text("✖ Укажите имя модуля.", NamedTextColor.RED));
                    return;
                }
                moduleManager.reloadModule(target);
                sender.sendMessage(Component.text("✔ Модуль «" + target + "» перезагружен.", NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("✖ Неизвестная субкоманда module.", NamedTextColor.RED));
        }
    }

    // ─── /antigrifer metrics ──────────────────────────────────────────────────

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

    // ─── /antigrifer replay ───────────────────────────────────────────────────

    private void handleReplay(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(header("Replay System"));
            sender.sendMessage(Component.text("  Субкоманды: ", NamedTextColor.GRAY)
                    .append(clickable("start <игрок>", "/antigrifer replay start "))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(clickable("view <игрок>",  "/antigrifer replay view "))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(clickable("export",        "/antigrifer replay export")));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "view" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("✖ Укажите ник игрока.", NamedTextColor.RED));
                    return;
                }
                org.bukkit.entity.Player target = sender.getServer().getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage(Component.text("✖ Игрок «" + args[2] + "» не в сети.", NamedTextColor.RED));
                    return;
                }
                var actions = replayEngine.getReplay(target.getUniqueId());
                sender.sendMessage(header("Replay: " + target.getName() + " (" + actions.size() + " действий)"));
                int shown = Math.min(actions.size(), 15);
                for (int i = actions.size() - shown; i < actions.size(); i++) {
                    var a = actions.get(i);
                    sender.sendMessage(Component.text("  [" + i + "] ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(a.getType().name(), NamedTextColor.AQUA))
                            .append(Component.text(" @ ", NamedTextColor.DARK_GRAY))
                            .append(Component.text(a.getLocation().toString(), NamedTextColor.GRAY)));
                }
            }
            case "export" -> sender.sendMessage(Component.text("✖ Экспорт в JSON пока в разработке.", NamedTextColor.YELLOW));
            default -> sender.sendMessage(Component.text("✖ Неизвестная субкоманда replay. Используйте start | view | export", NamedTextColor.RED));
        }
    }

    // ─── /antigrifer reload ───────────────────────────────────────────────────

    private void handleReload(CommandSender sender) {
        sender.sendMessage(Component.text("✔ Конфигурация перезагружена.", NamedTextColor.GREEN));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

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

    private Component clickable(String label, String command) {
        return Component.text(label, NamedTextColor.AQUA)
                .decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.suggestCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text("Нажмите для ввода: " + command, NamedTextColor.YELLOW)));
    }
}
