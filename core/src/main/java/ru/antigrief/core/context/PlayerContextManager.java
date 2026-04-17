package ru.antigrief.core.context;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import ru.antigrief.api.PlayerContext;

public class PlayerContextManager {
    private final Map<UUID, PlayerContext> contexts = new ConcurrentHashMap<>();

    public PlayerContext getContext(UUID uuid) {
        return contexts.computeIfAbsent(uuid, PlayerContextImpl::new);
    }

    public void removeContext(UUID uuid) {
        contexts.remove(uuid);
    }
}
