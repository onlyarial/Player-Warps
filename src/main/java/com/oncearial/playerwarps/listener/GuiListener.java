package com.oncearial.playerwarps.listener;

import com.oncearial.playerwarps.PlayerWarps;
import com.oncearial.playerwarps.model.PlayerWarp;
import com.oncearial.playerwarps.util.Text;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class GuiListener implements Listener {
    private final PlayerWarps plugin;

    public GuiListener(PlayerWarps plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        boolean browse = title.startsWith(Text.color(plugin.getConfig().getString("settings.gui-title", "&8Player Warps")));
        boolean manage = title.startsWith(Text.color(plugin.getConfig().getString("settings.manage-gui-title", "&8Your Player Warps")));
        if (!browse && !manage) return;
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) return;
        int slot = event.getRawSlot();
        int page = plugin.warpGui().pageFromTitle(title);
        if (slot == 49) { player.closeInventory(); return; }
        if (slot == 45 && clicked.getType() == Material.ARROW) {
            if (browse) plugin.warpGui().openBrowse(player, page - 1); else plugin.warpGui().openManage(player, page - 1);
            return;
        }
        if (slot == 53 && clicked.getType() == Material.ARROW) {
            if (browse) plugin.warpGui().openBrowse(player, page + 1); else plugin.warpGui().openManage(player, page + 1);
            return;
        }
        if (slot >= 45) return;
        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).toLowerCase();
        Optional<PlayerWarp> optional = plugin.storage().getWarp(name);
        if (optional.isEmpty()) { player.closeInventory(); Text.send(player, msg("prefix"), msg("warp-not-found")); return; }
        PlayerWarp warp = optional.get();
        player.closeInventory();
        if (manage && event.isRightClick()) {
            plugin.storage().removeWarp(warp.name());
            Text.send(player, msg("prefix"), msg("warp-deleted").replace("%warp%", warp.name()));
            return;
        }
        plugin.teleportListener().startTeleport(player, warp);
    }

    private String msg(String key) { return plugin.getConfig().getString("messages." + key, key); }
}
