package xyz.zcraft.studios.zffa.gui;

import org.bukkit.NamespacedKey;
import xyz.zcraft.studios.zffa.ZFfaPlugin;

public final class Keys {
    public static NamespacedKey KIT_ID;
    public static NamespacedKey MENU_ACTION;
    public static NamespacedKey TARGET_PLAYER;

    private Keys() {
    }

    public static void init(ZFfaPlugin plugin) {
        KIT_ID = new NamespacedKey(plugin, "kit_id");
        MENU_ACTION = new NamespacedKey(plugin, "menu_action");
        TARGET_PLAYER = new NamespacedKey(plugin, "target_player");
    }
}
