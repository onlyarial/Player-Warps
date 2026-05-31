package com.oncearial.playerwarps;

import com.oncearial.playerwarps.command.PWarpCommand;
import com.oncearial.playerwarps.gui.WarpGui;
import com.oncearial.playerwarps.listener.AliasListener;
import com.oncearial.playerwarps.listener.GuiListener;
import com.oncearial.playerwarps.listener.TeleportListener;
import com.oncearial.playerwarps.storage.WarpStorage;
import com.oncearial.playerwarps.storage.FavoriteStorage;
import com.oncearial.playerwarps.storage.FeaturedStorage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class PlayerWarps extends JavaPlugin {
    private WarpStorage storage;
    private WarpGui warpGui;
    private FavoriteStorage favorites;
    private FeaturedStorage featured;
    private TeleportListener teleportListener;
    private File guiFile;
    private FileConfiguration guiConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
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

        PWarpCommand command = new PWarpCommand(this);
        getCommand("pwarp").setExecutor(command);
        getCommand("pwarp").setTabCompleter(command);

        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new AliasListener(this), this);
        getServer().getPluginManager().registerEvents(teleportListener, this);
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

    public FileConfiguration guiConfig() { return guiConfig; }
    public WarpStorage storage() { return storage; }
    public FavoriteStorage favorites() { return favorites; }
    public FeaturedStorage featured() { return featured; }
    public WarpGui warpGui() { return warpGui; }
    public TeleportListener teleportListener() { return teleportListener; }
}
