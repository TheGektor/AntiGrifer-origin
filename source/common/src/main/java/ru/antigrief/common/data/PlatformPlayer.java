package ru.antigrief.common.data;

import java.util.UUID;

public class PlatformPlayer {
    private final UUID uuid;
    private final String name;

    public PlatformPlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
}
