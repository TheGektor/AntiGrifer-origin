package ru.antigrief.modules.detection;

import java.util.Collections;
import java.util.List;
import ru.antigrief.api.AGModule;
import ru.antigrief.core.engine.DetectionEngine;
import ru.antigrief.modules.detection.checks.LavaPlaceCheck;

public class DetectionModule implements AGModule {
    private final DetectionEngine detectionEngine;

    public DetectionModule(DetectionEngine detectionEngine) {
        this.detectionEngine = detectionEngine;
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
    }

    @Override
    public void onDisable() { }
}
