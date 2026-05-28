package xyz.zcraft.studios.zffa.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import xyz.zcraft.studios.zffa.ZFfaPlugin;

public final class PlayerInteractionListener implements Listener {
    private final ZFfaPlugin plugin;

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
        plugin.gui().openPlayerMenu(player, target);
    }}


