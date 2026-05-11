package ru.antigrief.core.storage;

import ru.antigrief.api.PlayerContext;
import ru.antigrief.core.context.PlayerContextImpl;

import java.util.UUID;

/**
 * Заглушка SQLite. Реальное хранилище — YamlPlayerStorage в platform-paper.
 */
public class SQLiteStorage implements StorageEngine {

    @Override
    public void savePlayer(PlayerContext context) {
        // stub — данные не персистируются
    }

    @Override
    public PlayerContext loadPlayer(UUID uuid) {
        return new PlayerContextImpl(uuid);
    }

    @Override
    public void saveAll(Iterable<PlayerContext> contexts) {
        // stub
    }

    @Override
    public void close() { }
}
