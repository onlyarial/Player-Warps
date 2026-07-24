package com.oncearial.playerwarps;

import com.oncearial.playerwarps.command.PWarpCommand;
import com.oncearial.playerwarps.gui.WarpGui;
import com.oncearial.playerwarps.listener.AliasListener;
import com.oncearial.playerwarps.listener.GuiListener;
import com.oncearial.playerwarps.listener.TeleportListener;
import com.oncearial.playerwarps.storage.WarpStorage;
import com.oncearial.playerwarps.storage.FavoriteStorage;
import com.oncearial.playerwarps.storage.FeaturedStorage;
import com.oncearial.playerwarps.update.UpdateChecker;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class PlayerWarps extends JavaPlugin {
    private WarpStorage storage;
    private WarpGui warpGui;
    private FavoriteStorage favorites;
    private FeaturedStorage featured;
    private TeleportListener teleportListener;
    private UpdateChecker updateChecker;
    private File guiFile;
    private FileConfiguration guiConfig;
    private File messagesFile;
    private FileConfiguration messagesConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("warps.yml", false);
        saveResource("gui.yml", false);
        saveResource("messages.yml", false);
        migrateMessagesConfig();
        reloadGuiConfig();
        reloadMessagesConfig();

        this.favorites = new FavoriteStorage(this);
        this.favorites.load();
        this.featured = new FeaturedStorage(this);
        this.featured.load();

        this.storage = new WarpStorage(this);
        this.storage.load();
        this.warpGui = new WarpGui(this);
        this.teleportListener = new TeleportListener(this);
        this.updateChecker = new UpdateChecker(this);

        PWarpCommand command = new PWarpCommand(this);
        getCommand("pwarp").setExecutor(command);
        getCommand("pwarp").setTabCompleter(command);

        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new AliasListener(this), this);
        getServer().getPluginManager().registerEvents(teleportListener, this);
        getServer().getPluginManager().registerEvents(updateChecker, this);
        updateChecker.checkAsync();
        getLogger().info("PlayerWarps enabled with " + storage.getWarps().size() + " warps.");
    }

    @Override
    public void onDisable() {
        if (storage != null) storage.save();
        if (favorites != null) favorites.save();
        if (featured != null) featured.save();
    }

    public void reloadEverything() {
        reloadConfig();
        reloadGuiConfig();
        reloadMessagesConfig();
        storage.load();
        favorites.load();
        featured.load();
    }

    public void reloadGuiConfig() {
        guiFile = new File(getDataFolder(), "gui.yml");
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);
    }

    public void reloadMessagesConfig() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private void migrateMessagesConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        File messagesFile = new File(getDataFolder(), "messages.yml");
        FileConfiguration messages = YamlConfiguration.loadConfiguration(messagesFile);

        if (config.isConfigurationSection("messages")) {
            for (String key : config.getConfigurationSection("messages").getKeys(false)) {
                messages.set("messages." + key, config.get("messages." + key));
            }
            config.set("messages", null);
            try {
                messages.save(messagesFile);
                config.save(configFile);
                reloadConfig();
                getLogger().info("Moved messages from config.yml to messages.yml.");
            } catch (IOException ex) {
                getLogger().warning("Could not migrate messages.yml: " + ex.getMessage());
            }
        }
    }

    public String message(String key) {
        return messagesConfig.getString("messages." + key, key);
    }

    public FileConfiguration messagesConfig() {
        return messagesConfig;
    }

    public FileConfiguration guiConfig() { return guiConfig; }
    public WarpStorage storage() { return storage; }
    public FavoriteStorage favorites() { return favorites; }
    public FeaturedStorage featured() { return featured; }
    public WarpGui warpGui() { return warpGui; }
    public TeleportListener teleportListener() { return teleportListener; }
}
