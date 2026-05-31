package com.oncearial.playerwarps.listener;

import com.oncearial.playerwarps.PlayerWarps;
import com.oncearial.playerwarps.model.PlayerWarp;
import com.oncearial.playerwarps.util.Text;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportListener implements Listener {
    private final PlayerWarps plugin;
    private final Map<UUID, PendingTeleport> pending = new HashMap<>();

    public TeleportListener(PlayerWarps plugin) { this.plugin = plugin; }

    public void startTeleport(Player player, PlayerWarp warp) {
        Location target = warp.location();
        if (target == null) { Text.send(player, msg("prefix"), "&cThat warp's world is not loaded."); return; }
        if (!plugin.getConfig().getBoolean("settings.allow-cross-world", true) && !target.getWorld().equals(player.getWorld())) {
            Text.send(player, msg("prefix"), "&cCross-world player warps are disabled."); return;
        }
        cancel(player, false);
        int delay = plugin.getConfig().getInt("settings.teleport-delay-seconds", 3);
        if (delay <= 0) {
            player.teleport(target);
            Text.send(player, msg("prefix"), msg("teleported").replace("%warp%", warp.name()));
            return;
        }
        Location start = player.getLocation().clone();
        Text.send(player, msg("prefix"), msg("teleport-starting").replace("%warp%", warp.name()).replace("%seconds%", String.valueOf(delay)));
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pending.remove(player.getUniqueId());
            if (!player.isOnline()) return;
            player.teleport(target);
            Text.send(player, msg("prefix"), msg("teleported").replace("%warp%", warp.name()));
        }, delay * 20L);
        pending.put(player.getUniqueId(), new PendingTeleport(start, task));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("settings.cancel-teleport-on-move", true)) return;
        PendingTeleport teleport = pending.get(event.getPlayer().getUniqueId());
        if (teleport == null) return;
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            cancel(event.getPlayer(), true);
        }
    }

    private void cancel(Player player, boolean message) {
        PendingTeleport teleport = pending.remove(player.getUniqueId());
        if (teleport != null) {
            teleport.task().cancel();
            if (message) Text.send(player, msg("prefix"), msg("teleport-cancelled"));
        }
    }

    private String msg(String key) { return plugin.getConfig().getString("messages." + key, key); }
    private record PendingTeleport(Location start, BukkitTask task) {}
}
