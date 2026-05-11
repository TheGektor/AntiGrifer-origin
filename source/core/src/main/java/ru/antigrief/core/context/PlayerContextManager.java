package ru.antigrief.core.context;

import ru.antigrief.api.PlayerContext;
import ru.antigrief.core.storage.StorageEngine;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerContextManager {
    private final Map<UUID, PlayerContext> contexts = new ConcurrentHashMap<>();
    private final StorageEngine storage;

    public PlayerContextManager(StorageEngine storage) {
        this.storage = storage;
    }

    /**
     * Возвращает контекст из памяти. Создаёт новый если не найден (fallback).
     * Предпочтительно использовать loadPlayer() на JOIN, чтобы данные были загружены заранее.
     */
    public PlayerContext getContext(UUID uuid) {
        return contexts.computeIfAbsent(uuid, id -> storage.loadPlayer(id));
    }

    /**
     * Принудительно загружает данные из хранилища и кладёт в кэш.
     * Вызывать при PlayerJoinEvent.
     */
    public PlayerContext loadPlayer(UUID uuid) {
        PlayerContext ctx = storage.loadPlayer(uuid);
        contexts.put(uuid, ctx);
        return ctx;
    }

    /**
     * Сохраняет контекст игрока в хранилище.
     * Вызывать при PlayerQuitEvent.
     */
    public void savePlayer(UUID uuid) {
        PlayerContext ctx = contexts.get(uuid);
        if (ctx != null) storage.savePlayer(ctx);
    }

    /**
     * Сохраняет всех игроков. Вызывать при onDisable.
     */
    public void saveAll() {
        storage.saveAll(contexts.values());
        contexts.clear();
    }

    public void removeContext(UUID uuid) {
        contexts.remove(uuid);
    }

    public Collection<PlayerContext> getAllContexts() {
        return contexts.values();
    }
}
