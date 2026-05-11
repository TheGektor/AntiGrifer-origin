package ru.antigrief.core.module;

import java.util.ArrayList;
import java.util.List;
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

    /** Hotreload a single module by name. */
    public void reloadModule(String name) {
        AGModule module = modules.get(name);
        if (module == null) {
            logger.warning("Module not found for reload: " + name);
            return;
        }
        try {
            module.onDisable();
            module.onLoad();
            module.onEnable();
            logger.info("Reloaded module: " + name);
        } catch (Exception e) {
            logger.severe("Failed to reload module: " + name);
            e.printStackTrace();
        }
    }

    /** Enables a single module by name. */
    public void enableModule(String name) {
        AGModule module = modules.get(name);
        if (module == null) {
            logger.warning("Module not found for enable: " + name);
            return;
        }
        try {
            module.onEnable();
            logger.info("Enabled module: " + name);
        } catch (Exception e) {
            logger.severe("Failed to enable module: " + name);
            e.printStackTrace();
        }
    }

    /** Disables a single module by name. */
    public void disableModule(String name) {
        AGModule module = modules.get(name);
        if (module == null) {
            logger.warning("Module not found for disable: " + name);
            return;
        }
        try {
            module.onDisable();
            logger.info("Disabled module: " + name);
        } catch (Exception e) {
            logger.severe("Failed to disable module: " + name);
            e.printStackTrace();
        }
    }

    /** Returns names of all registered modules (for tab completion). */
    public List<String> getModuleNames() {
        return new ArrayList<>(modules.keySet());
    }
}
