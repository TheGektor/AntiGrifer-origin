package ru.antigrief;

import org.bukkit.plugin.java.JavaPlugin;

import ru.antigrief.core.database.DatabaseManager;
import ru.antigrief.core.localization.LanguageManager;
import ru.antigrief.core.version.VersionManager;
import ru.antigrief.features.alerts.AlertListener;
import ru.antigrief.features.alerts.AlertManager;
import ru.antigrief.features.playtime.PlaytimeListener;
import ru.antigrief.features.playtime.PlaytimeManager;
import ru.antigrief.features.update_system.UpdateCommand;
import ru.antigrief.features.update_system.UpdateManager;

/**
 * Главный класс плагина AntiGriefSystem.
 *
 * @author Antag0nis1
 */
public class AntiGriefSystem extends JavaPlugin {

    private DatabaseManager databaseManager;
    private LanguageManager languageManager;
    private VersionManager versionManager;
    private AlertManager alertManager;
    private PlaytimeManager playtimeManager;
    private UpdateManager updateManager;

    @Override
    public void onEnable() {
        // 1. Сохранение дефолтного конфига
        saveDefaultConfig();

        // 2. Инициализация ядра
        this.languageManager = new LanguageManager(this);
        this.languageManager.loadLanguage(getConfig().getString("language", "ru"));
        
        this.databaseManager = new DatabaseManager(this);
        
        this.versionManager = new VersionManager(this);
        this.updateManager = new UpdateManager(this, versionManager);
        this.updateManager.startupCheck();

        // 3. Инициализация фич
        this.alertManager = new AlertManager(this);
        getServer().getPluginManager().registerEvents(new AlertListener(alertManager), this);

        this.playtimeManager = new PlaytimeManager(this, databaseManager);
        getServer().getPluginManager().registerEvents(new PlaytimeListener(playtimeManager), this);

        // 4. Регистрация команд
        if (getCommand("ags") != null) {
            getCommand("ags").setExecutor(new UpdateCommand(updateManager));
        }

        getLogger().info("AntiGriefSystem enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("AntiGriefSystem disabled.");
    }
    
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public LanguageManager getLanguageManager() { return languageManager; }
}
