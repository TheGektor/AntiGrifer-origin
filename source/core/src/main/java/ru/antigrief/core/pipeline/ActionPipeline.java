package ru.antigrief.core.pipeline;

import ru.antigrief.api.CheckResult;
import ru.antigrief.api.PlayerContext;
import ru.antigrief.common.action.ActionInfo;
import ru.antigrief.core.alert.AlertSystem;
import ru.antigrief.core.context.PlayerContextManager;
import ru.antigrief.core.debug.DebugService;
import ru.antigrief.core.engine.*;
import ru.antigrief.core.metrics.MetricsEngine;
import ru.antigrief.core.replay.ReplayEngine;
import ru.antigrief.core.rules.RuleEngine;

public class ActionPipeline {
    private final PlayerContextManager contextManager;
    private final DetectionEngine detectionEngine;
    private final PatternEngine patternEngine;
    private final BehaviorEngine behaviorEngine;
    private final SimulationEngine simulationEngine;
    private final RateLimitEngine rateLimitEngine;
    private final RuleEngine ruleEngine;
    private final AlertSystem alertSystem;
    private final MetricsEngine metricsEngine;
    private final DebugService debugService;
    private final ReplayEngine replayEngine;

    public ActionPipeline(PlayerContextManager contextManager, DetectionEngine detectionEngine, 
                          PatternEngine patternEngine, BehaviorEngine behaviorEngine, 
                          SimulationEngine simulationEngine, RateLimitEngine rateLimitEngine, 
                          RuleEngine ruleEngine, AlertSystem alertSystem, 
                          MetricsEngine metricsEngine, DebugService debugService, 
                          ReplayEngine replayEngine) {
        this.contextManager = contextManager;
        this.detectionEngine = detectionEngine;
        this.patternEngine = patternEngine;
        this.behaviorEngine = behaviorEngine;
        this.simulationEngine = simulationEngine;
        this.rateLimitEngine = rateLimitEngine;
        this.ruleEngine = ruleEngine;
        this.alertSystem = alertSystem;
        this.metricsEngine = metricsEngine;
        this.debugService = debugService;
        this.replayEngine = replayEngine;
    }

    public CheckResult process(ActionInfo action) {
        metricsEngine.increment("events_processed");
        PlayerContext context = contextManager.getContext(action.getPlayer().getUuid());

        // 1. Rate Limiting
        if (rateLimitEngine.isRateLimited(action.getPlayer().getUuid(), action.getType())) {
            metricsEngine.increment("rate_limited");
            return CheckResult.fail(0, true, "Slow down! You are performing actions too fast.");
        }

        // 2. Rule Evaluation
        RuleEngine.RuleAction ruleResult = ruleEngine.evaluate(context, action);
        if (ruleResult == RuleEngine.RuleAction.CANCEL || ruleResult == RuleEngine.RuleAction.BLOCK) {
            metricsEngine.increment("rules_triggered");
            return CheckResult.fail(0, true, "Action blocked by server rules.");
        }

        // 3. Behavior Scoring
        behaviorEngine.processAction(context, action);

        // 4. Pattern Detection
        patternEngine.feedAction(context, action);

        // 5. Simulation
        SimulationEngine.SimulationResult simResult = simulationEngine.simulate(action);
        if (simResult.dangerous()) {
            debugService.log("Dangerous action simulated for " + action.getPlayer().getName() + ": risk=" + simResult.playerDamageRisk());
        }

        // 6. Detection Engine
        CheckResult result = detectionEngine.runChecks(context, action);
        
        if (result.getViolationScore() > 0) {
            metricsEngine.increment("violations");
            context.setViolationScore(context.getViolationScore() + result.getViolationScore());
            if (context.getViolationScore() > 100) {
                alertSystem.dispatch("Player " + action.getPlayer().getName() + " reached high violation score!", AlertSystem.AlertLevel.HIGH);
            }
        }

        // 7. Recording (Replay)
        replayEngine.record(action);

        // Feed history
        context.getRecentActions().add(action);
        if (context.getRecentActions().size() > 50) {
            context.getRecentActions().remove(0);
        }

        return result;
    }
}
