package com.oncearial.playerwarps.storage;

import com.oncearial.playerwarps.PlayerWarps;
import com.oncearial.playerwarps.util.WarpNames;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FeaturedStorage {
    private final PlayerWarps plugin;
    private final File file;
    private FileConfiguration yaml;
    private final List<String> featured = new ArrayList<>();

    public FeaturedStorage(PlayerWarps plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "featured.yml");
    }

    public void load() {
        yaml = YamlConfiguration.loadConfiguration(file);
        featured.clear();
        for (String name : yaml.getStringList("featured")) {
            if (!name.isBlank()) featured.add(WarpNames.normalize(name));
        }
    }

    public void save() {
        if (yaml == null) yaml = new YamlConfiguration();
        yaml.set("featured", featured);
        try { yaml.save(file); } catch (IOException e) { plugin.getLogger().severe("Could not save featured.yml: " + e.getMessage()); }
    }

    public List<String> getFeatured() { return Collections.unmodifiableList(featured); }

    public boolean add(String warpName, int maxSlots) {
        String normalized = WarpNames.normalize(warpName);
        if (featured.contains(normalized)) return false;
        if (featured.size() >= maxSlots) featured.remove(0);
        featured.add(normalized);
        save();
        return true;
    }

    public void removeWarp(String warpName) {
        if (featured.remove(WarpNames.normalize(warpName))) save();
    }

    public void renameWarp(String oldName, String newName) {
        String oldNormalized = WarpNames.normalize(oldName);
        String newNormalized = WarpNames.normalize(newName);
        boolean changed = false;
        for (int i = 0; i < featured.size(); i++) {
            if (featured.get(i).equals(oldNormalized)) { featured.set(i, newNormalized); changed = true; }
        }
        if (changed) save();
    }
}
