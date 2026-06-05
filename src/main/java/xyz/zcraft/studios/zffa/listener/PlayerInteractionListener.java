package xyz.zcraft.studios.zffa.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import xyz.zcraft.studios.zffa.ZFfaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerInteractionListener implements Listener {
    private static final long PLAYER_MENU_COOLDOWN_MILLIS = 500L;

    private final ZFfaPlugin plugin;
    private final Map<UUID, Long> lastPlayerMenuMillis = new ConcurrentHashMap<>();

    public PlayerInteractionListener(ZFfaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractAtEntityEvent event) {
        if (!plugin.getConfig().getBoolean("settings.player-menu.enabled", true)) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player target)) return;
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        event.setCancelled(true);
        if (isCoolingDown(player)) return;
        plugin.gui().openPlayerMenu(player, target);
    }

    private boolean isCoolingDown(Player player) {
        long now = System.currentTimeMillis();
        Long previous = lastPlayerMenuMillis.put(player.getUniqueId(), now);
        return previous != null && now - previous < PLAYER_MENU_COOLDOWN_MILLIS;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastPlayerMenuMillis.remove(event.getPlayer().getUniqueId());
    }
}


