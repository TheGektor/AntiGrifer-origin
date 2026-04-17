package ru.antigrief.modules.detection;

import java.util.Collections;
import java.util.List;
import ru.antigrief.api.AGModule;
import ru.antigrief.core.alert.AlertSystem;
import ru.antigrief.core.config.ConfigManager;
import ru.antigrief.core.engine.DetectionEngine;
import ru.antigrief.core.trust.TrustConfig;
import ru.antigrief.modules.detection.checks.ExplosiveCheck;
import ru.antigrief.modules.detection.checks.FireUseCheck;
import ru.antigrief.modules.detection.checks.LavaPlaceCheck;
import ru.antigrief.modules.detection.checks.RedstoneGriefCheck;

public class DetectionModule implements AGModule {
    private final DetectionEngine detectionEngine;
    private final TrustConfig trustConfig;
    private final ConfigManager configManager;
    private final AlertSystem alertSystem;

    public DetectionModule(DetectionEngine detectionEngine, TrustConfig trustConfig, ConfigManager configManager, AlertSystem alertSystem) {
        this.detectionEngine = detectionEngine;
        this.trustConfig = trustConfig;
        this.configManager = configManager;
        this.alertSystem = alertSystem;
    }

    @Override
    public String getName() {
        return "DetectionModule";
    }

    @Override
    public List<String> getDependencies() {
        return Collections.emptyList();
    }

    @Override
    public void onLoad() { }

    @Override
    public void onEnable() {
        detectionEngine.registerCheck(new LavaPlaceCheck());
        detectionEngine.registerCheck(new FireUseCheck(trustConfig, configManager, alertSystem));
        detectionEngine.registerCheck(new ExplosiveCheck(trustConfig, configManager, alertSystem));
        detectionEngine.registerCheck(new RedstoneGriefCheck(trustConfig, configManager, alertSystem));
    }

    @Override
    public void onDisable() { }
}
