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

    @Override
    public void onEnable() {
        saveDefaultConfig();
        migrateConfig();
        saveResource("warps.yml", false);
        saveResource("gui.yml", false);
        reloadGuiConfig();

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
        storage.load();
        favorites.load();
        featured.load();
    }

    public void reloadGuiConfig() {
        guiFile = new File(getDataFolder(), "gui.yml");
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);
    }

    private void migrateConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        boolean changed = false;

        changed |= setDefault(config, "messages.usage-seticon", "&cUsage: /pwarp seticon <name>");
        changed |= setDefault(config, "messages.icon-updated", "&aUpdated icon for &e%warp%&a.");

        if (!changed) return;

        try {
            config.save(configFile);
            reloadConfig();
            getLogger().info("Added missing config options to config.yml.");
        } catch (IOException ex) {
            getLogger().warning("Could not save migrated config.yml: " + ex.getMessage());
        }
    }

    private boolean setDefault(FileConfiguration config, String path, Object value) {
        if (config.contains(path)) return false;
        config.set(path, value);
        return true;
    }

    public FileConfiguration guiConfig() { return guiConfig; }
    public WarpStorage storage() { return storage; }
    public FavoriteStorage favorites() { return favorites; }
    public FeaturedStorage featured() { return featured; }
    public WarpGui warpGui() { return warpGui; }
    public TeleportListener teleportListener() { return teleportListener; }
}
