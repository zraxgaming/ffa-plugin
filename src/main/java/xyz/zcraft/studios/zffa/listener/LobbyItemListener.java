package xyz.zcraft.studios.zffa.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import xyz.zcraft.studios.zffa.ZFfaPlugin;
import xyz.zcraft.studios.zffa.gui.Keys;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LobbyItemListener implements Listener {
    private static final long INTERACT_COOLDOWN_MILLIS = 250L;

    private final ZFfaPlugin plugin;
    private final Map<UUID, Long> lastInteractMillis = new ConcurrentHashMap<>();

    public LobbyItemListener(ZFfaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        try {
            Action action = event.getAction();
            if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
            Player player = event.getPlayer();
            ItemStack item = event.getItem();
            if (item == null || !item.hasItemMeta()) return;
            
            var meta = item.getItemMeta();
            if (meta == null) {
                plugin.debug("ItemMeta is null in LobbyItemListener");
                return;
            }
            
            String menuAction = meta.getPersistentDataContainer().get(Keys.MENU_ACTION, PersistentDataType.STRING);
            if (menuAction == null || menuAction.equals("FILLER")) return;
            event.setCancelled(true);
            if (isCoolingDown(player)) return;
            plugin.gui().executeAction(player, menuAction);
        } catch (Exception e) {
            plugin.getLogger().warning("Error in LobbyItemListener: " + e.getMessage());
            plugin.debug("Lobby interaction error type: " + e.getClass().getName());
        }
    }

    private boolean isCoolingDown(Player player) {
        long now = System.currentTimeMillis();
        Long previous = lastInteractMillis.put(player.getUniqueId(), now);
        return previous != null && now - previous < INTERACT_COOLDOWN_MILLIS;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastInteractMillis.remove(event.getPlayer().getUniqueId());
    }
}
