package ru.antigrief.data;

import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private long playtime; // in milliseconds
    private boolean trusted;

    public PlayerData(UUID uuid, long playtime, boolean trusted) {
        this.uuid = uuid;
        this.playtime = playtime;
        this.trusted = trusted;
    }

    public UUID getUuid() {
        return uuid;
    }

    public long getPlaytime() {
        return playtime;
    }

    public void setPlaytime(long playtime) {
        this.playtime = playtime;
    }

    public void addPlaytime(long millis) {
        this.playtime += millis;
    }

    public boolean isTrusted() {
        return trusted;
    }

    public void setTrusted(boolean trusted) {
        this.trusted = trusted;
    }
}
