package ru.antigrief.api;

import java.util.UUID;
import java.util.List;
import ru.antigrief.common.action.ActionInfo;

public interface PlayerContext {
    UUID getUniqueId();
    long getPlaytimeSeconds();
    void addPlaytime(long seconds);
    long getFirstSeenTimestamp();
    void setFirstSeenTimestamp(long timestamp);
    int getTrustScore();
    void setTrustScore(int score);
    int getViolationScore();
    void setViolationScore(int score);
    List<String> getFlags();
    void addFlag(String flag);
    void removeFlag(String flag);
    List<ActionInfo> getRecentActions();

    /**
     * Возвращает вручную установленный уровень доверия (0–3).
     * null — уровень определяется автоматически по плейтайму.
     */
    Integer getManualTier();
    void setManualTier(Integer tier);
}
