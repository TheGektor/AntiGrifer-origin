package ru.antigrief.core.replay;

import ru.antigrief.common.action.ActionInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReplayEngine {
    private final Map<UUID, List<ActionInfo>> recordings = new ConcurrentHashMap<>();

    public void record(ActionInfo action) {
        recordings.computeIfAbsent(action.getPlayer().getUuid(), k -> new ArrayList<>()).add(action);
        // Limit replay size per player
        List<ActionInfo> history = recordings.get(action.getPlayer().getUuid());
        if (history.size() > 1000) {
            history.remove(0);
        }
    }

    public List<ActionInfo> getReplay(UUID player) {
        return recordings.getOrDefault(player, new ArrayList<>());
    }

    public void clear(UUID player) {
        recordings.remove(player);
    }
}
