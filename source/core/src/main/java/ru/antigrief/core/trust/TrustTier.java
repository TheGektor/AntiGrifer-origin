package ru.antigrief.core.trust;

import ru.antigrief.api.PlayerContext;

public enum TrustTier {
    UNTRUSTED(0, "Недоверенный"),
    TIER_1(1, "Уровень 1 (Огнеопасно)"),
    TIER_2(2, "Уровень 2 (Взрывоопасно)"),
    TIER_3(3, "Уровень 3 (Редстоун)");

    private final int level;
    private final String displayName;

    TrustTier(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }

    public int getLevel() { return level; }
    public String getDisplayName() { return displayName; }

    /** Определяет Tier только по плейтайму (без учёта ручного переопределения). */
    public static TrustTier fromPlaytime(long seconds, TrustConfig config) {
        if (seconds >= config.getTier3Minutes() * 60L) return TIER_3;
        if (seconds >= config.getTier2Minutes() * 60L) return TIER_2;
        if (seconds >= config.getTier1Minutes() * 60L) return TIER_1;
        return UNTRUSTED;
    }

    /**
     * Основной метод: сначала проверяет ручной override от администратора,
     * затем считает по плейтайму. Используй во всех чеках.
     */
    public static TrustTier resolve(PlayerContext context, TrustConfig config) {
        Integer manual = context.getManualTier();
        if (manual != null) {
            for (TrustTier t : values()) {
                if (t.level == manual) return t;
            }
        }
        return fromPlaytime(context.getPlaytimeSeconds(), config);
    }

    public static TrustTier fromLevel(int level) {
        for (TrustTier t : values()) {
            if (t.level == level) return t;
        }
        return UNTRUSTED;
    }
}

