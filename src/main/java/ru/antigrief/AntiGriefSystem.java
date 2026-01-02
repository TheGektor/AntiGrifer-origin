package ru.antigrief;

import org.bukkit.plugin.java.JavaPlugin;
import ru.antigrief.managers.ConfigManager;
import ru.antigrief.managers.LocaleManager;
import ru.antigrief.data.DatabaseManager;
import ru.antigrief.handlers.PlayerHandler;
import ru.antigrief.listeners.RestrictionListener;
import ru.antigrief.integrations.DiscordManager;
import ru.antigrief.commands.CommandManager;

public class AntiGriefSystem extends JavaPlugin {

    private static AntiGriefSystem instance;
    private ConfigManager configManager;
    private LocaleManager localeManager;
    private DatabaseManager databaseManager;
    private PlayerHandler playerHandler;
    private DiscordManager discordManager;

    @Override
    public void onEnable() {
        instance = this;

        // Managers
        this.configManager = new ConfigManager(this);
        this.localeManager = new LocaleManager(this);
        this.databaseManager = new DatabaseManager(this); // Will implement next
        this.discordManager = new DiscordManager(this);

        // Handlers
        this.playerHandler = new PlayerHandler(this);

        // Listeners
        getServer().getPluginManager().registerEvents(playerHandler, this);
        getServer().getPluginManager().registerEvents(new RestrictionListener(this), this);

        // Commands
        getCommand("ags").setExecutor(new CommandManager(this));

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
}
