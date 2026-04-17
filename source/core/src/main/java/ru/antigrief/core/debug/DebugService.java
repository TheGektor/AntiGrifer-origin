package ru.antigrief.core.debug;

import ru.antigrief.api.PlayerContext;

import java.util.UUID;
import java.util.function.Consumer;

public class DebugService {
    private final java.util.Map<UUID, Consumer<String>> debugSessions = new java.util.concurrent.ConcurrentHashMap<>();

    public void startSession(UUID admin, Consumer<String> reporter) {
        debugSessions.put(admin, reporter);
    }

    public void stopSession(UUID admin) {
        debugSessions.remove(admin);
    }

    public void log(String message) {
        debugSessions.values().forEach(reporter -> reporter.accept("§7[AG-DEBUG] " + message));
    }

    public void reportPlayer(UUID admin, PlayerContext target) {
        Consumer<String> reporter = debugSessions.get(admin);
        if (reporter == null) return;

        reporter.accept("§b--- Debug Report for " + target.getUniqueId() + " ---");
        reporter.accept("§fTrust Score: §a" + target.getTrustScore());
        reporter.accept("§fViolation Score: §c" + target.getViolationScore());
        reporter.accept("§fRecent Actions: §7" + target.getRecentActions().size());
        reporter.accept("§b------------------------------------");
    }
}
