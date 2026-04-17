package ru.antigrief.api;

import ru.antigrief.common.action.ActionInfo;

public interface Check {
    String getName();
    int getPriority();
    CheckResult check(PlayerContext context, ActionInfo action);
}
