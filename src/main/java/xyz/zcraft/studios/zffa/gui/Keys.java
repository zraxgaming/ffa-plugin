package xyz.zcraft.studios.zffa.gui;

import org.bukkit.NamespacedKey;
import xyz.zcraft.studios.zffa.ZFfaPlugin;

public final class Keys {
    public static NamespacedKey KIT_ID;
    public static NamespacedKey MENU_ACTION;

    private Keys() {
    }

    public static void init(ZFfaPlugin plugin) {
        KIT_ID = new NamespacedKey(plugin, "kit_id");
        MENU_ACTION = new NamespacedKey(plugin, "menu_action");
    }
}
