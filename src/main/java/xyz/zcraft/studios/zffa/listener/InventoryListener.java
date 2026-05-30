package xyz.zcraft.studios.zffa.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
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
        try {
            if (!(event.getInventory().getHolder() instanceof ZFfaGuiHolder holder)) return;
            if (!(event.getWhoClicked() instanceof Player player)) return;

            boolean clickedTop = event.getClickedInventory() == event.getView().getTopInventory();
            if (!clickedTop && !event.getClick().isShiftClick()) return;

            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || !item.hasItemMeta()) return;
            
            var meta = item.getItemMeta();
            if (meta == null) {
                plugin.debug("ItemMeta is null in InventoryListener");
                return;
            }
            
            String action = meta.getPersistentDataContainer().get(Keys.MENU_ACTION, PersistentDataType.STRING);
            if (holder.type() == GuiType.KIT_SELECTOR) {
                handleKitSelection(player, item, action);
                return;
            }
            if (action == null || action.isBlank() || action.equalsIgnoreCase("FILLER")) {
                player.closeInventory();
                return;
            }
            if (action != null) {
                if (action.equalsIgnoreCase("DUEL_PLAYER")) {
                    handleDuelPlayer(player, item);
                    return;
                }
                if (action.equalsIgnoreCase("OPEN_STATS_TARGET")) {
                    handleOpenStatsTarget(player, item);
                    return;
                }
            }
            player.closeInventory();
            plugin.gui().executeAction(player, action);
        } catch (Exception e) {
            plugin.getLogger().warning("Error in InventoryListener.onClick: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleKitSelection(Player player, ItemStack item, String action) {
        var meta = item.getItemMeta();
        if (meta == null) return;
        
        String kitId = meta.getPersistentDataContainer().get(Keys.KIT_ID, PersistentDataType.STRING);
        String targetName = meta.getPersistentDataContainer().get(Keys.TARGET_PLAYER, PersistentDataType.STRING);
            if (kitId == null) return;
        
        boolean ranked = action == null
                || action.equalsIgnoreCase("QUEUE_RANKED")
                || action.equalsIgnoreCase("DUEL_REQUEST_RANKED");
        boolean partyFfa = action != null && action.equalsIgnoreCase("QUEUE_PARTY_FFA");
        
        plugin.kits().get(kitId).ifPresentOrElse(kit -> {
            if (!player.hasPermission("zf.kit." + kit.id()) && !player.hasPermission("zf.kit.*")) {
                plugin.messages().send(player, "permissions.no", "<red>You do not have permission for that kit.");
                return;
            }
            player.closeInventory();
            if (targetName != null && (action != null && (action.equalsIgnoreCase("DUEL_REQUEST_RANKED") || action.equalsIgnoreCase("DUEL_REQUEST_UNRANKED")))) {
                Player target = Bukkit.getPlayerExact(targetName);
                if (target == null) {
                    plugin.messages().send(player, "<red>The target player is no longer online.");
                    return;
                }
                if (target.equals(player)) {
                    plugin.messages().send(player, "duel.self", "<red>You cannot duel yourself.");
                    return;
                }
                plugin.matches().sendDuelRequest(player, target.getName(), kit.id(), ranked);
                return;
            }
            Party party = plugin.parties().party(player.getUniqueId()).orElse(null);
            if (party != null && party.size() > 1) {
                if (!party.isLeader(player.getUniqueId())) {
                    plugin.messages().send(player, "<red>Only your party leader can queue the party.");
                    return;
                }
                if (partyFfa) {
                    var arena = plugin.arenas().firstAvailable(kit.id()).orElse(null);
                    if (arena == null || !arena.isFfaReady()) {
                        plugin.parties().broadcast(party, "<red>No available FFA arena for that kit right now.");
                        return;
                    }
                    plugin.parties().joinFfa(party, arena, kit);
                    return;
                }
                plugin.queues().joinParty(party, kit, ranked);
                return;
            }
            plugin.queues().join(player, kit, ranked);
        }, () -> {
            plugin.messages().send(player, "<red>Kit not found.");
            player.closeInventory();
        });
    }

    private void handleDuelPlayer(Player player, ItemStack item) {
        var meta = item.getItemMeta();
        if (meta == null) return;
        
        String targetName = meta.getPersistentDataContainer().get(Keys.TARGET_PLAYER, PersistentDataType.STRING);
        if (targetName != null) {
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                plugin.messages().send(player, "<red>The player is no longer online.");
            } else if (target.equals(player)) {
                plugin.messages().send(player, "duel.self", "<red>You cannot duel yourself.");
            } else {
                player.closeInventory();
                plugin.gui().openDuelKits(player, target, true);
            }
        }
    }

    private void handleOpenStatsTarget(Player player, ItemStack item) {
        var meta = item.getItemMeta();
        if (meta == null) return;
        
        String targetName = meta.getPersistentDataContainer().get(Keys.TARGET_PLAYER, PersistentDataType.STRING);
        if (targetName != null) {
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                plugin.messages().send(player, "<red>The player is no longer online.");
            } else {
                player.closeInventory();
                plugin.gui().openStats(target);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof ZFfaGuiHolder)) return;
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) {
            event.setCancelled(true);
        }
    }
}
