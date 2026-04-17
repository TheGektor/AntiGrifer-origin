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

public class AntiGriferPaperPlugin extends JavaPlugin {
    private ModuleManager moduleManager;
    private ActionPipeline pipeline;

    @Override
    public void onEnable() {
        getLogger().info("Starting AntiGrifer v3...");

        // Infrastructure
        ConfigManager configManager = new ConfigManager();
        MetricsEngine metricsEngine = new MetricsEngine();
        AlertSystem alertSystem = new AlertSystem(getLogger());
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

        // Default rules/limits
        rateLimitEngine.registerLimit(ru.antigrief.common.action.ActionType.BLOCK_PLACE, 10, 1000); // 10 bps

        // Register bundled modules
        moduleManager.registerModule(new TrustModule(behaviorEngine));
        moduleManager.registerModule(new DetectionModule(detectionEngine));

        // Enable modules
        moduleManager.enableAll();

        // Register adapter
        getServer().getPluginManager().registerEvents(new PaperAdapter(pipeline), this);

        // Register commands
        getCommand("antigrifer").setExecutor(new AntiGriferCommand(debugService, contextManager));

        getLogger().info("AntiGrifer v3 successfully loaded!");
    }

    @Override
    public void onDisable() {
        if (moduleManager != null) {
            moduleManager.disableAll();
        }
        getLogger().info("AntiGrifer v3 disabled.");
    }
}
