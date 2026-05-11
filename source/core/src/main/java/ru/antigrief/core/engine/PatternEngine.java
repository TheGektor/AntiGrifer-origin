package ru.antigrief.core.engine;

import java.util.ArrayList;
import java.util.List;
import ru.antigrief.api.ActionPattern;
import ru.antigrief.api.PlayerContext;
import ru.antigrief.common.action.ActionInfo;

public class PatternEngine {
    private final List<ActionPattern> patterns = new ArrayList<>();

    public void registerPattern(ActionPattern pattern) {
        patterns.add(pattern);
    }

    public void feedAction(PlayerContext context, ActionInfo currentAction) {
        for (ActionPattern pattern : patterns) {
            if (matches(context, currentAction, pattern)) {
                pattern.onMatch(context);
            }
        }
    }

    private boolean matches(PlayerContext context, ActionInfo currentAction, ActionPattern pattern) {
        List<ActionInfo> recent = context.getRecentActions();
        List<ru.antigrief.common.action.ActionType> sequence = pattern.getSequence();
        
        if (sequence.isEmpty()) return false;
        
        // Sequence last element must match currentAction
        if (currentAction.getType() != sequence.get(sequence.size() - 1)) {
            return false;
        }
        
        if (sequence.size() == 1) return true;
        
        // Check remaining sequence in history
        if (recent.size() < sequence.size() - 1) return false;
        
        int historyStartOffset = recent.size() - (sequence.size() - 1);
        for (int i = 0; i < sequence.size() - 1; i++) {
            if (recent.get(historyStartOffset + i).getType() != sequence.get(i)) {
                return false;
            }
        }
        
        return true;
    }
}
