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
    private final String headOwner;
    private final String description;
    private final long visits;
    private final long createdAt;

    public PlayerWarp(String name, UUID ownerUuid, String ownerName, Location location, Material icon, long createdAt) {
        this(name, ownerUuid, ownerName, location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch(), icon, null, "", 0L, createdAt);
    }

    public PlayerWarp(String name, UUID ownerUuid, String ownerName, String worldName, double x, double y, double z, float yaw, float pitch, Material icon, long createdAt) {
        this(name, ownerUuid, ownerName, worldName, x, y, z, yaw, pitch, icon, null, "", 0L, createdAt);
    }

    public PlayerWarp(String name, UUID ownerUuid, String ownerName, String worldName, double x, double y, double z, float yaw, float pitch, Material icon, String headOwner, String description, long visits, long createdAt) {
        this.name = name;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.icon = icon;
        this.headOwner = headOwner;
        this.description = description == null ? "" : description;
        this.visits = Math.max(0, visits);
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
    public String headOwner() { return headOwner; }
    public String description() { return description; }
    public long visits() { return visits; }
    public long createdAt() { return createdAt; }

    public Location location() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z, yaw, pitch);
    }

    public PlayerWarp renamed(String newName) {
        return new PlayerWarp(newName, ownerUuid, ownerName, worldName, x, y, z, yaw, pitch, icon, headOwner, description, visits, createdAt);
    }

    public PlayerWarp withIcon(Material newIcon, String newHeadOwner) {
        return new PlayerWarp(name, ownerUuid, ownerName, worldName, x, y, z, yaw, pitch, newIcon, newHeadOwner, description, visits, createdAt);
    }

    public PlayerWarp withDescription(String newDescription) {
        return new PlayerWarp(name, ownerUuid, ownerName, worldName, x, y, z, yaw, pitch, icon, headOwner, newDescription, visits, createdAt);
    }

    public PlayerWarp withVisits(long newVisits) {
        return new PlayerWarp(name, ownerUuid, ownerName, worldName, x, y, z, yaw, pitch, icon, headOwner, description, newVisits, createdAt);
    }
}
