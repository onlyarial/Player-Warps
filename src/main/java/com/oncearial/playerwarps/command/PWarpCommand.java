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
    private static final long FEATURE_CONFIRM_MS = 30_000L;
    private final PlayerWarps plugin;
    private final Map<UUID, PendingFeature> pendingFeatures = new HashMap<>();

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
            case "help" -> sendHelp(sender, label, args);
            case "set", "create" -> set(sender, args);
            case "delete", "del", "remove" -> delete(sender, args);
            case "rename" -> rename(sender, args);
            case "seticon" -> setIcon(sender, args);
            case "favorite", "fav" -> favorite(sender, args);
            case "description", "desc", "setdesc" -> description(sender, args);
            case "feature", "setfeatured" -> feature(sender, args);
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
        DescriptionParts parts = parseDescription(args, player);
        if (parts == null) { Text.send(player, prefix, msg("usage-description")); return; }
        Optional<PlayerWarp> optional = plugin.storage().getWarp(parts.warpName());
        if (optional.isEmpty()) { Text.send(player, prefix, msg("warp-not-found")); return; }
        PlayerWarp warp = optional.get();
        if (!warp.ownerUuid().equals(player.getUniqueId()) && !player.hasPermission("playerwarps.admin")) {
            Text.send(player, prefix, msg("not-owner")); return;
        }

        if (!validDescriptionLine(parts.description())) {
            Text.send(player, prefix, msg("invalid-description-line"));
            return;
        }

        List<String> lines = new ArrayList<>(warp.descriptionLines());
        switch (parts.action()) {
            case ADD -> {
                int maxLines = plugin.getConfig().getInt("settings.max-description-lines", 8);
                if (maxLines >= 0 && lines.size() >= maxLines) {
                    Text.send(player, prefix, msg("description-lines-limit").replace("%max%", String.valueOf(maxLines)));
                    return;
                }
                lines.add(parts.description());
                plugin.storage().replaceWarp(warp.name(), warp.withDescriptionLines(lines));
                Text.send(player, prefix, msg("description-added").replace("%warp%", warp.name()).replace("%line%", String.valueOf(lines.size())));
            }
            case SET -> {
                int maxLines = plugin.getConfig().getInt("settings.max-description-lines", 8);
                if (parts.lineNumber() < 1 || (maxLines >= 0 && parts.lineNumber() > maxLines)) {
                    Text.send(player, prefix, msg("invalid-description-line-number").replace("%max%", String.valueOf(maxLines)));
                    return;
                }
                while (lines.size() < parts.lineNumber()) lines.add("");
                lines.set(parts.lineNumber() - 1, parts.description());
                plugin.storage().replaceWarp(warp.name(), warp.withDescriptionLines(lines));
                Text.send(player, prefix, msg("description-line-set").replace("%warp%", warp.name()).replace("%line%", String.valueOf(parts.lineNumber())));
            }
            case REPLACE -> {
                plugin.storage().replaceWarp(warp.name(), warp.withDescription(parts.description()));
                Text.send(player, prefix, msg("description-set").replace("%warp%", warp.name()));
            }
        }
    }

    private void feature(CommandSender sender, String[] args) {
        String prefix = msg("prefix");
        if (!(sender instanceof Player player)) { Text.send(sender, prefix, msg("player-only")); return; }
        if (!player.hasPermission("playerwarps.feature") && !player.hasPermission("playerwarps.setfeatured") && !player.hasPermission("playerwarps.admin")) { Text.send(player, prefix, msg("no-permission")); return; }
        if (args.length < 2) { Text.send(player, prefix, msg("usage-feature")); return; }
        String name = WarpNames.join(args, 1);
        Optional<PlayerWarp> optional = plugin.storage().getWarp(name);
        if (optional.isEmpty()) { Text.send(player, prefix, msg("warp-not-found")); return; }
        PlayerWarp warp = optional.get();

        int maxSlots = plugin.guiConfig().getIntegerList("browse.featured.slots").size();
        if (maxSlots <= 0) maxSlots = 6;
        if (plugin.featured().getFeatured().contains(WarpNames.normalize(warp.name()))) {
            Text.send(player, prefix, msg("featured-already").replace("%warp%", warp.name()));
            return;
        }

        List<ItemCosts.Cost> featuredCosts = featuredCosts();
        boolean chargesCost = !player.hasPermission("playerwarps.admin") && plugin.getConfig().getBoolean("costs.enabled", true) && !featuredCosts.isEmpty();
        if (chargesCost && plugin.getConfig().getBoolean("settings.feature-confirmation", true) && !hasConfirmedFeature(player, warp)) {
            pendingFeatures.put(player.getUniqueId(), new PendingFeature(WarpNames.normalize(warp.name()), System.currentTimeMillis() + FEATURE_CONFIRM_MS));
            Text.send(player, prefix, msg("feature-confirm").replace("%warp%", warp.name()).replace("%cost%", ItemCosts.pretty(featuredCosts)));
            return;
        }

        if (chargesCost) {
            if (!ItemCosts.has(player, featuredCosts)) {
                Text.send(player, prefix, msg("featured-cost-missing").replace("%cost%", ItemCosts.pretty(featuredCosts)));
                return;
            }
            ItemCosts.take(player, featuredCosts);
            Text.send(player, prefix, msg("featured-cost-taken").replace("%cost%", ItemCosts.pretty(featuredCosts)));
        }

        pendingFeatures.remove(player.getUniqueId());
        boolean added = plugin.featured().add(warp.name(), maxSlots);
        Text.send(player, prefix, (added ? msg("featured-set") : msg("featured-already")).replace("%warp%", warp.name()));
    }

    private void warp(CommandSender sender, String name) {
        String prefix = msg("prefix");
        if (!(sender instanceof Player player)) { Text.send(sender, prefix, msg("player-only")); return; }
        if (!player.hasPermission("playerwarps.use")) { Text.send(player, prefix, msg("no-permission")); return; }
        Optional<PlayerWarp> optional = plugin.storage().getWarp(name);
        if (optional.isEmpty()) { Text.send(player, prefix, msg("warp-not-found")); return; }
        plugin.teleportListener().startTeleport(player, optional.get());
    }

    private void sendHelp(CommandSender sender, String label, String[] args) {
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
                page = 1;
            }
        }

        if (page <= 1) {
            sender.sendMessage(Text.color("&8&m---------------- &6Player Warps Help &7(1/2) &8&m----------------"));
            sender.sendMessage(Text.color("&e/" + label + " &7- Open the public warps GUI."));
            sender.sendMessage(Text.color("&e/" + label + " <name> &7- Teleport to a warp."));
            sender.sendMessage(Text.color("&e/" + label + " set <name> &7- Create a warp at your location."));
            sender.sendMessage(Text.color("&e/" + label + " delete <name> &7- Delete one of your warps."));
            sender.sendMessage(Text.color("&e/" + label + " rename <old> <new> &7- Rename one of your warps."));
            sender.sendMessage(Text.color("&e/" + label + " seticon <name> &7- Set a warp icon from your hand."));
            sender.sendMessage(Text.color("&8Example: &e/" + label + " rename My-Warp &#ff66ccNew Spawn"));
            sender.sendMessage(Text.color("&7Next page: &e/" + label + " help 2"));
            return;
        }

        sender.sendMessage(Text.color("&8&m---------------- &6Player Warps &8&m----------------"));
        sender.sendMessage(Text.color("&e/" + label + " desc <warp> add <message> &7- Add a description line."));
        sender.sendMessage(Text.color("&e/" + label + " desc <warp> set <line#> <message> &7- Set a description line."));
        sender.sendMessage(Text.color("&8Example: &e/" + label + " desc Spawn add &7Public farm and shops"));
        sender.sendMessage(Text.color("&8Example: &e/" + label + " desc Spawn set 2 &bNow with crates"));
        sender.sendMessage(Text.color("&e/" + label + " favorite <name> &7- Favorite or unfavorite a warp."));
        sender.sendMessage(Text.color("&e/" + label + " manage &7- Manage your warps."));
        sender.sendMessage(Text.color("&e/" + label + " list &7- Open the public warps GUI."));
        if (sender.hasPermission("playerwarps.feature") || sender.hasPermission("playerwarps.setfeatured") || sender.hasPermission("playerwarps.admin")) {
            sender.sendMessage(Text.color("&e/" + label + " feature <name> &7- Put a warp in featured slots."));
        }
        if (sender.hasPermission("playerwarps.admin")) {
            sender.sendMessage(Text.color("&e/" + label + " reload &7- Reload config, GUI, and warp data."));
        }
    }

    private boolean validName(String name) { return WarpNames.isValid(name, plugin.getConfig().getInt("settings.max-warp-name-length", 32)); }

    private boolean validDescriptionLine(String line) {
        if (line == null || line.isBlank()) return false;
        int maxLength = plugin.getConfig().getInt("settings.max-description-line-length", 80);
        String visible = Text.stripFormatting(line).trim();
        if (visible.isEmpty()) return false;
        return maxLength < 0 || visible.length() <= maxLength;
    }

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

    private DescriptionParts parseDescription(String[] args, Player player) {
        String first = args[1].toLowerCase();
        if (first.equals("add")) {
            String warpName = onlyOwnedWarpName(player);
            String description = WarpNames.join(args, 2);
            return warpName == null || description.isBlank() ? null : new DescriptionParts(warpName, DescriptionAction.ADD, -1, description);
        }
        if (first.equals("set")) {
            String warpName = onlyOwnedWarpName(player);
            if (warpName == null || args.length < 4) return null;
            int lineNumber = parsePositiveInt(args[2]);
            String description = WarpNames.join(args, 3);
            return lineNumber < 1 || description.isBlank() ? null : new DescriptionParts(warpName, DescriptionAction.SET, lineNumber, description);
        }

        for (int actionIndex = args.length - 1; actionIndex >= 2; actionIndex--) {
            String action = args[actionIndex].toLowerCase();
            if (!action.equals("add") && !action.equals("set")) continue;

            String warpName = WarpNames.join(args, 1, actionIndex);
            if (warpName.isBlank() || !plugin.storage().exists(warpName)) continue;

            if (action.equals("add")) {
                String description = WarpNames.join(args, actionIndex + 1);
                if (!description.isBlank()) return new DescriptionParts(warpName, DescriptionAction.ADD, -1, description);
                continue;
            }

            if (args.length <= actionIndex + 2) continue;
            int lineNumber = parsePositiveInt(args[actionIndex + 1]);
            String description = WarpNames.join(args, actionIndex + 2);
            if (lineNumber >= 1 && !description.isBlank()) {
                return new DescriptionParts(warpName, DescriptionAction.SET, lineNumber, description);
            }
        }

        // Legacy compatibility: /pwarp desc <warp> <description> replaces the
        // description with a single line, matching the old command behavior.
        for (int end = args.length - 1; end >= 2; end--) {
            String warpName = WarpNames.join(args, 1, end);
            String description = WarpNames.join(args, end, args.length);
            if (!warpName.isBlank() && !description.isBlank() && plugin.storage().exists(warpName)) {
                return new DescriptionParts(warpName, DescriptionAction.REPLACE, -1, description);
            }
        }
        return null;
    }

    private String onlyOwnedWarpName(Player player) {
        List<PlayerWarp> owned = plugin.storage().ownedBy(player.getUniqueId());
        if (owned.size() != 1) return null;
        return owned.get(0).name();
    }

    private int parsePositiveInt(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private boolean hasConfirmedFeature(Player player, PlayerWarp warp) {
        PendingFeature pending = pendingFeatures.get(player.getUniqueId());
        if (pending == null) return false;
        if (pending.expiresAt() < System.currentTimeMillis()) {
            pendingFeatures.remove(player.getUniqueId());
            return false;
        }
        return pending.normalizedWarpName().equals(WarpNames.normalize(warp.name()));
    }

    private record RenameParts(String oldName, String newName) {}
    private record DescriptionParts(String warpName, DescriptionAction action, int lineNumber, String description) {}
    private enum DescriptionAction { ADD, SET, REPLACE }
    private record PendingFeature(String normalizedWarpName, long expiresAt) {}
    private String msg(String key) { return plugin.getConfig().getString("messages." + key, defaultMessage(key)); }

    private String defaultMessage(String key) {
        return switch (key) {
            case "usage-feature" -> "&cUsage: /pwarp feature <name>";
            case "feature-confirm" -> "&eFeature &f%warp% &efor &f%cost%&e? Run &f/pwarp feature %warp% &eagain within 30 seconds to confirm.";
            case "description-added" -> "&aAdded description line &e%line% &afor &e%warp%&a.";
            case "description-line-set" -> "&aSet description line &e%line% &afor &e%warp%&a.";
            case "description-lines-limit" -> "&cThat warp already has the maximum of &e%max% &cdescription lines.";
            case "invalid-description-line" -> "&cDescription lines cannot be empty or longer than the configured limit.";
            case "invalid-description-line-number" -> "&cChoose a line number from &e1 &cto &e%max%&c.";
            default -> key;
        };
    }
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
            if (sender.hasPermission("playerwarps.feature") || sender.hasPermission("playerwarps.setfeatured") || sender.hasPermission("playerwarps.admin")) base.add("feature");
            if (sender.hasPermission("playerwarps.admin")) base.add("reload");
            for (PlayerWarp warp : plugin.storage().getWarps()) base.add(warp.name());
            return filter(base, args[0]);
        }
        if (args.length == 2 && List.of("delete", "rename", "seticon", "description", "desc", "setdesc", "feature", "setfeatured").contains(args[0].toLowerCase())) {
            List<String> names = new ArrayList<>();
            if (List.of("description", "desc", "setdesc").contains(args[0].toLowerCase())) {
                names.add("add");
                names.add("set");
            }
            for (PlayerWarp warp : plugin.storage().getWarps()) names.add(warp.name());
            return filter(names, args[1]);
        }
        if (List.of("description", "desc", "setdesc").contains(args[0].toLowerCase())) {
            if (args.length == 3 && plugin.storage().exists(args[1])) {
                return filter(List.of("add", "set"), args[2]);
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("set")) {
                return filter(lineNumbers(), args[2]);
            }
            if (args.length == 4 && args[2].equalsIgnoreCase("set")) {
                return filter(lineNumbers(), args[3]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> lineNumbers() {
        int maxLines = plugin.getConfig().getInt("settings.max-description-lines", 8);
        if (maxLines < 1) maxLines = 8;
        List<String> numbers = new ArrayList<>();
        for (int i = 1; i <= maxLines; i++) numbers.add(String.valueOf(i));
        return numbers;
    }

    private List<String> filter(List<String> options, String start) {
        List<String> out = new ArrayList<>();
        for (String option : options) if (option.toLowerCase().startsWith(start.toLowerCase())) out.add(option);
        return out;
    }
}
