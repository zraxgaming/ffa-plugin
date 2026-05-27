package xyz.zcraft.studios.zffa.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import xyz.zcraft.studios.zffa.ZFfaPlugin;
import xyz.zcraft.studios.zffa.duel.DuelMatch;

public final class CombatListener implements Listener {
    private final ZFfaPlugin plugin;

    public CombatListener(ZFfaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player loser = event.getEntity();
        plugin.matches().match(loser.getUniqueId()).ifPresent(match -> {
            event.getDrops().clear();
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            plugin.matches().handleDeath(loser, "Death");
        });
        if (plugin.ffa().isInFfa(loser.getUniqueId())) {
            event.getDrops().clear();
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            plugin.ffa().handleKill(loser, loser.getKiller());
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        plugin.matches().match(player.getUniqueId()).ifPresent(match -> {
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID || event.getFinalDamage() >= player.getHealth()) {
                event.setCancelled(true);
                plugin.matches().handleDeath(player, event.getCause() == EntityDamageEvent.DamageCause.VOID ? "Void" : "Death");
            }
        });
        if (plugin.ffa().isInFfa(player.getUniqueId()) && (event.getCause() == EntityDamageEvent.DamageCause.VOID || event.getFinalDamage() >= player.getHealth())) {
            event.setCancelled(true);
            plugin.ffa().handleKill(player, lastDamager(event));
        }
    }

    private Player lastDamager(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent byEntity && byEntity.getDamager() instanceof Player player) return player;
        return null;
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        DuelMatch match = plugin.matches().match(player.getUniqueId()).orElse(null);
        if (match != null && !match.kit().allowHunger()) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    @EventHandler
    public void onRegen(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        DuelMatch match = plugin.matches().match(player.getUniqueId()).orElse(null);
        if (match != null && !match.kit().allowRegen()) event.setCancelled(true);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (plugin.arenas().lobby() != null) event.setRespawnLocation(plugin.arenas().lobby());
    }
}
