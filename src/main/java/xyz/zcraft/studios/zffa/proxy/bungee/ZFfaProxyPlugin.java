package xyz.zcraft.studios.zffa.proxy.bungee;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class ZFfaProxyPlugin extends Plugin implements Listener {
    private static final String CHANNEL = "zffa:main";

    private final Map<String, ServerCapacity> capacities = new HashMap<>();
    private final Map<String, Queue<QueuedPlayer>> queues = new HashMap<>();

    @Override
    public void onEnable() {
        ProxyServer proxy = getProxy();
        proxy.registerChannel(CHANNEL);
        proxy.getPluginManager().registerListener(this, this);
        getLogger().info("Z-FFA proxy coordinator enabled on channel " + CHANNEL + ".");
    }

    @Override
    public void onDisable() {
        getProxy().unregisterChannel(CHANNEL);
        capacities.clear();
        queues.clear();
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!CHANNEL.equalsIgnoreCase(event.getTag())) return;
        event.setCancelled(true);
        if (!(event.getSender() instanceof Server server)) return;
        String message = new String(event.getData(), StandardCharsets.UTF_8);
        String[] parts = message.split("\\|", -1);
        if (parts.length == 0) return;
        switch (parts[0].toLowerCase(Locale.ROOT)) {
            case "capacity-bulk" -> updateCapacityBulk(server.getInfo(), parts);
            case "capacity" -> updateCapacity(server.getInfo(), parts);
            case "queue-join" -> queuePlayer(server.getInfo(), parts);
            case "party-queue-join" -> queueParty(server.getInfo(), parts);
            case "queue-leave" -> leaveQueue(parts);
            default -> getLogger().fine("Unknown backend message: " + message);
        }
    }

    private void updateCapacity(ServerInfo server, String[] parts) {
        if (parts.length < 6) return;
        String kit = key(parts[2]);
        capacities.put(parts[1] + ":" + kit, new ServerCapacity(server, kit, intValue(parts[4])));
        pump(kit);
    }

    private void updateCapacityBulk(ServerInfo server, String[] parts) {
        if (parts.length < 3) return;
        String serverId = parts[1];
        for (String entry : parts[2].split(";")) {
            String[] values = entry.split(",", -1);
            if (values.length < 3) continue;
            String kit = key(values[0]);
            capacities.put(serverId + ":" + kit, new ServerCapacity(server, kit, intValue(values[2])));
            pump(kit);
        }
    }

    private void queuePlayer(ServerInfo source, String[] parts) {
        if (parts.length < 6) return;
        UUID uuid = uuid(parts[2]).orElse(null);
        if (uuid == null) return;
        String kit = key(parts[4]);
        boolean ranked = Boolean.parseBoolean(parts[5]);
        removeQueued(uuid);
        queues.computeIfAbsent(queueKey(kit, ranked), ignored -> new ArrayDeque<>())
                .offer(new QueuedPlayer(uuid, kit, ranked, source));
        source.sendData(CHANNEL, ("queue-accepted|" + uuid).getBytes(StandardCharsets.UTF_8));
        pump(kit);
    }

    private void queueParty(ServerInfo source, String[] parts) {
        if (parts.length < 6) return;
        String kit = key(parts[4]);
        boolean ranked = Boolean.parseBoolean(parts[5]);
        for (String rawUuid : parts[3].split(",")) {
            if (rawUuid.isBlank()) continue;
            UUID uuid = uuid(rawUuid).orElse(null);
            if (uuid == null) continue;
            removeQueued(uuid);
            queues.computeIfAbsent(queueKey(kit, ranked), ignored -> new ArrayDeque<>())
                    .offer(new QueuedPlayer(uuid, kit, ranked, source));
        }
        source.sendData(CHANNEL, ("queue-accepted|" + parts[2]).getBytes(StandardCharsets.UTF_8));
        pump(kit);
    }

    private void leaveQueue(String[] parts) {
        if (parts.length < 3) return;
        uuid(parts[2]).ifPresent(this::removeQueued);
    }

    private void removeQueued(UUID uuid) {
        for (Queue<QueuedPlayer> queue : queues.values()) {
            queue.removeIf(player -> player.uuid().equals(uuid));
        }
    }

    private void pump(String kit) {
        for (boolean ranked : new boolean[]{true, false}) {
            Queue<QueuedPlayer> queue = queues.get(queueKey(kit, ranked));
            while (queue != null && queue.size() >= 2) {
                QueuedPlayer first = pollOnline(queue);
                QueuedPlayer second = pollOnline(queue);
                if (first == null || second == null) return;
                ServerInfo target = selectServer(kit).orElse(first.source());
                connectAndStart(first, second, target);
            }
        }
    }

    private QueuedPlayer pollOnline(Queue<QueuedPlayer> queue) {
        while (!queue.isEmpty()) {
            QueuedPlayer player = queue.poll();
            if (getProxy().getPlayer(player.uuid()) != null) return player;
        }
        return null;
    }

    private Optional<ServerInfo> selectServer(String kit) {
        return capacities.values().stream()
                .filter(capacity -> capacity.kit().equals(kit))
                .filter(capacity -> capacity.freeArenas() > 0)
                .max((left, right) -> Integer.compare(left.freeArenas(), right.freeArenas()))
                .map(ServerCapacity::server);
    }

    private void connectAndStart(QueuedPlayer first, QueuedPlayer second, ServerInfo target) {
        ProxiedPlayer firstPlayer = getProxy().getPlayer(first.uuid());
        ProxiedPlayer secondPlayer = getProxy().getPlayer(second.uuid());
        if (firstPlayer == null || secondPlayer == null) return;
        firstPlayer.connect(target);
        secondPlayer.connect(target);
        String payload = "start-duel|" + first.uuid() + "|" + second.uuid() + "|" + first.kit() + "|" + first.ranked() + "|";
        getProxy().getScheduler().schedule(this, () -> target.sendData(CHANNEL, payload.getBytes(StandardCharsets.UTF_8)), 1, TimeUnit.SECONDS);
    }

    private String queueKey(String kit, boolean ranked) {
        return kit + ":" + (ranked ? "ranked" : "unranked");
    }

    private String key(String value) {
        return Objects.toString(value, "").toLowerCase(Locale.ROOT);
    }

    private int intValue(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private Optional<UUID> uuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private record QueuedPlayer(UUID uuid, String kit, boolean ranked, ServerInfo source) {
    }

    private record ServerCapacity(ServerInfo server, String kit, int freeArenas) {
    }
}
