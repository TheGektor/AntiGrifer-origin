package ru.antigrief.common.action;

import java.util.Map;
import ru.antigrief.common.data.PlatformLocation;
import ru.antigrief.common.data.PlatformPlayer;

public class ActionInfo {
    private final PlatformPlayer player;
    private final ActionType type;
    private final PlatformLocation location;
    private final String targetBlockOrEntity;
    private final Map<String, Object> metadata;
    private final long timestamp;

    public ActionInfo(PlatformPlayer player, ActionType type, PlatformLocation location, String targetBlockOrEntity, Map<String, Object> metadata) {
        this.player = player;
        this.type = type;
        this.location = location;
        this.targetBlockOrEntity = targetBlockOrEntity;
        this.metadata = metadata;
        this.timestamp = System.currentTimeMillis();
    }

    public PlatformPlayer getPlayer() { return player; }
    public ActionType getType() { return type; }
    public PlatformLocation getLocation() { return location; }
    public String getTargetBlockOrEntity() { return targetBlockOrEntity; }
    public Map<String, Object> getMetadata() { return metadata; }
    public long getTimestamp() { return timestamp; }
}
