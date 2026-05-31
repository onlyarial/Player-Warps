package com.oncearial.playerwarps.command;

import com.oncearial.playerwarps.PlayerWarps;
import com.oncearial.playerwarps.model.PlayerWarp;
import com.oncearial.playerwarps.util.ItemCosts;
import com.oncearial.playerwarps.util.Text;
import com.oncearial.playerwarps.util.WarpNames;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class PWarpCommand implements CommandExecutor, TabCompleter {
    private final PlayerWarps plugin;

    public PWarpCommand(PlayerWarps plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = msg("prefix");
        if (args.length == 0) {
            if (!(sender instanceof Player player)) { Text.send(sender, prefix, msg("player-only")); return true; }
            if (!player.hasPermission("playerwarps.gui")) { Text.send(sender, prefix, msg("no-permission")); return true; }
            plugin.warpGui().openBrowse(player, 0);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help" -> sendHelp(sender, label);
            case "set", "create" -> set(sender, args);
            case "delete", "del", "remove" -> delete(sender, args);
            case "rename" -> rename(sender, args);
            case "seticon" -> setIcon(sender, args);
            case "favorite", "fav" -> favorite(sender, args);
            case "description", "desc", "setdesc" -> description(sender, args);
            case "setfeatured" -> setFeatured(sender, args);
            case "list", "menu", "gui" -> {
                if (!(sender instanceof Player player)) { Text.send(sender, prefix, msg("player-only")); return true; }
                plugin.warpGui().openBrowse(player, 0);
            }
            case "manage" -> {
                if (!(sender instanceof Player player)) { Text.send(sender, prefix, msg("player-only")); return true; }
                plugin.warpGui().openManage(player, 0);
            }
            case "reload" -> {
                if (!sender.hasPermission("playerwarps.admin")) { Text.send(sender, prefix, msg("no-permission")); return true; }
                plugin.reloadEverything();
                Text.send(sender, prefix, msg("reloaded"));
            }
            default -> warp(sender, WarpNames.join(args, 0));
        }
        return true;
    }

    private void set(CommandSender sender, String[] args) {
        String prefix = msg("prefix");
        if (!(sender instanceof Player player)) { Text.send(sender, prefix, msg("player-only")); return; }
        if (!player.hasPermission("playerwarps.set")) { Text.send(player, prefix, msg("no-permission")); return; }
        if (args.length < 2) { Text.send(player, prefix, msg("usage-set")); return; }
        String name = WarpNames.join(args, 1);
        if (!validName(name)) { Text.send(player, prefix, msg("invalid-name")); return; }
        if (plugin.storage().exists(name)) { Text.send(player, prefix, msg("warp-exists")); return; }
        int limit = plugin.getConfig().getInt("settings.max-warps-per-player", 3);
        if (!player.hasPermission("playerwarps.admin") && limit >= 0 && plugin.storage().countOwned(player.getUniqueId()) >= limit) {
            Text.send(player, prefix, msg("limit-reached")); return;
        }
        List<ItemCosts.Cost> costs = costs();
        if (plugin.getConfig().getBoolean("costs.enabled", true) && !costs.isEmpty()) {
            if (!ItemCosts.has(player, costs)) {
                Text.send(player, prefix, msg("cost-missing").replace("%cost%", ItemCosts.pretty(costs)));
                return;
            }
            ItemCosts.take(player, costs);
            Text.send(player, prefix, msg("cost-taken").replace("%cost%", ItemCosts.pretty(costs)));
        }
        Material icon = Material.matchMaterial(plugin.getConfig().getString("settings.default-icon", "ENDER_PEARL"));
        if (icon == null) icon = Material.ENDER_PEARL;
        PlayerWarp warp = new PlayerWarp(name, player.getUniqueId(), player.getName(), player.getLocation(), icon, System.currentTimeMillis());
        plugin.storage().addWarp(warp);
        Text.send(player, prefix, msg("warp-created").replace("%warp%", name));
    }

    private void delete(CommandSender sender, String[] args) {
        String prefix = msg("prefix");
        if (!(sender instanceof Player player)) { Text.send(sender, prefix, msg("player-only")); return; }
        if (args.length < 2) { Text.send(player, prefix, msg("usage-delete")); return; }
        Optional<PlayerWarp> optional = plugin.storage().getWarp(WarpNames.join(args, 1));
        if (optional.isEmpty()) { Text.send(player, prefix, msg("warp-not-found")); return; }
        PlayerWarp warp = optional.get();
        if (!warp.ownerUuid().equals(player.getUniqueId()) && !player.hasPermission("playerwarps.admin")) {
            Text.send(player, prefix, msg("not-owner")); return;
        }
        plugin.storage().removeWarp(warp.name());
        Text.send(player, prefix, msg("warp-deleted").replace("%warp%", warp.name()));
    }

    private void rename(CommandSender sender, String[] args) {
        String prefix = msg("prefix");
        if (!(sender instanceof Player player)) { Text.send(sender, prefix, msg("player-only")); return; }
        if (args.length < 3) { Text.send(player, prefix, msg("usage-rename")); return; }

        RenameParts parts = parseRename(args);
        if (parts == null) { Text.send(player, prefix, msg("usage-rename")); return; }

        Optional<PlayerWarp> optional = plugin.storage().getWarp(parts.oldName());
        if (optional.isEmpty()) { Text.send(player, prefix, msg("warp-not-found")); return; }

        String newName = parts.newName();
        if (!validName(newName)) { Text.send(player, prefix, msg("invalid-name")); return; }
        if (plugin.storage().exists(newName)) { Text.send(player, prefix, msg("warp-exists")); return; }
        PlayerWarp warp = optional.get();
        if (!warp.ownerUuid().equals(player.getUniqueId()) && !player.hasPermission("playerwarps.admin")) {
            Text.send(player, prefix, msg("not-owner")); return;
        }
        plugin.storage().replaceWarp(warp.name(), warp.renamed(newName));
        Text.send(player, prefix, msg("warp-renamed").replace("%old%", warp.name()).replace("%new%", newName));
    }

    private void setIcon(CommandSender sender, String[] args) {
        String prefix = msg("prefix");
        if (!(sender instanceof Player player)) { Text.send(sender, prefix, msg("player-only")); return; }
        if (args.length < 2) { Text.send(player, prefix, "&cUsage: /pwarp seticon <name>"); return; }
        Optional<PlayerWarp> optional = plugin.storage().getWarp(WarpNames.join(args, 1));
        if (optional.isEmpty()) { Text.send(player, prefix, msg("warp-not-found")); return; }
        PlayerWarp warp = optional.get();
        if (!warp.ownerUuid().equals(player.getUniqueId()) && !player.hasPermission("playerwarps.admin")) {
            Text.send(player, prefix, msg("not-owner")); return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) { Text.send(player, prefix, "&cHold the icon item in your main hand."); return; }
        String headOwner = null;
        if (hand.getType() == Material.PLAYER_HEAD || hand.getType() == Material.PLAYER_WALL_HEAD) {
            ItemMeta meta = hand.getItemMeta();
            if (meta instanceof SkullMeta skullMeta && skullMeta.getOwningPlayer() != null) {
                headOwner = skullMeta.getOwningPlayer().getName();
            }
        }
        plugin.storage().replaceWarp(warp.name(), warp.withIcon(hand.getType(), headOwner));
        Text.send(player, prefix, "&aUpdated icon for &e" + warp.name() + "&a.");
    }

    private void favorite(CommandSender sender, String[] args) {
        String prefix = msg("prefix");
        if (!(sender instanceof Player player)) { Text.send(sender, prefix, msg("player-only")); return; }
        if (!player.hasPermission("playerwarps.favorite")) { Text.send(player, prefix, msg("no-permission")); return; }
        if (args.length < 2) { Text.send(player, prefix, msg("usage-favorite")); return; }
        String name = WarpNames.join(args, 1);
        Optional<PlayerWarp> optional = plugin.storage().getWarp(name);
        if (optional.isEmpty()) { Text.send(player, prefix, msg("warp-not-found")); return; }
        boolean nowFavorite = plugin.favorites().toggle(player.getUniqueId(), optional.get().name());
        Text.send(player, prefix, (nowFavorite ? msg("favorite-added") : msg("favorite-removed")).replace("%warp%", optional.get().name()));
    }

    private void description(CommandSender sender, String[] args) {
        String prefix = msg("prefix");
        if (!(sender instanceof Player player)) { Text.send(sender, prefix, msg("player-only")); return; }
        if (!player.hasPermission("playerwarps.description")) { Text.send(player, prefix, msg("no-permission")); return; }
        if (args.length < 3) { Text.send(player, prefix, msg("usage-description")); return; }
        DescriptionParts parts = parseDescription(args);
        if (parts == null) { Text.send(player, prefix, msg("usage-description")); return; }
        Optional<PlayerWarp> optional = plugin.storage().getWarp(parts.warpName());
        if (optional.isEmpty()) { Text.send(player, prefix, msg("warp-not-found")); return; }
        PlayerWarp warp = optional.get();
        if (!warp.ownerUuid().equals(player.getUniqueId()) && !player.hasPermission("playerwarps.admin")) {
            Text.send(player, prefix, msg("not-owner")); return;
        }
        plugin.storage().replaceWarp(warp.name(), warp.withDescription(parts.description()));
        Text.send(player, prefix, msg("description-set").replace("%warp%", warp.name()));
    }

    private void setFeatured(CommandSender sender, String[] args) {
        String prefix = msg("prefix");
        if (!(sender instanceof Player player)) { Text.send(sender, prefix, msg("player-only")); return; }
        if (!player.hasPermission("playerwarps.setfeatured") && !player.hasPermission("playerwarps.admin")) { Text.send(player, prefix, msg("no-permission")); return; }
        if (args.length < 2) { Text.send(player, prefix, msg("usage-setfeatured")); return; }
        String name = WarpNames.join(args, 1);
        Optional<PlayerWarp> optional = plugin.storage().getWarp(name);
        if (optional.isEmpty()) { Text.send(player, prefix, msg("warp-not-found")); return; }

        int maxSlots = plugin.guiConfig().getIntegerList("browse.featured.slots").size();
        if (maxSlots <= 0) maxSlots = 6;
        if (plugin.featured().getFeatured().contains(WarpNames.normalize(optional.get().name()))) {
            Text.send(player, prefix, msg("featured-already").replace("%warp%", optional.get().name()));
            return;
        }

        List<ItemCosts.Cost> featuredCosts = featuredCosts();
        if (!player.hasPermission("playerwarps.admin") && plugin.getConfig().getBoolean("costs.enabled", true) && !featuredCosts.isEmpty()) {
            if (!ItemCosts.has(player, featuredCosts)) {
                Text.send(player, prefix, msg("featured-cost-missing").replace("%cost%", ItemCosts.pretty(featuredCosts)));
                return;
            }
            ItemCosts.take(player, featuredCosts);
            Text.send(player, prefix, msg("featured-cost-taken").replace("%cost%", ItemCosts.pretty(featuredCosts)));
        }

        boolean added = plugin.featured().add(optional.get().name(), maxSlots);
        Text.send(player, prefix, (added ? msg("featured-set") : msg("featured-already")).replace("%warp%", optional.get().name()));
    }

    private void warp(CommandSender sender, String name) {
        String prefix = msg("prefix");
        if (!(sender instanceof Player player)) { Text.send(sender, prefix, msg("player-only")); return; }
        if (!player.hasPermission("playerwarps.use")) { Text.send(player, prefix, msg("no-permission")); return; }
        Optional<PlayerWarp> optional = plugin.storage().getWarp(name);
        if (optional.isEmpty()) { Text.send(player, prefix, msg("warp-not-found")); return; }
        plugin.teleportListener().startTeleport(player, optional.get());
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(Text.color("&8&m---------------- &6Player Warps &8&m----------------"));
        sender.sendMessage(Text.color("&e/" + label + " &7- Open warps GUI"));
        sender.sendMessage(Text.color("&e/" + label + " <name> &7- Teleport to a warp"));
        sender.sendMessage(Text.color("&e/" + label + " set <name> &7- Create a warp"));
        sender.sendMessage(Text.color("&8Tip: &7Names support uppercase, spaces, Unicode, & colors, and &#RRGGBB hex colors."));
        sender.sendMessage(Text.color("&e/" + label + " delete <name> &7- Delete your warp"));
        sender.sendMessage(Text.color("&e/" + label + " rename <old> <new> &7- Rename your warp"));
        sender.sendMessage(Text.color("&8Tip: &7Example: &e/" + label + " rename My-Warp &#ff66ccThis-Is-A-Warp"));
        sender.sendMessage(Text.color("&e/" + label + " seticon <name> &7- Set icon from hand"));
        sender.sendMessage(Text.color("&8Tip: &7Right-click a warp in the GUI to favorite or unfavorite it."));
        sender.sendMessage(Text.color("&e/" + label + " desc <name> <description> &7- Set a warp description"));
        sender.sendMessage(Text.color("&e/" + label + " manage &7- Manage your warps"));
    }

    private boolean validName(String name) { return WarpNames.isValid(name, plugin.getConfig().getInt("settings.max-warp-name-length", 32)); }

    private RenameParts parseRename(String[] args) {
        if (args.length < 3) return null;

        // Format: /pwarp rename <old name> <new name>
        // To support old names with spaces without needing a separator, try the longest
        // existing old-warp name first, then use everything after it as the new name.
        // Example: /pwarp rename Old Spawn &#ff66ccNew Spawn
        for (int end = args.length - 1; end >= 2; end--) {
            String oldName = WarpNames.join(args, 1, end);
            String newName = WarpNames.join(args, end, args.length);
            if (!oldName.isBlank() && !newName.isBlank() && plugin.storage().exists(oldName)) {
                return new RenameParts(oldName, newName);
            }
        }

        // Fallback for the common case where the old name is one word:
        // /pwarp rename My-Warp &#ff66ccThis-Is-A-Warp
        String oldName = args[1];
        String newName = WarpNames.join(args, 2);
        if (oldName.isBlank() || newName.isBlank()) return null;
        return new RenameParts(oldName, newName);
    }

    private DescriptionParts parseDescription(String[] args) {
        for (int end = args.length - 1; end >= 2; end--) {
            String warpName = WarpNames.join(args, 1, end);
            String description = WarpNames.join(args, end, args.length);
            if (!warpName.isBlank() && !description.isBlank() && plugin.storage().exists(warpName)) {
                return new DescriptionParts(warpName, description);
            }
        }
        String warpName = args[1];
        String description = WarpNames.join(args, 2);
        if (warpName.isBlank() || description.isBlank()) return null;
        return new DescriptionParts(warpName, description);
    }

    private record RenameParts(String oldName, String newName) {}
    private record DescriptionParts(String warpName, String description) {}
    private String msg(String key) { return plugin.getConfig().getString("messages." + key, key); }
    @SuppressWarnings("unchecked")
    private List<ItemCosts.Cost> costs() { return ItemCosts.readCosts((List<Map<?, ?>>)(List<?>) plugin.getConfig().getMapList("costs.create-warp")); }

    @SuppressWarnings("unchecked")
    private List<ItemCosts.Cost> featuredCosts() {
        List<Map<?, ?>> featured =
                (List<Map<?, ?>>)(List<?>) plugin.getConfig().getMapList("costs.featured-cost");

        if (featured.isEmpty()) {
            featured =
                    (List<Map<?, ?>>)(List<?>) plugin.getConfig().getMapList("featured-cost");
        }

        return ItemCosts.readCosts(featured);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> base = new ArrayList<>(List.of("help", "set", "delete", "rename", "seticon", "desc", "list", "manage"));
            if (sender.hasPermission("playerwarps.admin")) { base.add("reload"); base.add("setfeatured"); }
            for (PlayerWarp warp : plugin.storage().getWarps()) base.add(warp.name());
            return filter(base, args[0]);
        }
        if (args.length == 2 && List.of("delete", "rename", "seticon", "description", "desc", "setdesc", "setfeatured").contains(args[0].toLowerCase())) {
            List<String> names = new ArrayList<>();
            for (PlayerWarp warp : plugin.storage().getWarps()) names.add(warp.name());
            return filter(names, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String start) {
        List<String> out = new ArrayList<>();
        for (String option : options) if (option.toLowerCase().startsWith(start.toLowerCase())) out.add(option);
        return out;
    }
}
