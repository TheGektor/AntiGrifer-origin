package ru.antigrief.core.replay;

import ru.antigrief.common.action.ActionInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReplayEngine {
    private final Map<UUID, List<ActionInfo>> recordings = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> activeSessions = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void startRecording(UUID player) {
        activeSessions.add(player);
        recordings.computeIfAbsent(player, k -> new ArrayList<>());
    }

    public void stopRecording(UUID player) {
        activeSessions.remove(player);
    }

    public boolean isRecording(UUID player) {
        return activeSessions.contains(player);
    }

    public void record(ActionInfo action) {
        if (!activeSessions.contains(action.getPlayer().getUuid())) {
            return;
        }
        // Get-or-create the recording list and add the action once
        List<ActionInfo> history = recordings.computeIfAbsent(action.getPlayer().getUuid(), k -> new ArrayList<>());
        history.add(action);
        // Limit replay size per player
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
