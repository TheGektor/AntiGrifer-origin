package ru.antigrief.core.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import ru.antigrief.api.Check;
import ru.antigrief.api.CheckResult;
import ru.antigrief.api.PlayerContext;
import ru.antigrief.common.action.ActionInfo;

public class DetectionEngine {
    private final List<Check> checks = new ArrayList<>();

    public void registerCheck(Check check) {
        checks.add(check);
        checks.sort(Comparator.comparingInt(Check::getPriority).reversed());
    }

    public CheckResult runChecks(PlayerContext context, ActionInfo action) {
        CheckResult worstResult = CheckResult.pass();

        for (Check check : checks) {
            CheckResult result = check.check(context, action);
            if (result.isCancelAction()) {
                // Instantly return the first cancellation result because it's high priority
                return result;
            }
            if (result.getViolationScore() > worstResult.getViolationScore()) {
                worstResult = result;
            }
        }
        return worstResult;
    }
}
