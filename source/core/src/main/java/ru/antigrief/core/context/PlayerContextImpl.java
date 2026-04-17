package ru.antigrief.core.context;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import ru.antigrief.api.PlayerContext;
import ru.antigrief.common.action.ActionInfo;

public class PlayerContextImpl implements PlayerContext {
    private final UUID uuid;
    private long playtimeSeconds;
    private int trustScore;
    private int violationScore;
    private final List<String> flags = new ArrayList<>();
    private final List<ActionInfo> recentActions = new ArrayList<>();
    private long firstSeenTimestamp;

    public PlayerContextImpl(UUID uuid) {
        this.uuid = uuid;
        this.firstSeenTimestamp = System.currentTimeMillis();
    }

    @Override public UUID getUniqueId() { return uuid; }
    @Override public long getPlaytimeSeconds() { return playtimeSeconds; }
    @Override public void addPlaytime(long seconds) { this.playtimeSeconds += seconds; }
    
    @Override public long getFirstSeenTimestamp() { return firstSeenTimestamp; }
    @Override public void setFirstSeenTimestamp(long timestamp) { this.firstSeenTimestamp = timestamp; }
    
    @Override public int getTrustScore() { return trustScore; }
    @Override public void setTrustScore(int score) { this.trustScore = score; }
    
    @Override public int getViolationScore() { return violationScore; }
    @Override public void setViolationScore(int score) { this.violationScore = score; }
    
    @Override public List<String> getFlags() { return flags; }
    @Override public void addFlag(String flag) { if (!flags.contains(flag)) flags.add(flag); }
    @Override public void removeFlag(String flag) { flags.remove(flag); }
    
    @Override public List<ActionInfo> getRecentActions() { return recentActions; }
}
