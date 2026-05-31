package com.oncearial.playerwarps.util;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class ItemCosts {
    private ItemCosts() {}

    public record Cost(Material material, int amount) {}

    public static List<Cost> readCosts(List<Map<?, ?>> raw) {
        List<Cost> costs = new ArrayList<>();
        for (Map<?, ?> map : raw) {
            Material mat = Material.matchMaterial(String.valueOf(map.get("material")));
            Object amountObject = map.get("amount");
            int amount = amountObject == null ? 1 : Integer.parseInt(String.valueOf(amountObject));
            if (mat != null && amount > 0) costs.add(new Cost(mat, amount));
        }
        return costs;
    }

    public static boolean has(Player player, List<Cost> costs) {
        for (Cost cost : costs) {
            if (!player.getInventory().containsAtLeast(new ItemStack(cost.material()), cost.amount())) return false;
        }
        return true;
    }

    public static void take(Player player, List<Cost> costs) {
        for (Cost cost : costs) {
            int remaining = cost.amount();
            ItemStack[] contents = player.getInventory().getContents();
            for (int i = 0; i < contents.length && remaining > 0; i++) {
                ItemStack item = contents[i];
                if (item == null || item.getType() != cost.material()) continue;
                int remove = Math.min(item.getAmount(), remaining);
                item.setAmount(item.getAmount() - remove);
                remaining -= remove;
                if (item.getAmount() <= 0) contents[i] = null;
            }
            player.getInventory().setContents(contents);
        }
        player.updateInventory();
    }

    public static String pretty(List<Cost> costs) {
        if (costs.isEmpty()) return "nothing";
        List<String> parts = new ArrayList<>();
        for (Cost cost : costs) parts.add(cost.amount() + "x " + cost.material().name().toLowerCase().replace('_', ' '));
        return String.join(", ", parts);
    }
}
