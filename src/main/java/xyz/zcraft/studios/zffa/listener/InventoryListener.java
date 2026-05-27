package xyz.zcraft.studios.zffa.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import xyz.zcraft.studios.zffa.ZFfaPlugin;
import xyz.zcraft.studios.zffa.gui.GuiType;
import xyz.zcraft.studios.zffa.gui.Keys;
import xyz.zcraft.studios.zffa.gui.ZFfaGuiHolder;
import xyz.zcraft.studios.zffa.party.Party;

public final class InventoryListener implements Listener {
    private final ZFfaPlugin plugin;

    public InventoryListener(ZFfaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ZFfaGuiHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        if (holder.type() == GuiType.KIT_SELECTOR) {
            String kitId = item.getItemMeta().getPersistentDataContainer().get(Keys.KIT_ID, PersistentDataType.STRING);
            if (kitId == null) return;
            plugin.kits().get(kitId).ifPresent(kit -> {
                if (!player.hasPermission("zf.kit." + kit.id()) && !player.hasPermission("zf.kit.*")) {
                    plugin.messages().send(player, "<red>You do not have permission for that kit.");
                    return;
                }
                player.closeInventory();
                Party party = plugin.parties().party(player.getUniqueId()).orElse(null);
                if (party != null && party.size() > 1) {
                    if (!party.isLeader(player.getUniqueId())) {
                        plugin.messages().send(player, "<red>Only your party leader can queue the party.");
                        return;
                    }
                    plugin.queues().joinParty(party, kit);
                    return;
                }
                plugin.queues().join(player, kit);
            });
        }
    }
}
