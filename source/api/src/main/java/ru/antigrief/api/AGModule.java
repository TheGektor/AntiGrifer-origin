package ru.antigrief.api;

import java.util.List;

public interface AGModule {
    String getName();
    List<String> getDependencies();
    void onLoad();
    void onEnable();
    void onDisable();
}
