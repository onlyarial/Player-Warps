package com.oncearial.playerwarps.storage;

import com.oncearial.playerwarps.PlayerWarps;
import com.oncearial.playerwarps.util.WarpNames;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FavoriteStorage {
    private final PlayerWarps plugin;
    private final File file;
    private FileConfiguration yaml;
    private final Map<UUID, LinkedHashSet<String>> favorites = new HashMap<>();

    public FavoriteStorage(PlayerWarps plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "favorites.yml");
    }

    public void load() {
        favorites.clear();
        yaml = YamlConfiguration.loadConfiguration(file);
        if (!yaml.isConfigurationSection("favorites")) return;
        for (String uuidText : yaml.getConfigurationSection("favorites").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidText);
                LinkedHashSet<String> names = new LinkedHashSet<>();
                for (String name : yaml.getStringList("favorites." + uuidText)) names.add(WarpNames.normalize(name));
                favorites.put(uuid, names);
            } catch (Exception ex) {
                plugin.getLogger().warning("Could not load favorites for " + uuidText + ": " + ex.getMessage());
            }
        }
    }

    public void save() {
        if (yaml == null) yaml = new YamlConfiguration();
        yaml.set("favorites", null);
        for (Map.Entry<UUID, LinkedHashSet<String>> entry : favorites.entrySet()) {
            yaml.set("favorites." + entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        try { yaml.save(file); } catch (IOException e) { plugin.getLogger().severe("Could not save favorites.yml: " + e.getMessage()); }
    }

    public boolean isFavorite(UUID player, String warpName) {
        return favorites.getOrDefault(player, new LinkedHashSet<>()).contains(WarpNames.normalize(warpName));
    }

    public boolean toggle(UUID player, String warpName) {
        LinkedHashSet<String> set = favorites.computeIfAbsent(player, k -> new LinkedHashSet<>());
        String normalized = WarpNames.normalize(warpName);
        boolean nowFavorite;
        if (set.contains(normalized)) { set.remove(normalized); nowFavorite = false; }
        else { set.add(normalized); nowFavorite = true; }
        save();
        return nowFavorite;
    }

    public List<String> favorites(UUID player) {
        return new ArrayList<>(favorites.getOrDefault(player, new LinkedHashSet<>()));
    }

    public void removeWarp(String warpName) {
        String normalized = WarpNames.normalize(warpName);
        boolean changed = false;
        for (LinkedHashSet<String> set : favorites.values()) changed |= set.remove(normalized);
        if (changed) save();
    }

    public void renameWarp(String oldName, String newName) {
        String oldNormalized = WarpNames.normalize(oldName);
        String newNormalized = WarpNames.normalize(newName);
        boolean changed = false;
        for (LinkedHashSet<String> set : favorites.values()) {
            if (set.remove(oldNormalized)) { set.add(newNormalized); changed = true; }
        }
        if (changed) save();
    }
}
