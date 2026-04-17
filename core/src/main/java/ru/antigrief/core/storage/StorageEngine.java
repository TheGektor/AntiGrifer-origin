package ru.antigrief.core.storage;

import ru.antigrief.api.PlayerContext;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StorageEngine {
    CompletableFuture<Void> savePlayer(PlayerContext context);
    CompletableFuture<PlayerContext> loadPlayer(UUID uuid);
    void close();
}
