package xyz.zcraft.studios.zffa.proxy.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

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

@Plugin(
        id = "zffa",
        name = "Z-FFA",
        version = "1.3.9",
        authors = {"ZCraft Studios"},
        description = "Z-FFA Velocity coordinator for backend capacity and queue routing."
)
public final class ZFfaVelocityPlugin {
    private static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from("zffa:main");

    private final ProxyServer proxy;
    private final Logger logger;
    private final Map<String, ServerCapacity> capacities = new HashMap<>();
    private final Map<String, Queue<QueuedPlayer>> queues = new HashMap<>();

    @Inject
    public ZFfaVelocityPlugin(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        proxy.getChannelRegistrar().register(CHANNEL);
        logger.info("Z-FFA Velocity coordinator enabled on channel {}.", CHANNEL.getId());
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!CHANNEL.equals(event.getIdentifier())) return;
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection backend)) return;

        String message = new String(event.getData(), StandardCharsets.UTF_8);
        String[] parts = message.split("\\|", -1);
        if (parts.length == 0) return;
        switch (parts[0].toLowerCase(Locale.ROOT)) {
            case "capacity-bulk" -> updateCapacityBulk(backend.getServer(), parts);
            case "capacity" -> updateCapacity(backend.getServer(), parts);
            case "queue-join" -> queuePlayer(backend.getServer(), parts);
            case "party-queue-join" -> queueParty(backend.getServer(), parts);
            case "queue-leave" -> leaveQueue(parts);
            default -> logger.debug("Unknown Z-FFA backend message: {}", message);
        }
    }

    private void updateCapacity(RegisteredServer server, String[] parts) {
        if (parts.length < 6) return;
        String kit = key(parts[2]);
        capacities.put(parts[1] + ":" + kit, new ServerCapacity(server, kit, intValue(parts[4])));
        pump(kit);
    }

    private void updateCapacityBulk(RegisteredServer server, String[] parts) {
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

    private void queuePlayer(RegisteredServer source, String[] parts) {
        if (parts.length < 6) return;
        UUID uuid = uuid(parts[2]).orElse(null);
        if (uuid == null) return;
        String kit = key(parts[4]);
        boolean ranked = Boolean.parseBoolean(parts[5]);
        removeQueued(uuid);
        queues.computeIfAbsent(queueKey(kit, ranked), ignored -> new ArrayDeque<>())
                .offer(new QueuedPlayer(uuid, kit, ranked, source));
        source.sendPluginMessage(CHANNEL, ("queue-accepted|" + uuid).getBytes(StandardCharsets.UTF_8));
        pump(kit);
    }

    private void queueParty(RegisteredServer source, String[] parts) {
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
        source.sendPluginMessage(CHANNEL, ("queue-accepted|" + parts[2]).getBytes(StandardCharsets.UTF_8));
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
                RegisteredServer target = selectServer(kit).orElse(first.source());
                connectAndStart(first, second, target);
            }
        }
    }

    private QueuedPlayer pollOnline(Queue<QueuedPlayer> queue) {
        while (!queue.isEmpty()) {
            QueuedPlayer player = queue.poll();
            if (proxy.getPlayer(player.uuid()).isPresent()) return player;
        }
        return null;
    }

    private Optional<RegisteredServer> selectServer(String kit) {
        return capacities.values().stream()
                .filter(capacity -> capacity.kit().equals(kit))
                .filter(capacity -> capacity.freeArenas() > 0)
                .max((left, right) -> Integer.compare(left.freeArenas(), right.freeArenas()))
                .map(ServerCapacity::server);
    }

    private void connectAndStart(QueuedPlayer first, QueuedPlayer second, RegisteredServer target) {
        Optional<Player> firstPlayer = proxy.getPlayer(first.uuid());
        Optional<Player> secondPlayer = proxy.getPlayer(second.uuid());
        if (firstPlayer.isEmpty() || secondPlayer.isEmpty()) return;
        firstPlayer.get().createConnectionRequest(target).fireAndForget();
        secondPlayer.get().createConnectionRequest(target).fireAndForget();
        String payload = "start-duel|" + first.uuid() + "|" + second.uuid() + "|" + first.kit() + "|" + first.ranked() + "|";
        proxy.getScheduler()
                .buildTask(this, () -> target.sendPluginMessage(CHANNEL, payload.getBytes(StandardCharsets.UTF_8)))
                .delay(1, TimeUnit.SECONDS)
                .schedule();
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

    private record QueuedPlayer(UUID uuid, String kit, boolean ranked, RegisteredServer source) {
    }

    private record ServerCapacity(RegisteredServer server, String kit, int freeArenas) {
    }
}
