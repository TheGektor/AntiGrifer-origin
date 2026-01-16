package ru.antigrief;

import org.bukkit.plugin.java.JavaPlugin;
import ru.antigrief.managers.ConfigManager;
import ru.antigrief.managers.LocaleManager;
import ru.antigrief.data.DatabaseManager;
import ru.antigrief.handlers.PlayerHandler;
import ru.antigrief.features.alerts.AlertManager;
import ru.antigrief.integrations.DiscordManager;
import ru.antigrief.commands.CommandManager;
import ru.antigrief.features.feedback.FeedbackCommand;
import ru.antigrief.features.feedback.FeedbackManager;
import ru.antigrief.listeners.RestrictionListener;

public class AntiGriefSystem extends JavaPlugin {

    private ConfigManager configManager;
    private LocaleManager localeManager;
    private PlayerHandler playerHandler;
    private DiscordManager discordManager;
    private AlertManager alertManager;
    private FeedbackManager feedbackManager;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        // Load config
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Load locale
        localeManager = new LocaleManager(this);
        localeManager.loadLocale();

        // Connect to DB
        databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("Could not initialize database! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Handlers and Managers
        playerHandler = new PlayerHandler(this);
        discordManager = new DiscordManager(this);
        alertManager = new AlertManager(this);
        feedbackManager = new FeedbackManager(this);

        // Commands
        getCommand("ags").setExecutor(new CommandManager(this));
        getCommand("ags").setTabCompleter((CommandManager) getCommand("ags").getExecutor());

        getCommand("feedback").setExecutor(new FeedbackCommand(this.feedbackManager));

        // Listeners
        getServer().getPluginManager().registerEvents(playerHandler, this);
        getServer().getPluginManager().registerEvents(new RestrictionListener(this), this);

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
