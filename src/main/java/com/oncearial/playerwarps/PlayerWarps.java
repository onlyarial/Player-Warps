package com.oncearial.playerwarps;

import com.oncearial.playerwarps.command.PWarpCommand;
import com.oncearial.playerwarps.gui.WarpGui;
import com.oncearial.playerwarps.listener.GuiListener;
import com.oncearial.playerwarps.listener.TeleportListener;
import com.oncearial.playerwarps.storage.WarpStorage;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerWarps extends JavaPlugin {
    private WarpStorage storage;
    private WarpGui warpGui;
    private TeleportListener teleportListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("warps.yml", false);
        this.storage = new WarpStorage(this);
        this.storage.load();
        this.warpGui = new WarpGui(this);
        this.teleportListener = new TeleportListener(this);

        PWarpCommand command = new PWarpCommand(this);
        getCommand("pwarp").setExecutor(command);
        getCommand("pwarp").setTabCompleter(command);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(teleportListener, this);
        getLogger().info("PlayerWarps enabled with " + storage.getWarps().size() + " warps.");
    }

    @Override
    public void onDisable() {
        if (storage != null) storage.save();
    }

    public void reloadEverything() {
        reloadConfig();
        storage.load();
    }

    public WarpStorage storage() { return storage; }
    public WarpGui warpGui() { return warpGui; }
    public TeleportListener teleportListener() { return teleportListener; }
}
