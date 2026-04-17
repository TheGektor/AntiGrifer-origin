package ru.antigrief.core.engine;

import ru.antigrief.common.action.ActionType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitEngine {
    private final Map<ActionType, RateLimitConfig> limits = new HashMap<>();
    private final Map<UUID, Map<ActionType, ActionTracker>> trackers = new ConcurrentHashMap<>();

    public void registerLimit(ActionType type, int max, long windowMillis) {
        limits.put(type, new RateLimitConfig(max, windowMillis));
    }

    public boolean isRateLimited(UUID playerUuid, ActionType type) {
        RateLimitConfig config = limits.get(type);
        if (config == null) return false;

        Map<ActionType, ActionTracker> playerTrackers = trackers.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>());
        ActionTracker tracker = playerTrackers.computeIfAbsent(type, k -> new ActionTracker());

        long now = System.currentTimeMillis();
        tracker.cleanup(now, config.windowMillis);
        
        if (tracker.getCount() >= config.max) {
            return true;
        }

        tracker.add(now);
        return false;
    }

    private record RateLimitConfig(int max, long windowMillis) {}

    private static class ActionTracker {
        private final java.util.Deque<Long> timestamps = new java.util.ArrayDeque<>();

        public synchronized void add(long timestamp) {
            timestamps.addLast(timestamp);
        }

        public synchronized void cleanup(long now, long window) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < now - window) {
                timestamps.removeFirst();
            }
        }

        public synchronized int getCount() {
            return timestamps.size();
        }
    }
}
