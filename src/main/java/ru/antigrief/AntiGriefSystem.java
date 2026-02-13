package ru.antigrief;

import org.bukkit.plugin.java.JavaPlugin;

import ru.antigrief.commands.CommandManager;
import ru.antigrief.data.DatabaseManager;
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

    private static AntiGriefSystem instance;
    private ConfigManager configManager;
    private LocaleManager localeManager;
    private DatabaseManager databaseManager;
    private PlayerHandler playerHandler;
    private DiscordManager discordManager;
    private FeedbackManager feedbackManager;
    private CriticalLocationManager criticalLocationManager;

    @Override
    public void onEnable() {
        instance = this;

        // Managers
        this.configManager = new ConfigManager(this);
        this.localeManager = new LocaleManager(this);
        this.databaseManager = new DatabaseManager(this);
        this.discordManager = new DiscordManager(this);
        this.feedbackManager = new FeedbackManager(this);
        this.criticalLocationManager = new CriticalLocationManager(this, databaseManager);

        // Handlers
        this.playerHandler = new PlayerHandler(this);

        // Listeners
        getServer().getPluginManager().registerEvents(playerHandler, this);
        getServer().getPluginManager().registerEvents(new RestrictionListener(this), this);
        getServer().getPluginManager().registerEvents(new CriticalLocationListener(this, criticalLocationManager), this);

        // Commands
        getCommand("ags").setExecutor(new CommandManager(this));
        getCommand("ags").setTabCompleter((CommandManager) getCommand("ags").getExecutor());

        getCommand("feedback").setExecutor(new FeedbackCommand(this.feedbackManager));
        getCommand("critical").setExecutor(new CriticalLocationCommand(this.criticalLocationManager));
        getCommand("critical").setTabCompleter((CriticalLocationCommand) getCommand("critical").getExecutor());

        getLogger().info("AntiGriefSystem enabled!");
    }

    @Override
    public void onDisable() {
        if (playerHandler != null) {
            playerHandler.saveAll();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("AntiGriefSystem disabled!");
    }

    public static AntiGriefSystem getInstance() {
        return instance;
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

    public FeedbackManager getFeedbackManager() {
        return feedbackManager;
    }
}
