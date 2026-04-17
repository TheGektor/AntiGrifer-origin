package ru.antigrief.core.module;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import ru.antigrief.api.AGModule;

public class ModuleManager {
    private final Map<String, AGModule> modules = new ConcurrentHashMap<>();
    private final Logger logger;

    public ModuleManager(Logger logger) {
        this.logger = logger;
    }

    public void registerModule(AGModule module) {
        modules.put(module.getName(), module);
        logger.info("Registered module: " + module.getName());
    }

    public void enableAll() {
        for (AGModule module : modules.values()) {
            try {
                module.onLoad();
                module.onEnable();
            } catch (Exception e) {
                logger.severe("Failed to enable module: " + module.getName());
                e.printStackTrace();
            }
        }
    }

    public void disableAll() {
        for (AGModule module : modules.values()) {
            try {
                module.onDisable();
            } catch (Exception e) {
                logger.severe("Failed to disable module: " + module.getName());
            }
        }
    }
}
