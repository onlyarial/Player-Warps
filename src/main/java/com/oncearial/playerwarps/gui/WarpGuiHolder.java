package com.oncearial.playerwarps.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class WarpGuiHolder implements InventoryHolder {
    public enum Type { BROWSE, MANAGE }

    private final Type type;
    private final int page;

    public WarpGuiHolder(Type type, int page) {
        this.type = type;
        this.page = page;
    }

    public Type type() { return type; }
    public int page() { return page; }

    @Override
    public Inventory getInventory() { return null; }
}
