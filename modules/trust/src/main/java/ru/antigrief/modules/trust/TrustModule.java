package ru.antigrief.modules.trust;

import java.util.Collections;
import java.util.List;
import ru.antigrief.api.AGModule;
import ru.antigrief.common.action.ActionType;
import ru.antigrief.core.engine.BehaviorEngine;

public class TrustModule implements AGModule {
    private final BehaviorEngine behaviorEngine;

    public TrustModule(BehaviorEngine behaviorEngine) {
        this.behaviorEngine = behaviorEngine;
    }

    @Override
    public String getName() {
        return "TrustModule";
    }

    @Override
    public List<String> getDependencies() {
        return Collections.emptyList();
    }

    @Override
    public void onLoad() {
        // Init logic
    }

    @Override
    public void onEnable() {
        behaviorEngine.defineRule(ActionType.JOIN, 5);
        behaviorEngine.defineRule(ActionType.BLOCK_BREAK, 1);
        behaviorEngine.defineRule(ActionType.BLOCK_PLACE, 1);
    }

    @Override
    public void onDisable() {
        // Cleanup if necessary
    }
}
