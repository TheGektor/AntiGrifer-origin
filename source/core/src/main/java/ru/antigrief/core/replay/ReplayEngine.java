package ru.antigrief.core.replay;

import ru.antigrief.common.action.ActionInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ReplayEngine — записывает действия ВСЕХ игроков автоматически.
 * Больше не требует ручного /replay start.
 */
public class ReplayEngine {
    private static final int MAX_HISTORY = 1000;

    private final Map<UUID, List<ActionInfo>> recordings = new ConcurrentHashMap<>();

    /** Записывает действие игрока. Вызывается автоматически из ActionPipeline. */
    public void record(ActionInfo action) {
        UUID uuid = action.getPlayer().getUuid();
        List<ActionInfo> history = recordings.computeIfAbsent(uuid, k -> new ArrayList<>());
        history.add(action);
        if (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
    }

    /** Возвращает все записанные действия игрока. */
    public List<ActionInfo> getReplay(UUID player) {
        return recordings.getOrDefault(player, new ArrayList<>());
    }

    /** Возвращает последние N действий игрока. */
    public List<ActionInfo> getLastN(UUID player, int count) {
        List<ActionInfo> all = getReplay(player);
        int from = Math.max(0, all.size() - count);
        return new ArrayList<>(all.subList(from, all.size()));
    }

    /** Очищает историю игрока. */
    public void clear(UUID player) {
        recordings.remove(player);
    }

    // --- Legacy API (сохраняем для совместимости с AntiGriferCommand) ---

    /** @deprecated Запись теперь всегда включена. Оставлено для совместимости. */
    @Deprecated
    public void startRecording(UUID player) { /* no-op */ }

    /** @deprecated Запись теперь всегда включена. Оставлено для совместимости. */
    @Deprecated
    public void stopRecording(UUID player) { /* no-op */ }

    /** @deprecated Всегда возвращает true. */
    @Deprecated
    public boolean isRecording(UUID player) { return true; }
}
