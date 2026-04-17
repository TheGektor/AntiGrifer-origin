package ru.antigrief.core.engine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import ru.antigrief.api.PlayerContext;
import ru.antigrief.common.action.ActionInfo;
import ru.antigrief.common.action.ActionType;

public class BehaviorEngine {
    private final Map<ActionType, Integer> scoreRules = new ConcurrentHashMap<>();

    public BehaviorEngine() {
        // Defaults
        scoreRules.put(ActionType.JOIN, 1);
        scoreRules.put(ActionType.BLOCK_BREAK, 0);
    }

    public void defineRule(ActionType type, int points) {
        scoreRules.put(type, points);
    }

    public void processAction(PlayerContext context, ActionInfo action) {
        Integer change = scoreRules.get(action.getType());
        if (change != null && change != 0) {
            context.setTrustScore(context.getTrustScore() + change);
        }
    }
}
