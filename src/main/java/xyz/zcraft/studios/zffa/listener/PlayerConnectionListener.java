package xyz.zcraft.studios.zffa.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xyz.zcraft.studios.zffa.ZFfaPlugin;

public final class PlayerConnectionListener implements Listener {
    private final ZFfaPlugin plugin;

    public PlayerConnectionListener(ZFfaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.profiles().load(event.getPlayer());
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                plugin.gui().giveLobbyItems(event.getPlayer());
            }
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.queues().leave(event.getPlayer().getUniqueId());
        plugin.matches().forfeit(event.getPlayer(), "Opponent disconnected");
        plugin.ffa().leave(event.getPlayer());
        plugin.profiles().save(event.getPlayer(), true);
    }
}
