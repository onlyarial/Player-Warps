package com.oncearial.playerwarps.gui;

import com.oncearial.playerwarps.PlayerWarps;
import com.oncearial.playerwarps.model.PlayerWarp;
import com.oncearial.playerwarps.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WarpGui {
    public static final String BROWSE_PREFIX = "PWARP:BROWSE:";
    public static final String MANAGE_PREFIX = "PWARP:MANAGE:";
    private final PlayerWarps plugin;

    public WarpGui(PlayerWarps plugin) { this.plugin = plugin; }

    public void openBrowse(Player player, int page) {
        List<PlayerWarp> warps = new ArrayList<>(plugin.storage().getWarps());
        warps.sort(Comparator.comparing(PlayerWarp::name));
        Inventory inv = Bukkit.createInventory(null, 54, Text.color(plugin.getConfig().getString("settings.gui-title", "&8Player Warps") + " &0#" + page));
        fill(inv);
        int start = page * 45;
        for (int slot = 0; slot < 45 && start + slot < warps.size(); slot++) {
            inv.setItem(slot, warpItem(warps.get(start + slot), false));
        }
        if (page > 0) inv.setItem(45, nav(Material.ARROW, "&ePrevious Page"));
        if (warps.size() > start + 45) inv.setItem(53, nav(Material.ARROW, "&eNext Page"));
        inv.setItem(49, nav(Material.BARRIER, "&cClose"));
        player.openInventory(inv);
    }

    public void openManage(Player player, int page) {
        List<PlayerWarp> warps = plugin.storage().ownedBy(player.getUniqueId());
        warps.sort(Comparator.comparing(PlayerWarp::name));
        Inventory inv = Bukkit.createInventory(null, 54, Text.color(plugin.getConfig().getString("settings.manage-gui-title", "&8Your Player Warps") + " &0#" + page));
        fill(inv);
        int start = page * 45;
        for (int slot = 0; slot < 45 && start + slot < warps.size(); slot++) {
            inv.setItem(slot, warpItem(warps.get(start + slot), true));
        }
        if (page > 0) inv.setItem(45, nav(Material.ARROW, "&ePrevious Page"));
        if (warps.size() > start + 45) inv.setItem(53, nav(Material.ARROW, "&eNext Page"));
        inv.setItem(49, nav(Material.BARRIER, "&cClose"));
        player.openInventory(inv);
    }

    private ItemStack warpItem(PlayerWarp warp, boolean manage) {
        ItemStack item = new ItemStack(warp.icon() == null ? Material.ENDER_PEARL : warp.icon());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Text.color("&6" + warp.name()));
        List<String> lore = new ArrayList<>();
        lore.add(Text.color("&7Owner: &f" + warp.ownerName()));
        lore.add(Text.color("&7World: &f" + warp.worldName()));
        lore.add("");
        lore.add(Text.color(manage ? "&eLeft-click: Teleport" : "&eClick to teleport"));
        if (manage) lore.add(Text.color("&cRight-click: Delete"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack nav(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Text.color(name));
        item.setItemMeta(meta);
        return item;
    }

    private void fill(Inventory inv) {
        ItemStack pane = nav(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, pane);
    }

    public int pageFromTitle(String title) {
        int index = title.lastIndexOf("#");
        if (index == -1) return 0;
        try { return Integer.parseInt(title.substring(index + 1)); } catch (NumberFormatException ignored) { return 0; }
    }
}
