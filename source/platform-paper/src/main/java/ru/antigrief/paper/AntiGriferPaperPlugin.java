package ru.antigrief.paper;

import org.bukkit.plugin.java.JavaPlugin;
import ru.antigrief.core.alert.AlertSystem;
import ru.antigrief.core.alert.AlertTracker;
import ru.antigrief.core.config.ConfigManager;
import ru.antigrief.core.context.PlayerContextManager;
import ru.antigrief.core.debug.DebugService;
import ru.antigrief.core.engine.*;
import ru.antigrief.core.metrics.MetricsEngine;
import ru.antigrief.core.module.ModuleManager;
import ru.antigrief.core.pipeline.ActionPipeline;
import ru.antigrief.core.replay.ReplayEngine;
import ru.antigrief.core.rules.RuleEngine;
import ru.antigrief.core.trust.TrustConfig;
import ru.antigrief.modules.detection.DetectionModule;
import ru.antigrief.modules.trust.TrustModule;
import ru.antigrief.paper.alert.AdminAlertChannel;
import ru.antigrief.paper.discord.DiscordAlertChannel;
import ru.antigrief.paper.discord.DiscordWebhookClient;
import ru.antigrief.paper.listener.RedstoneActivationListener;
import ru.antigrief.paper.storage.YamlPlayerStorage;
import ru.antigrief.paper.task.PlaytimeTracker;
import ru.antigrief.paper.tracking.BlockPlaceTracker;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.Set;

public class AntiGriferPaperPlugin extends JavaPlugin {
    private ModuleManager moduleManager;
    private PlayerContextManager contextManager;
    private DiscordWebhookClient discordClient;

    @Override
    public void onEnable() {
        getLogger().info("Starting AntiGrifer v3...");

        // ── Config ─────────────────────────────────────────────────────────────
        saveDefaultConfig();
        ConfigManager configManager = new ConfigManager();
        loadConfigIntoManager(configManager);

        // ── Storage ────────────────────────────────────────────────────────────
        YamlPlayerStorage storage = new YamlPlayerStorage(getDataFolder(), getLogger());
        contextManager = new PlayerContextManager(storage);

        // ── Alert infrastructure ───────────────────────────────────────────────
        AlertTracker alertTracker = new AlertTracker();
        AlertSystem alertSystem = new AlertSystem(getLogger());
        alertSystem.registerChannel(new AdminAlertChannel(configManager, alertTracker));

        // ── Core engines ───────────────────────────────────────────────────────
        MetricsEngine metricsEngine     = new MetricsEngine();
        DebugService debugService       = new DebugService();
        ReplayEngine replayEngine       = new ReplayEngine();

        DetectionEngine detectionEngine = new DetectionEngine();
        PatternEngine patternEngine     = new PatternEngine();
        BehaviorEngine behaviorEngine   = new BehaviorEngine();
        SimulationEngine simEngine      = new SimulationEngine();
        RateLimitEngine rateLimitEngine = new RateLimitEngine();
        RuleEngine ruleEngine           = new RuleEngine();

        // ── Pipeline ───────────────────────────────────────────────────────────
        moduleManager = new ModuleManager(getLogger());
        ActionPipeline pipeline = new ActionPipeline(
                contextManager, detectionEngine, patternEngine, behaviorEngine,
                simEngine, rateLimitEngine, ruleEngine, alertSystem,
                metricsEngine, debugService, replayEngine);

        // ── Trust ──────────────────────────────────────────────────────────────
        TrustConfig trustConfig = configManager.getTrustConfig();
        rateLimitEngine.registerLimit(ru.antigrief.common.action.ActionType.BLOCK_PLACE, 10, 1000);

        // ── Modules ────────────────────────────────────────────────────────────
        moduleManager.registerModule(new TrustModule(behaviorEngine));
        moduleManager.registerModule(new DetectionModule(detectionEngine, trustConfig, configManager, alertSystem));
        moduleManager.enableAll();

        // ── Block tracking ─────────────────────────────────────────────────────
        BlockPlaceTracker blockPlaceTracker = new BlockPlaceTracker();

        // ── Event listeners ────────────────────────────────────────────────────
        getServer().getPluginManager().registerEvents(
                new PaperAdapter(pipeline, contextManager), this);
        getServer().getPluginManager().registerEvents(
                new RedstoneActivationListener(blockPlaceTracker, contextManager,
                        trustConfig, alertSystem, alertTracker), this);

        // ── Playtime tracker (каждые 60 сек) ──────────────────────────────────
        PlaytimeTracker.start(this, contextManager);

        // ── Commands ───────────────────────────────────────────────────────────
        AntiGriferCommand cmd = new AntiGriferCommand(
                debugService, contextManager, metricsEngine, moduleManager,
                replayEngine, alertTracker, trustConfig, configManager);
        var pluginCmd = getCommand("antigrifer");
        if (pluginCmd != null) {
            pluginCmd.setExecutor(cmd);
            pluginCmd.setTabCompleter(cmd);
        }

        // ── Discord (optional) ────────────────────────────────────────────────
        String webhookUrl = configManager.getString("discord.webhook_url", "");
        boolean discordEnabled = configManager.getBoolean("discord.enabled", false)
                && !webhookUrl.isBlank();
        if (discordEnabled) {
            boolean forumChannel = configManager.getBoolean("discord.forum_channel", true);
            discordClient = new DiscordWebhookClient(webhookUrl, getLogger());
            DiscordAlertChannel discordAlertChannel =
                    new DiscordAlertChannel(discordClient, alertTracker, forumChannel);
            alertSystem.registerChannel(discordAlertChannel);
            cmd.setDiscordChannel(discordAlertChannel);
            getLogger().info("[AntiGrifer] Discord включён. Forum channel: " + forumChannel);
        } else {
            getLogger().info("[AntiGrifer] Discord отключён (discord.enabled=false или webhook_url пустой).");
        }

        getLogger().info("AntiGrifer v3 loaded! Storage: YAML | Replay: always-on");
    }

    @Override
    public void onDisable() {
        if (moduleManager != null) moduleManager.disableAll();
        if (contextManager != null) contextManager.saveAll();
        if (discordClient  != null) discordClient.shutdown();
        getLogger().info("AntiGrifer v3 disabled. All player data saved.");
    }

    private void loadConfigIntoManager(ConfigManager manager) {
        FileConfiguration config = getConfig();
        Set<String> keys = config.getKeys(true);
        for (String key : keys) {
            if (config.isList(key))         manager.set(key, config.getStringList(key));
            else if (config.isInt(key))     manager.set(key, config.getInt(key));
            else if (config.isBoolean(key)) manager.set(key, config.getBoolean(key));
            else                            manager.set(key, config.getString(key));
        }
    }
}
