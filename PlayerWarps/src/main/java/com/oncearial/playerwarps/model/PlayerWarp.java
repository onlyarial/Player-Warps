package com.oncearial.playerwarps.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.UUID;

public class PlayerWarp {
    private final String name;
    private final UUID ownerUuid;
    private final String ownerName;
    private final String worldName;
    private final double x, y, z;
    private final float yaw, pitch;
    private final Material icon;
    private final long createdAt;

    public PlayerWarp(String name, UUID ownerUuid, String ownerName, Location location, Material icon, long createdAt) {
        this.name = name;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.icon = icon;
        this.createdAt = createdAt;
    }

    public PlayerWarp(String name, UUID ownerUuid, String ownerName, String worldName, double x, double y, double z, float yaw, float pitch, Material icon, long createdAt) {
        this.name = name;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.worldName = worldName;
        this.x = x; this.y = y; this.z = z; this.yaw = yaw; this.pitch = pitch;
        this.icon = icon;
        this.createdAt = createdAt;
    }

    public String name() { return name; }
    public UUID ownerUuid() { return ownerUuid; }
    public String ownerName() { return ownerName; }
    public String worldName() { return worldName; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }
    public Material icon() { return icon; }
    public long createdAt() { return createdAt; }

    public Location location() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z, yaw, pitch);
    }

    public PlayerWarp renamed(String newName) {
        return new PlayerWarp(newName, ownerUuid, ownerName, worldName, x, y, z, yaw, pitch, icon, createdAt);
    }

    public PlayerWarp withIcon(Material newIcon) {
        return new PlayerWarp(name, ownerUuid, ownerName, worldName, x, y, z, yaw, pitch, newIcon, createdAt);
    }
}
