package ru.antigrief.paper;

import org.bukkit.plugin.java.JavaPlugin;
import ru.antigrief.core.alert.AlertSystem;
import ru.antigrief.core.config.ConfigManager;
import ru.antigrief.core.context.PlayerContextManager;
import ru.antigrief.core.debug.DebugService;
import ru.antigrief.core.engine.*;
import ru.antigrief.core.metrics.MetricsEngine;
import ru.antigrief.core.module.ModuleManager;
import ru.antigrief.core.pipeline.ActionPipeline;
import ru.antigrief.core.replay.ReplayEngine;
import ru.antigrief.core.rules.RuleEngine;
import ru.antigrief.modules.detection.DetectionModule;
import ru.antigrief.modules.trust.TrustModule;
import ru.antigrief.paper.alert.AdminAlertChannel;
import ru.antigrief.core.trust.TrustConfig;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;
import java.util.Set;

public class AntiGriferPaperPlugin extends JavaPlugin {
    private ModuleManager moduleManager;
    private ActionPipeline pipeline;

    @Override
    public void onEnable() {
        getLogger().info("Starting AntiGrifer v3...");

        // Infrastructure
        saveDefaultConfig();
        ConfigManager configManager = new ConfigManager();
        loadConfigIntoManager(configManager);
        
        MetricsEngine metricsEngine = new MetricsEngine();
        AlertSystem alertSystem = new AlertSystem(getLogger());
        alertSystem.registerChannel(new AdminAlertChannel(configManager));
        
        DebugService debugService = new DebugService();
        ReplayEngine replayEngine = new ReplayEngine();

        // Core Engines
        PlayerContextManager contextManager = new PlayerContextManager();
        DetectionEngine detectionEngine = new DetectionEngine();
        PatternEngine patternEngine = new PatternEngine();
        BehaviorEngine behaviorEngine = new BehaviorEngine();
        SimulationEngine simulationEngine = new SimulationEngine();
        RateLimitEngine rateLimitEngine = new RateLimitEngine();
        RuleEngine ruleEngine = new RuleEngine();

        moduleManager = new ModuleManager(getLogger());
        pipeline = new ActionPipeline(
            contextManager, detectionEngine, patternEngine, behaviorEngine,
            simulationEngine, rateLimitEngine, ruleEngine, alertSystem,
            metricsEngine, debugService, replayEngine
        );

        // Trust
        TrustConfig trustConfig = configManager.getTrustConfig();

        // Default rules/limits
        rateLimitEngine.registerLimit(ru.antigrief.common.action.ActionType.BLOCK_PLACE, 10, 1000);

        // Register bundled modules
        moduleManager.registerModule(new TrustModule(behaviorEngine));
        moduleManager.registerModule(new DetectionModule(detectionEngine, trustConfig, configManager, alertSystem));

        // Enable modules
        moduleManager.enableAll();

        // Register adapter
        getServer().getPluginManager().registerEvents(new PaperAdapter(pipeline), this);

        // Register command with tab completer
        AntiGriferCommand cmd = new AntiGriferCommand(
            debugService, contextManager, metricsEngine, moduleManager, replayEngine
        );
        var pluginCommand = getCommand("antigrifer");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(cmd);
            pluginCommand.setTabCompleter(cmd);
        }

        getLogger().info("AntiGrifer v3 successfully loaded!");
    }

    @Override
    public void onDisable() {
        if (moduleManager != null) {
            moduleManager.disableAll();
        }
        getLogger().info("AntiGrifer v3 disabled.");
    }

    private void loadConfigIntoManager(ConfigManager manager) {
        FileConfiguration config = getConfig();
        Set<String> keys = config.getKeys(true);
        for (String key : keys) {
            if (config.isList(key)) {
                manager.set(key, config.getStringList(key));
            } else if (config.isInt(key)) {
                manager.set(key, config.getInt(key));
            } else if (config.isBoolean(key)) {
                manager.set(key, config.getBoolean(key));
            } else {
                manager.set(key, config.getString(key));
            }
        }
    }
}
