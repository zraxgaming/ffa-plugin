package xyz.zcraft.studios.zffa.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public record ZFfaGuiHolder(GuiType type) implements InventoryHolder {
    @Override
    public Inventory getInventory() {
        return null;
    }
}
