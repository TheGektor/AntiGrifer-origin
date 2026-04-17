package ru.antigrief.core.rules;

import ru.antigrief.api.PlayerContext;
import ru.antigrief.common.action.ActionInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

public class RuleEngine {
    private final List<Rule> rules = new ArrayList<>();

    public void addRule(String id, BiPredicate<PlayerContext, ActionInfo> condition, RuleAction action) {
        rules.add(new Rule(id, condition, action));
    }

    public RuleAction evaluate(PlayerContext context, ActionInfo action) {
        for (Rule rule : rules) {
            if (rule.condition.test(context, action)) {
                return rule.action;
            }
        }
        return RuleAction.ALLOW;
    }

    public enum RuleAction {
        ALLOW,
        CANCEL,
        BLOCK,
        MONITOR
    }

    private record Rule(String id, BiPredicate<PlayerContext, ActionInfo> condition, RuleAction action) {}
}
