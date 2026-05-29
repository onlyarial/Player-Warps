package com.oncearial.playerwarps.storage;

import com.oncearial.playerwarps.PlayerWarps;
import com.oncearial.playerwarps.model.PlayerWarp;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WarpStorage {
    private final PlayerWarps plugin;
    private final File file;
    private FileConfiguration yaml;
    private final Map<String, PlayerWarp> warps = new TreeMap<>();

    public WarpStorage(PlayerWarps plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "warps.yml");
    }

    public void load() {
        warps.clear();
        yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("warps");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            try {
                String path = "warps." + key + ".";
                UUID owner = UUID.fromString(yaml.getString(path + "owner-uuid"));
                String ownerName = yaml.getString(path + "owner-name", "Unknown");
                String world = yaml.getString(path + "world");
                double x = yaml.getDouble(path + "x");
                double y = yaml.getDouble(path + "y");
                double z = yaml.getDouble(path + "z");
                float yaw = (float) yaml.getDouble(path + "yaw");
                float pitch = (float) yaml.getDouble(path + "pitch");
                Material icon = Material.matchMaterial(yaml.getString(path + "icon", plugin.getConfig().getString("settings.default-icon", "ENDER_PEARL")));
                if (icon == null) icon = Material.ENDER_PEARL;
                long createdAt = yaml.getLong(path + "created-at", System.currentTimeMillis());
                warps.put(key.toLowerCase(), new PlayerWarp(key, owner, ownerName, world, x, y, z, yaw, pitch, icon, createdAt));
            } catch (Exception ex) {
                plugin.getLogger().warning("Could not load warp " + key + ": " + ex.getMessage());
            }
        }
    }

    public void save() {
        yaml.set("warps", null);
        for (PlayerWarp warp : warps.values()) {
            String path = "warps." + warp.name() + ".";
            yaml.set(path + "owner-uuid", warp.ownerUuid().toString());
            yaml.set(path + "owner-name", warp.ownerName());
            yaml.set(path + "world", warp.worldName());
            yaml.set(path + "x", warp.x());
            yaml.set(path + "y", warp.y());
            yaml.set(path + "z", warp.z());
            yaml.set(path + "yaw", warp.yaw());
            yaml.set(path + "pitch", warp.pitch());
            yaml.set(path + "icon", warp.icon().name());
            yaml.set(path + "created-at", warp.createdAt());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save warps.yml: " + e.getMessage());
        }
    }

    public Collection<PlayerWarp> getWarps() { return Collections.unmodifiableCollection(warps.values()); }
    public Optional<PlayerWarp> getWarp(String name) { return Optional.ofNullable(warps.get(name.toLowerCase())); }
    public boolean exists(String name) { return warps.containsKey(name.toLowerCase()); }

    public void addWarp(PlayerWarp warp) { warps.put(warp.name(), warp); save(); }
    public void removeWarp(String name) { warps.remove(name.toLowerCase()); save(); }
    public void replaceWarp(String oldName, PlayerWarp warp) { warps.remove(oldName.toLowerCase()); warps.put(warp.name(), warp); save(); }

    public int countOwned(UUID owner) {
        int count = 0;
        for (PlayerWarp warp : warps.values()) if (warp.ownerUuid().equals(owner)) count++;
        return count;
    }

    public List<PlayerWarp> ownedBy(UUID owner) {
        List<PlayerWarp> owned = new ArrayList<>();
        for (PlayerWarp warp : warps.values()) if (warp.ownerUuid().equals(owner)) owned.add(warp);
        return owned;
    }
}
