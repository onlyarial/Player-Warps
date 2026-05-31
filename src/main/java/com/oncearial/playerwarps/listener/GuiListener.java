package com.oncearial.playerwarps.listener;

import com.oncearial.playerwarps.PlayerWarps;
import com.oncearial.playerwarps.gui.WarpGuiHolder;
import com.oncearial.playerwarps.model.PlayerWarp;
import com.oncearial.playerwarps.util.Text;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public class GuiListener implements Listener {
    private final PlayerWarps plugin;

    public GuiListener(PlayerWarps plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof WarpGuiHolder holder)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String base = holder.type() == WarpGuiHolder.Type.BROWSE ? "browse" : "manage";
        int previousSlot = plugin.guiConfig().getInt(base + ".navigation.previous.slot", -1);
        int nextSlot = plugin.guiConfig().getInt(base + ".navigation.next.slot", -1);

        if (slot == previousSlot) {
            if (holder.page() <= 0) {
                player.closeInventory();
                return;
            }
            if (holder.type() == WarpGuiHolder.Type.BROWSE) plugin.warpGui().openBrowse(player, holder.page() - 1);
            else plugin.warpGui().openManage(player, holder.page() - 1);
            return;
        }

        if (slot == nextSlot) {
            if (holder.type() == WarpGuiHolder.Type.BROWSE) plugin.warpGui().openBrowse(player, holder.page() + 1);
            else plugin.warpGui().openManage(player, holder.page() + 1);
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        String warpName = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "warp_name"), PersistentDataType.STRING);
        if (warpName == null || warpName.isBlank()) return;

        Optional<PlayerWarp> optional = plugin.storage().getWarp(warpName);
        if (optional.isEmpty()) {
            player.closeInventory();
            Text.send(player, msg("prefix"), msg("warp-not-found"));
            return;
        }

        PlayerWarp warp = optional.get();

        if (holder.type() == WarpGuiHolder.Type.BROWSE && event.isRightClick()) {
            if (!player.hasPermission("playerwarps.favorite")) {
                Text.send(player, msg("prefix"), msg("no-permission"));
                return;
            }
            boolean nowFavorite = plugin.favorites().toggle(player.getUniqueId(), warp.name());
            Text.send(player, msg("prefix"), (nowFavorite ? msg("favorite-added") : msg("favorite-removed")).replace("%warp%", warp.name()));
            plugin.warpGui().openBrowse(player, holder.page());
            return;
        }

        player.closeInventory();

        if (holder.type() == WarpGuiHolder.Type.MANAGE && event.isRightClick()) {
            plugin.storage().removeWarp(warp.name());
            Text.send(player, msg("prefix"), msg("warp-deleted").replace("%warp%", warp.name()));
            return;
        }

        plugin.teleportListener().startTeleport(player, warp);
    }

    private String msg(String key) {
        return plugin.getConfig().getString("messages." + key, key);
    }
}
