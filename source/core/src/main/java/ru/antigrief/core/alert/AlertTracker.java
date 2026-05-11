package ru.antigrief.core.alert;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Трекер необработанных алертов.
 * Администраторы получают повторные напоминания пока не "обработают" гриф
 * (телепортируются к подозреваемому или вызовут acknowledge вручную).
 */
public class AlertTracker {
    /** UUID игрока → timestamp первого алерта в мс */
    private final Map<UUID, Long> pending = new ConcurrentHashMap<>();
    /** UUID игрока → причина последнего алерта */
    private final Map<UUID, String> reasons = new ConcurrentHashMap<>();
    /** UUID игрока → Discord thread/channel ID (-1 если нет) */
    private final Map<UUID, Long> discordThreadIds = new ConcurrentHashMap<>();

    /**
     * Регистрирует новый алерт. Если уже есть — обновляет причину, не сбрасывает время.
     */
    public void report(UUID suspect, String reason) {
        pending.putIfAbsent(suspect, System.currentTimeMillis());
        reasons.put(suspect, reason);
    }

    /**
     * Помечает алерт как обработанный (вызывается когда адм телепортируется к игроку).
     */
    public void acknowledge(UUID suspect) {
        pending.remove(suspect);
        reasons.remove(suspect);
    }

    /** Возвращает true если есть необработанный алерт для этого игрока. */
    public boolean isPending(UUID suspect) {
        return pending.containsKey(suspect);
    }

    /** Возвращает время первого алерта или -1 если нет. */
    public long getAlertTime(UUID suspect) {
        return pending.getOrDefault(suspect, -1L);
    }

    /** Возвращает причину последнего алерта. */
    public String getReason(UUID suspect) {
        return reasons.getOrDefault(suspect, "Неизвестно");
    }

    /** Все текущие необработанные алерты. */
    public Map<UUID, Long> getPendingAlerts() {
        return Map.copyOf(pending);
    }

    /** Удаляет игроков у которых алерт висит дольше maxAgeMs миллисекунд. */
    public void cleanup(long maxAgeMs) {
        long now = System.currentTimeMillis();
        pending.entrySet().removeIf(e -> (now - e.getValue()) > maxAgeMs);
    }

    // ── Discord thread tracking ────────────────────────────────────────────────

    /** Сохраняет ID Discord треда для подозреваемого (-1 = нет треда). */
    public void setDiscordThreadId(UUID suspect, long threadId) {
        if (threadId <= 0) discordThreadIds.remove(suspect);
        else discordThreadIds.put(suspect, threadId);
    }

    /** Возвращает ID Discord треда или -1 если нет. */
    public long getDiscordThreadId(UUID suspect) {
        return discordThreadIds.getOrDefault(suspect, -1L);
    }
}
