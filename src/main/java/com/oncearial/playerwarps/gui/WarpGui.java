package com.oncearial.playerwarps.gui;

import com.oncearial.playerwarps.PlayerWarps;
import com.oncearial.playerwarps.model.PlayerWarp;
import com.oncearial.playerwarps.util.Text;
import com.oncearial.playerwarps.util.WarpNames;
import com.oncearial.playerwarps.util.ItemCosts;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class WarpGui {
    private final PlayerWarps plugin;

    public WarpGui(PlayerWarps plugin) {
        this.plugin = plugin;
    }

    public void openBrowse(Player player, int page) {
        List<PlayerWarp> warps = new ArrayList<>(plugin.storage().getWarps());
        List<String> favorites = plugin.favorites().favorites(player.getUniqueId());
        warps.sort((a, b) -> {
            int ai = favorites.indexOf(WarpNames.normalize(a.name()));
            int bi = favorites.indexOf(WarpNames.normalize(b.name()));
            boolean af = ai >= 0;
            boolean bf = bi >= 0;
            if (af && bf) return Integer.compare(ai, bi);
            if (af) return -1;
            if (bf) return 1;
            return Text.stripFormatting(displayName(a.name())).compareToIgnoreCase(Text.stripFormatting(displayName(b.name())));
        });
        open(player, page, WarpGuiHolder.Type.BROWSE, warps);
    }

    public void openManage(Player player, int page) {
        List<PlayerWarp> warps = plugin.storage().ownedBy(player.getUniqueId());
        warps.sort(Comparator.comparing(w -> Text.stripFormatting(displayName(w.name())).toLowerCase()));
        open(player, page, WarpGuiHolder.Type.MANAGE, warps);
    }

    private void open(Player player, int page, WarpGuiHolder.Type type, List<PlayerWarp> warps) {
        if (page < 0) page = 0;

        FileConfiguration gui = plugin.guiConfig();
        String base = type == WarpGuiHolder.Type.BROWSE ? "browse" : "manage";

        int size = gui.getInt(base + ".size", 54);
        if (size < 9) size = 9;
        if (size > 54) size = 54;
        size = (size / 9) * 9;

        List<Integer> warpSlots = gui.getIntegerList(base + ".warp-slots");
        if (warpSlots.isEmpty()) {
            for (int i = 9; i < Math.min(45, size); i++) warpSlots.add(i);
        }

        int perPage = Math.max(1, warpSlots.size());
        int totalPages = Math.max(1, (int) Math.ceil(warps.size() / (double) perPage));
        if (page >= totalPages) page = totalPages - 1;

        String title = Text.color(gui.getString(base + ".title", type == WarpGuiHolder.Type.BROWSE ? "&ePlayerWarps" : "&eYour Player Warps"));
        Inventory inv = Bukkit.createInventory(new WarpGuiHolder(type, page), size, title);

        applyGuiItems(inv, base + ".items", page, totalPages);
        if (type == WarpGuiHolder.Type.BROWSE) applyFeatured(inv, size, player);

        int start = page * perPage;
        for (int i = 0; i < warpSlots.size() && start + i < warps.size(); i++) {
            int slot = warpSlots.get(i);
            if (slot >= 0 && slot < size) {
                inv.setItem(slot, warpItem(warps.get(start + i), type == WarpGuiHolder.Type.MANAGE, player));
            }
        }

        int previousSlot = gui.getInt(base + ".navigation.previous.slot", -1);
        int nextSlot = gui.getInt(base + ".navigation.next.slot", -1);
        int pageSlot = gui.getInt(base + ".navigation.page.slot", -1);

        // Always show the back slot. On page 1 it acts as Close.
        if (previousSlot >= 0 && previousSlot < size) {
            inv.setItem(previousSlot, configItem(page > 0 ? base + ".navigation.previous" : base + ".navigation.close", page, totalPages));
        }
        if (page + 1 < totalPages && nextSlot >= 0 && nextSlot < size) {
            inv.setItem(nextSlot, configItem(base + ".navigation.next", page, totalPages));
        }
        if (pageSlot >= 0 && pageSlot < size) {
            inv.setItem(pageSlot, configItem(base + ".navigation.page", page, totalPages));
        }

        player.openInventory(inv);
    }

    private void applyFeatured(Inventory inv, int size, Player viewer) {
        List<Integer> slots = plugin.guiConfig().getIntegerList("browse.featured.slots");
        if (slots.isEmpty()) slots = List.of(45, 46, 47, 51, 52, 53);

        List<String> featured = plugin.featured().getFeatured();
        for (int i = 0; i < slots.size(); i++) {
            int slot = slots.get(i);
            if (slot < 0 || slot >= size) continue;
            if (i < featured.size()) {
                Optional<PlayerWarp> warp = plugin.storage().getWarp(featured.get(i));
                if (warp.isPresent()) inv.setItem(slot, warpItem(warp.get(), false, viewer));
            }
        }
    }

    private void applyGuiItems(Inventory inv, String path, int page, int totalPages) {
        ConfigurationSection section = plugin.guiConfig().getConfigurationSection(path);
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String itemPath = path + "." + key;
            ItemStack item = configItem(itemPath, page, totalPages);
            for (int slot : plugin.guiConfig().getIntegerList(itemPath + ".slots")) {
                if (slot >= 0 && slot < inv.getSize()) inv.setItem(slot, item);
            }
        }
    }

    private ItemStack warpItem(PlayerWarp warp, boolean manage, Player viewer) {
        String path = manage ? "manage.warp-item" : "browse.warp-item";
        Material fallback = warp.icon() == null ? Material.ENDER_PEARL : warp.icon();
        ItemStack item = new ItemStack(fallback);

        if ((fallback == Material.PLAYER_HEAD || fallback == Material.PLAYER_WALL_HEAD) && warp.headOwner() != null && !warp.headOwner().isBlank()) {
            ItemMeta rawMeta = item.getItemMeta();
            if (rawMeta instanceof SkullMeta skullMeta) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(warp.headOwner());
                skullMeta.setOwningPlayer(offlinePlayer);
                item.setItemMeta(skullMeta);
            }
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = plugin.guiConfig().getString(path + ".name", "&6%warp%");
        meta.setDisplayName(Text.color(warpPlaceholders(name, warp, viewer)));

        List<String> lore = new ArrayList<>();
        for (String line : plugin.guiConfig().getStringList(path + ".lore")) {
            lore.add(Text.color(warpPlaceholders(line, warp, viewer)));
        }
        meta.setLore(lore);

        int modelData = plugin.guiConfig().getInt(path + ".custom-model-data", 0);
        if (modelData > 0) meta.setCustomModelData(modelData);

        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "warp_name"), PersistentDataType.STRING, warp.name());
        item.setItemMeta(meta);
        return item;
    }

    private String warpPlaceholders(String input, PlayerWarp warp, Player viewer) {
        boolean favorite = viewer != null && plugin.favorites().isFavorite(viewer.getUniqueId(), warp.name());
        String description = warp.description().isBlank() ? plugin.guiConfig().getString("placeholders.no-description", "&7No description set.") : warp.description();
        return input
                .replace("%favorite_star%", favorite ? plugin.guiConfig().getString("placeholders.favorite-star", "&e★ ") : "")
                .replace("%warp%", displayName(warp.name()))
                .replace("%warp_raw%", warp.name())
                .replace("%description%", description)
                .replace("%visits%", String.valueOf(warp.visits()))
                .replace("%owner%", warp.ownerName())
                .replace("%world%", worldDisplayName(warp.worldName()))
                .replace("%world_raw%", warp.worldName())
                .replace("%x%", String.valueOf(Math.round(warp.x())))
                .replace("%y%", String.valueOf(Math.round(warp.y())))
                .replace("%z%", String.valueOf(Math.round(warp.z())));
    }

    private String displayName(String rawName) {
        return rawName.replace('-', ' ');
    }

    private String worldDisplayName(String rawWorld) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("world-naming");
        if (section != null) {
            String value = section.getString(rawWorld);
            if (value != null) return value;
        }
        for (Map<?, ?> map : plugin.getConfig().getMapList("world-naming")) {
            Object value = map.get(rawWorld);
            if (value != null) return String.valueOf(value);
        }
        return rawWorld;
    }

    private ItemStack configItem(String path, int page, int totalPages) {
        Material material = Material.matchMaterial(plugin.guiConfig().getString(path + ".material", "GRAY_STAINED_GLASS_PANE"));
        if (material == null) material = Material.GRAY_STAINED_GLASS_PANE;

        ItemStack item = new ItemStack(material, Math.max(1, plugin.guiConfig().getInt(path + ".amount", 1)));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(Text.color(pagePlaceholders(plugin.guiConfig().getString(path + ".name", " "), page, totalPages)));

        List<String> lore = new ArrayList<>();
        for (String line : plugin.guiConfig().getStringList(path + ".lore")) {
            lore.add(Text.color(pagePlaceholders(line, page, totalPages)));
        }
        meta.setLore(lore);

        int modelData = plugin.guiConfig().getInt(path + ".custom-model-data", 0);
        if (modelData > 0) meta.setCustomModelData(modelData);

        item.setItemMeta(meta);
        return item;
    }

    private String pagePlaceholders(String input, int page, int totalPages) {
        String featuredCost = ItemCosts.pretty(ItemCosts.readCosts((List<Map<?, ?>>)(List<?>) plugin.getConfig().getMapList("costs.featured-cost")));
        return input
                .replace("%page%", String.valueOf(page))
                .replace("%page_display%", String.valueOf(page + 1))
                .replace("%previous_page%", String.valueOf(page))
                .replace("%next_page%", String.valueOf(page + 2))
                .replace("%total_pages%", String.valueOf(totalPages))
                .replace("%featured_cost%", featuredCost);
    }
}
