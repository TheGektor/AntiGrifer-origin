package ru.antigrief.core.trust;

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

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TrustTier fromPlaytime(long seconds, TrustConfig config) {
        if (seconds >= config.getTier3Minutes() * 60) return TIER_3;
        if (seconds >= config.getTier2Minutes() * 60) return TIER_2;
        if (seconds >= config.getTier1Minutes() * 60) return TIER_1;
        return UNTRUSTED;
    }
}
