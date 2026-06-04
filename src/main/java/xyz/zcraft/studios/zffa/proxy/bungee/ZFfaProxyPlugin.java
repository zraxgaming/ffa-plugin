package xyz.zcraft.studios.zffa.proxy.bungee;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class ZFfaProxyPlugin extends Plugin implements Listener {
    private static final String DEFAULT_CHANNEL = "zffa:main";
    private static final String CONFIG_FILE = "zffa-proxy.properties";

    private final Map<String, ServerCapacity> capacities = new HashMap<>();
    private final Map<String, Queue<QueuedPlayer>> queues = new HashMap<>();
    private ProxySettings settings = ProxySettings.defaults();
    private String channel = DEFAULT_CHANNEL;

    @Override
    public void onEnable() {
        settings = loadSettings();
        channel = settings.channel();
        if (channel.isBlank()) {
            getLogger().warning("Blank Z-FFA proxy channel; falling back to " + DEFAULT_CHANNEL + ".");
            settings = settings.withChannel(DEFAULT_CHANNEL);
            channel = DEFAULT_CHANNEL;
        }
        if (!settings.enabled()) {
            getLogger().info("Z-FFA proxy coordinator disabled in " + CONFIG_FILE + ".");
            return;
        }
        ProxyServer proxy = getProxy();
        proxy.registerChannel(channel);
        proxy.getPluginManager().registerListener(this, this);
        getLogger().info("Z-FFA proxy coordinator enabled on channel " + channel + ".");
    }

    @Override
    public void onDisable() {
        if (settings.enabled()) {
            getProxy().unregisterChannel(channel);
        }
        capacities.clear();
        queues.clear();
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!settings.enabled() || !channel.equalsIgnoreCase(event.getTag())) return;
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
        capacities.put(parts[1] + ":" + kit, new ServerCapacity(server, kit, intValue(parts[4]), System.currentTimeMillis()));
        pump(kit);
    }

    private void updateCapacityBulk(ServerInfo server, String[] parts) {
        if (parts.length < 3) return;
        String serverId = parts[1];
        for (String entry : parts[2].split(";")) {
            String[] values = entry.split(",", -1);
            if (values.length < 3) continue;
            String kit = key(values[0]);
            capacities.put(serverId + ":" + kit, new ServerCapacity(server, kit, intValue(values[2]), System.currentTimeMillis()));
            pump(kit);
        }
    }

    private void queuePlayer(ServerInfo source, String[] parts) {
        if (!settings.queueEnabled()) return;
        if (parts.length < 6) return;
        UUID uuid = uuid(parts[2]).orElse(null);
        if (uuid == null) return;
        String kit = key(parts[4]);
        boolean ranked = Boolean.parseBoolean(parts[5]);
        Queue<QueuedPlayer> queue = queues.computeIfAbsent(queueKey(kit, ranked), ignored -> new ArrayDeque<>());
        if (queue.size() >= settings.maxQueueSizePerKit()) {
            source.sendData(channel, ("queue-rejected|" + uuid + "|<red>That proxy queue is full.</red>").getBytes(StandardCharsets.UTF_8));
            return;
        }
        removeQueued(uuid);
        queue.offer(new QueuedPlayer(uuid, kit, ranked, source));
        source.sendData(channel, ("queue-accepted|" + uuid).getBytes(StandardCharsets.UTF_8));
        pump(kit);
    }

    private void queueParty(ServerInfo source, String[] parts) {
        if (!settings.queueEnabled()) return;
        if (parts.length < 6) return;
        String kit = key(parts[4]);
        boolean ranked = Boolean.parseBoolean(parts[5]);
        Queue<QueuedPlayer> queue = queues.computeIfAbsent(queueKey(kit, ranked), ignored -> new ArrayDeque<>());
        for (String rawUuid : parts[3].split(",")) {
            if (rawUuid.isBlank()) continue;
            UUID uuid = uuid(rawUuid).orElse(null);
            if (uuid == null) continue;
            if (queue.size() >= settings.maxQueueSizePerKit()) {
                source.sendData(channel, ("queue-rejected|" + uuid + "|<red>That proxy queue is full.</red>").getBytes(StandardCharsets.UTF_8));
                continue;
            }
            removeQueued(uuid);
            queue.offer(new QueuedPlayer(uuid, kit, ranked, source));
        }
        source.sendData(channel, ("queue-accepted|" + parts[2]).getBytes(StandardCharsets.UTF_8));
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
        pruneStaleCapacity();
        for (boolean ranked : new boolean[]{true, false}) {
            Queue<QueuedPlayer> queue = queues.get(queueKey(kit, ranked));
            while (queue != null && queue.size() >= 2) {
                Optional<ServerCapacity> selected = selectCapacity(kit);
                if (selected.isEmpty() && settings.requireFreeArena()) return;
                QueuedPlayer first = pollOnline(queue);
                QueuedPlayer second = pollOnline(queue);
                if (first == null || second == null) return;
                ServerInfo target = selected.map(ServerCapacity::server)
                        .orElseGet(() -> settings.fallbackToSource() ? first.source() : null);
                if (target == null) {
                    queue.offer(first);
                    queue.offer(second);
                    return;
                }
                reserveCapacity(target, kit);
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

    private Optional<ServerCapacity> selectCapacity(String kit) {
        long now = System.currentTimeMillis();
        return capacities.values().stream()
                .filter(capacity -> capacity.kit().equals(kit))
                .filter(capacity -> now - capacity.updatedAtMillis() <= settings.staleCapacityMillis())
                .filter(capacity -> capacity.freeArenas() >= settings.minFreeArenas())
                .max((left, right) -> Integer.compare(left.freeArenas(), right.freeArenas()));
    }

    private void reserveCapacity(ServerInfo target, String kit) {
        for (Map.Entry<String, ServerCapacity> entry : capacities.entrySet()) {
            ServerCapacity capacity = entry.getValue();
            if (!capacity.server().equals(target) || !capacity.kit().equals(kit) || capacity.freeArenas() <= 0) continue;
            entry.setValue(new ServerCapacity(target, kit, capacity.freeArenas() - 1, System.currentTimeMillis()));
            return;
        }
    }

    private void pruneStaleCapacity() {
        long now = System.currentTimeMillis();
        capacities.values().removeIf(capacity -> now - capacity.updatedAtMillis() > settings.staleCapacityMillis());
    }

    private void connectAndStart(QueuedPlayer first, QueuedPlayer second, ServerInfo target) {
        ProxiedPlayer firstPlayer = getProxy().getPlayer(first.uuid());
        ProxiedPlayer secondPlayer = getProxy().getPlayer(second.uuid());
        if (firstPlayer == null || secondPlayer == null) return;
        firstPlayer.connect(target);
        secondPlayer.connect(target);
        String payload = "start-duel|" + first.uuid() + "|" + second.uuid() + "|" + first.kit() + "|" + first.ranked() + "|";
        getProxy().getScheduler().schedule(this, () -> target.sendData(channel, payload.getBytes(StandardCharsets.UTF_8)), settings.connectDelayMillis(), TimeUnit.MILLISECONDS);
    }

    private ProxySettings loadSettings() {
        try {
            if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
                getLogger().warning("Unable to create " + getDataFolder().getAbsolutePath());
            }
            File file = new File(getDataFolder(), CONFIG_FILE);
            if (!file.exists()) {
                try (FileOutputStream output = new FileOutputStream(file)) {
                    output.write(defaultConfig().getBytes(StandardCharsets.UTF_8));
                }
            }
            Properties properties = new Properties();
            try (FileInputStream input = new FileInputStream(file)) {
                properties.load(input);
            }
            return new ProxySettings(
                    bool(properties, "enabled", true),
                    properties.getProperty("channel", DEFAULT_CHANNEL).trim().toLowerCase(Locale.ROOT),
                    bool(properties, "queue.enabled", true),
                    Math.max(2, integer(properties, "queue.max-size-per-kit", 500)),
                    bool(properties, "queue.require-free-arena", true),
                    Math.max(0, integer(properties, "queue.connect-delay-millis", 1000)),
                    Math.max(1, integer(properties, "capacity.stale-seconds", 15)) * 1000L,
                    Math.max(1, integer(properties, "server-selection.min-free-arenas", 1)),
                    bool(properties, "fallback-to-source", false)
            );
        } catch (IOException exception) {
            getLogger().warning("Unable to load " + CONFIG_FILE + "; using safe defaults: " + exception.getMessage());
            return ProxySettings.defaults();
        }
    }

    private String defaultConfig() {
        return """
                # Z-FFA proxy coordinator configuration.
                # This file is generated by the Bungee/Waterfall proxy jar.
                enabled=true
                channel=zffa:main
                queue.enabled=true
                queue.max-size-per-kit=500
                queue.require-free-arena=true
                queue.connect-delay-millis=1000
                capacity.stale-seconds=15
                server-selection.min-free-arenas=1
                fallback-to-source=false
                """;
    }

    private boolean bool(Properties properties, String key, boolean fallback) {
        return Boolean.parseBoolean(properties.getProperty(key, String.valueOf(fallback)));
    }

    private int integer(Properties properties, String key, int fallback) {
        return intValue(properties.getProperty(key, String.valueOf(fallback)));
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

    private record ServerCapacity(ServerInfo server, String kit, int freeArenas, long updatedAtMillis) {
    }

    private record ProxySettings(boolean enabled, String channel, boolean queueEnabled, int maxQueueSizePerKit,
                                 boolean requireFreeArena, int connectDelayMillis, long staleCapacityMillis,
                                 int minFreeArenas, boolean fallbackToSource) {
        private static ProxySettings defaults() {
            return new ProxySettings(true, DEFAULT_CHANNEL, true, 500, true, 1000, 15_000L, 1, false);
        }

        private ProxySettings withChannel(String value) {
            return new ProxySettings(enabled, value, queueEnabled, maxQueueSizePerKit, requireFreeArena,
                    connectDelayMillis, staleCapacityMillis, minFreeArenas, fallbackToSource);
        }
    }
}
