package com.oncearial.playerwarps.command;

import com.oncearial.playerwarps.PlayerWarps;
import com.oncearial.playerwarps.model.PlayerWarp;
import com.oncearial.playerwarps.util.ItemCosts;
import com.oncearial.playerwarps.util.Text;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
            default -> warp(sender, args[0]);
        }
        return true;
    }

    private void set(CommandSender sender, String[] args) {
        String prefix = msg("prefix");
        if (!(sender instanceof Player player)) { Text.send(sender, prefix, msg("player-only")); return; }
        if (!player.hasPermission("playerwarps.set")) { Text.send(player, prefix, msg("no-permission")); return; }
        if (args.length < 2) { Text.send(player, prefix, msg("usage-set")); return; }
        String name = args[1].toLowerCase();
        if (!validName(name)) { Text.send(player, prefix, msg("invalid-name")); return; }
        if (plugin.storage().exists(name)) { Text.send(player, prefix, msg("warp-exists")); return; }
        int limit = plugin.getConfig().getInt("settings.max-warps-per-player", 3);
        if (!player.hasPermission("playerwarps.admin") && plugin.storage().countOwned(player.getUniqueId()) >= limit) {
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
        Optional<PlayerWarp> optional = plugin.storage().getWarp(args[1]);
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
        Optional<PlayerWarp> optional = plugin.storage().getWarp(args[1]);
        if (optional.isEmpty()) { Text.send(player, prefix, msg("warp-not-found")); return; }
        String newName = args[2].toLowerCase();
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
        Optional<PlayerWarp> optional = plugin.storage().getWarp(args[1]);
        if (optional.isEmpty()) { Text.send(player, prefix, msg("warp-not-found")); return; }
        PlayerWarp warp = optional.get();
        if (!warp.ownerUuid().equals(player.getUniqueId()) && !player.hasPermission("playerwarps.admin")) {
            Text.send(player, prefix, msg("not-owner")); return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) { Text.send(player, prefix, "&cHold the icon item in your main hand."); return; }
        plugin.storage().replaceWarp(warp.name(), warp.withIcon(hand.getType()));
        Text.send(player, prefix, "&aUpdated icon for &e" + warp.name() + "&a.");
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
        sender.sendMessage(Text.color("&e/" + label + " delete <name> &7- Delete your warp"));
        sender.sendMessage(Text.color("&e/" + label + " rename <old> <new> &7- Rename your warp"));
        sender.sendMessage(Text.color("&e/" + label + " seticon <name> &7- Set icon from hand"));
        sender.sendMessage(Text.color("&e/" + label + " manage &7- Manage your warps"));
    }

    private boolean validName(String name) { return name.matches("[a-zA-Z0-9_-]{1,16}"); }
    private String msg(String key) { return plugin.getConfig().getString("messages." + key, key); }
    @SuppressWarnings("unchecked")
    private List<ItemCosts.Cost> costs() { return ItemCosts.readCosts((List<Map<?, ?>>)(List<?>) plugin.getConfig().getMapList("costs.create-warp")); }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> base = new ArrayList<>(List.of("help", "set", "delete", "rename", "seticon", "list", "manage"));
            if (sender.hasPermission("playerwarps.admin")) base.add("reload");
            for (PlayerWarp warp : plugin.storage().getWarps()) base.add(warp.name());
            return filter(base, args[0]);
        }
        if (args.length == 2 && List.of("delete", "rename", "seticon").contains(args[0].toLowerCase())) {
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
