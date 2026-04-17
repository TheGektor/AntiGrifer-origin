package ru.antigrief.modules.detection.checks;

import ru.antigrief.api.Check;
import ru.antigrief.api.CheckResult;
import ru.antigrief.api.PlayerContext;
import ru.antigrief.common.action.ActionInfo;
import ru.antigrief.common.action.ActionType;

public class LavaPlaceCheck implements Check {
    @Override
    public String getName() {
        return "LavaPlaceCheck";
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public CheckResult check(PlayerContext context, ActionInfo action) {
        if (action.getType() == ActionType.BLOCK_PLACE && action.getTargetBlockOrEntity() != null) {
            if (action.getTargetBlockOrEntity().contains("LAVA")) {
                if (context.getTrustScore() < 50) {
                    return CheckResult.fail(50, true, "You are not trusted enough to place lava!");
                }
            }
        }
        return CheckResult.pass();
    }
}
