package ru.antigrief;

import org.bukkit.plugin.java.JavaPlugin;

import ru.antigrief.commands.CommandManager;
import ru.antigrief.data.DatabaseManager;
import ru.antigrief.features.alerts.AlertManager;
import ru.antigrief.features.criticallocations.CriticalLocationCommand;
import ru.antigrief.features.criticallocations.CriticalLocationListener;
import ru.antigrief.features.criticallocations.CriticalLocationManager;
import ru.antigrief.features.feedback.FeedbackCommand;
import ru.antigrief.features.feedback.FeedbackManager;
import ru.antigrief.handlers.PlayerHandler;
import ru.antigrief.integrations.DiscordManager;
import ru.antigrief.listeners.RestrictionListener;
import ru.antigrief.managers.ConfigManager;
import ru.antigrief.managers.LocaleManager;

public class AntiGriefSystem extends JavaPlugin {

    private ConfigManager configManager;
    private LocaleManager localeManager;
    private PlayerHandler playerHandler;
    private DiscordManager discordManager;
    private AlertManager alertManager;
    private FeedbackManager feedbackManager;
    private DatabaseManager databaseManager;
    private CriticalLocationManager criticalLocationManager;

    @Override
    public void onEnable() {
        // Load config
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Managers
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfig();

        this.localeManager = new LocaleManager(this);
        this.localeManager.loadLocale();

        this.databaseManager = new DatabaseManager(this);
        
        // Initialize other managers
        this.discordManager = new DiscordManager(this);
        this.alertManager = new AlertManager(this);
        this.feedbackManager = new FeedbackManager(this);
        this.criticalLocationManager = new CriticalLocationManager(this, databaseManager);

        // Connect to DB
        databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("Could not initialize database! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Listeners
        getServer().getPluginManager().registerEvents(playerHandler, this);
        getServer().getPluginManager().registerEvents(new RestrictionListener(this), this);
        getServer().getPluginManager().registerEvents(new CriticalLocationListener(this, criticalLocationManager), this);

        // Handlers are initialized in constructor/fields above, but ensuring consistency
        if (playerHandler == null) playerHandler = new PlayerHandler(this);

        // Commands
        getCommand("ags").setExecutor(new CommandManager(this));
        getCommand("ags").setTabCompleter((CommandManager) getCommand("ags").getExecutor());

        getCommand("feedback").setExecutor(new FeedbackCommand(this.feedbackManager));
        getCommand("critical").setExecutor(new CriticalLocationCommand(this.criticalLocationManager));
        getCommand("critical").setTabCompleter((CriticalLocationCommand) getCommand("critical").getExecutor());

        // Listeners
        getServer().getPluginManager().registerEvents(playerHandler, this);
        getServer().getPluginManager().registerEvents(new RestrictionListener(this), this);

        // Initialize bStats
        int pluginId = 24869; // TODO: Replace with your own plugin ID from bStats.org
        new org.bstats.bukkit.Metrics(this, pluginId);

        getLogger().info("AntiGriefSystem enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        getLogger().info("AntiGriefSystem disabled!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LocaleManager getLocaleManager() {
        return localeManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PlayerHandler getPlayerHandler() {
        return playerHandler;
    }

    public DiscordManager getDiscordManager() {
        return discordManager;
    }

    public AlertManager getAlertManager() {
        return alertManager;
    }

    public FeedbackManager getFeedbackManager() {
        return feedbackManager;
    }
}
