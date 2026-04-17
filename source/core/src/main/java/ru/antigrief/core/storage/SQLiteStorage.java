package ru.antigrief.core.storage;

import ru.antigrief.api.PlayerContext;
import ru.antigrief.core.context.PlayerContextImpl;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLiteStorage implements StorageEngine {
    // Simple mock implementation as actual JDBC setup takes time and requires external lib in core
    // In a real scenario, this would use HikariCP and SQLite JDBC.
    
    @Override
    public CompletableFuture<Void> savePlayer(PlayerContext context) {
        return CompletableFuture.runAsync(() -> {
            // Simulated save logic
        });
    }

    @Override
    public CompletableFuture<PlayerContext> loadPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            // Simulated load logic
            return new PlayerContextImpl(uuid);
        });
    }

    @Override
    public void close() {
        // Close connection pool
    }
}
