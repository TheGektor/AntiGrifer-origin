package ru.antigrief.api;

import java.util.UUID;

public interface AntiGriferAPI {
    boolean isTrusted(UUID uuid);
    int getViolationScore(UUID uuid);
    void registerCheck(Check check);
    void registerPattern(ActionPattern pattern);
    void registerModule(AGModule module);
}
