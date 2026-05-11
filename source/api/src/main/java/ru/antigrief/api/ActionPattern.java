package ru.antigrief.api;

import java.util.List;
import ru.antigrief.common.action.ActionType;

public interface ActionPattern {
    String getName();
    List<ActionType> getSequence();
    void onMatch(PlayerContext context);
}
