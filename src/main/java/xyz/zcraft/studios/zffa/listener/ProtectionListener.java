package xyz.zcraft.studios.zffa.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import xyz.zcraft.studios.zffa.ZFfaPlugin;

import java.util.List;
import java.util.Locale;

public final class ProtectionListener implements Listener {
    private final ZFfaPlugin plugin;

    public ProtectionListener(ZFfaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;
        if (!plugin.matches().isInMatch(player.getUniqueId()) || plugin.matches().isActive(player.getUniqueId())) return;
        event.setTo(event.getFrom());
    }

    @EventHandler
    public void onDamageByPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!plugin.getConfig().getBoolean("settings.protection.prevent-outside-pvp", true)) return;
        if (plugin.ffa().isInFfa(attacker.getUniqueId()) && plugin.ffa().isInFfa(victim.getUniqueId())) {
            if (!plugin.ffa().canDamage(attacker, victim)) event.setCancelled(true);
            return;
        }
        if (plugin.matches().sameMatch(attacker, victim) && plugin.matches().isActive(attacker.getUniqueId())) {
            plugin.matches().match(attacker.getUniqueId()).ifPresent(match -> {
                if (match.teamOf(attacker.getUniqueId()).equals(match.teamOf(victim.getUniqueId()))) {
                    event.setCancelled(true);
                }
            });
            return;
        }
        if (!plugin.matches().sameMatch(attacker, victim) || !plugin.matches().isActive(attacker.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (plugin.getConfig().getBoolean("settings.protection.block-break", true) && protectedPlayer(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (plugin.getConfig().getBoolean("settings.protection.block-place", true) && protectedPlayer(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (plugin.getConfig().getBoolean("settings.protection.item-drop", true) && protectedPlayer(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (plugin.getConfig().getBoolean("settings.protection.item-pickup", true) && protectedPlayer(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfig().getBoolean("settings.block-commands-in-match", true)) return;
        if (!plugin.matches().isInMatch(event.getPlayer().getUniqueId())) {
            if (plugin.ffa().isInFfa(event.getPlayer().getUniqueId())) {
                String command = event.getMessage().toLowerCase(Locale.ROOT);
                List<String> bypass = plugin.getConfig().getStringList("settings.blocked-match-commands-bypass")
                        .stream()
                        .map(value -> value.toLowerCase(Locale.ROOT))
                        .toList();
                for (String allowed : bypass) {
                    if (command.startsWith(allowed)) return;
                }
                event.setCancelled(true);
                plugin.messages().send(event.getPlayer(), "<red>You cannot use that command during FFA.");
            }
            return;
        }
        String command = event.getMessage().toLowerCase(Locale.ROOT);
        List<String> bypass = plugin.getConfig().getStringList("settings.blocked-match-commands-bypass")
                .stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList();
        for (String allowed : bypass) {
            if (command.startsWith(allowed)) return;
        }
        event.setCancelled(true);
        plugin.messages().send(event.getPlayer(), "<red>You cannot use that command during a match.");
    }

    private boolean protectedPlayer(Player player) {
        return plugin.matches().isInMatch(player.getUniqueId()) || plugin.ffa().isInFfa(player.getUniqueId());
    }
}
