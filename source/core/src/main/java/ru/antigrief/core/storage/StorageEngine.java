package ru.antigrief.core.storage;

import ru.antigrief.api.PlayerContext;

import java.util.UUID;

public interface StorageEngine {
    /** Синхронно сохраняет данные игрока. */
    void savePlayer(PlayerContext context);

    /** Синхронно загружает данные игрока. Возвращает новый контекст если не найден. */
    PlayerContext loadPlayer(UUID uuid);

    /** Сохраняет всех игроков (вызывается при onDisable). */
    void saveAll(Iterable<PlayerContext> contexts);

    void close();
}
