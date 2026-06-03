package xyz.zcraft.studios.zffa.proxy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitTask;
import xyz.zcraft.studios.zffa.ZFfaPlugin;
import xyz.zcraft.studios.zffa.arena.Arena;
import xyz.zcraft.studios.zffa.kit.Kit;
import xyz.zcraft.studios.zffa.party.Party;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class BackendProxyBridge implements PluginMessageListener {
    private final ZFfaPlugin plugin;
    private BukkitTask capacityTask;
    private String channel;

    public BackendProxyBridge(ZFfaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!enabled()) return;
        channel = plugin.getConfig().getString("settings.proxy.channel", "zffa:main").toLowerCase(Locale.ROOT);
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, channel);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, channel, this);
        if (plugin.getConfig().getBoolean("settings.proxy.report-capacity", true)) {
            long seconds = Math.max(1L, plugin.getConfig().getLong("settings.proxy.report-interval-seconds", 5L));
            capacityTask = Bukkit.getScheduler().runTaskTimer(plugin, this::reportCapacity, 20L, seconds * 20L);
        }
        plugin.getLogger().info("Proxy-assisted backend mode enabled on channel " + channel + ".");
    }

    public void stop() {
        if (capacityTask != null) {
            capacityTask.cancel();
            capacityTask = null;
        }
        if (channel != null) {
            Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, channel);
            Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, channel, this);
        }
    }

    public boolean enabled() {
        return "backend".equalsIgnoreCase(plugin.getConfig().getString("settings.proxy.mode", "standalone"));
    }

    public boolean queueRoutingEnabled() {
        return enabled() && plugin.getConfig().getBoolean("settings.proxy.route-queues", false);
    }

    public boolean requestQueue(Player player, Kit kit, boolean ranked) {
        if (!queueRoutingEnabled()) return false;
        send(player, "queue-join|" + serverId() + "|" + player.getUniqueId() + "|" + player.getName() + "|" + kit.id() + "|" + ranked);
        return true;
    }

    public boolean requestPartyQueue(Party party, Kit kit, boolean ranked) {
        if (!queueRoutingEnabled()) return false;
        Player leader = Bukkit.getPlayer(party.leader());
        if (leader == null) return false;
        String members = party.members().stream().map(UUID::toString).collect(Collectors.joining(","));
        send(leader, "party-queue-join|" + serverId() + "|" + party.leader() + "|" + members + "|" + kit.id() + "|" + ranked);
        return true;
    }

    public void requestQueueLeave(UUID uuid) {
        if (!queueRoutingEnabled()) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            send(player, "queue-leave|" + serverId() + "|" + uuid);
        }
    }

    @Override
    public void onPluginMessageReceived(String receivedChannel, Player player, byte[] bytes) {
        if (!enabled() || !receivedChannel.equalsIgnoreCase(channel)) return;
        String message = new String(bytes, StandardCharsets.UTF_8);
        String[] parts = message.split("\\|");
        if (parts.length == 0) return;
        switch (parts[0].toLowerCase(Locale.ROOT)) {
            case "queue-accepted" -> notifyPlayer(parts, "<green>Proxy queue accepted. Waiting for a match.");
            case "queue-rejected" -> notifyPlayer(parts, parts.length >= 3 ? parts[2] : "<red>The proxy could not queue you.");
            case "queue-remove" -> {
                if (parts.length >= 2) {
                    plugin.queues().leave(UUID.fromString(parts[1]));
                }
            }
            case "start-duel" -> startProxyDuel(parts);
            default -> plugin.debug("Unknown proxy message: " + message);
        }
    }

    private void notifyPlayer(String[] parts, String fallback) {
        if (parts.length < 2) return;
        Player target = Bukkit.getPlayer(UUID.fromString(parts[1]));
        if (target != null) {
            plugin.messages().send(target, fallback);
        }
    }

    private void startProxyDuel(String[] parts) {
        if (parts.length < 5) return;
        Player first = Bukkit.getPlayer(UUID.fromString(parts[1]));
        Player second = Bukkit.getPlayer(UUID.fromString(parts[2]));
        if (first == null || second == null) return;
        Optional<Kit> kit = plugin.kits().get(parts[3]);
        if (kit.isEmpty()) return;
        Optional<Arena> arena = parts.length >= 6 && !parts[5].isBlank()
                ? plugin.arenas().get(parts[5]).filter(value -> value.claim(kit.get().id()))
                : plugin.arenas().firstAvailable(kit.get().id());
        arena.ifPresent(value -> plugin.matches().start(first, second, kit.get(), value, Boolean.parseBoolean(parts[4])));
    }

    private void reportCapacity() {
        Player carrier = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (carrier == null) return;
        for (Kit kit : plugin.kits().all()) {
            long ready = plugin.arenas().readyArenaCount(kit.id());
            long free = plugin.arenas().freeArenaCount(kit.id());
            int queued = plugin.queues().size(kit.id());
            send(carrier, "capacity|" + serverId() + "|" + kit.id() + "|" + ready + "|" + free + "|" + queued);
        }
    }

    private void send(Player player, String message) {
        if (channel == null || player == null) return;
        player.sendPluginMessage(plugin, channel, message.getBytes(StandardCharsets.UTF_8));
    }

    private String serverId() {
        return plugin.getConfig().getString("settings.proxy.server-id", "ffa-1");
    }
}
