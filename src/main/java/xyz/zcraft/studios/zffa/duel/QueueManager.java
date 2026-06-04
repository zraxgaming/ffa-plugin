package xyz.zcraft.studios.zffa.duel;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xyz.zcraft.studios.zffa.ZFfaPlugin;
import xyz.zcraft.studios.zffa.arena.Arena;
import xyz.zcraft.studios.zffa.kit.Kit;
import xyz.zcraft.studios.zffa.party.Party;
import xyz.zcraft.studios.zffa.platform.ScheduledTaskHandle;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class QueueManager {
    private final ZFfaPlugin plugin;
    private final MatchManager matches;
    private final Map<String, Deque<UUID>> queues = new HashMap<>();
    private final Map<String, Deque<PartyQueueEntry>> partyQueues = new HashMap<>();
    private final Map<UUID, String> queuedKit = new HashMap<>();
    private final Map<UUID, Long> queuedAt = new HashMap<>();
    private final Map<UUID, Long> pingNoticeAt = new HashMap<>();
    private ScheduledTaskHandle task;

    public QueueManager(ZFfaPlugin plugin, MatchManager matches) {
        this.plugin = plugin;
        this.matches = matches;
    }

    public void join(Player player, Kit kit) {
        join(player, kit, true);
    }

    public void join(Player player, Kit kit, boolean ranked) {
        if (matches.isInMatch(player.getUniqueId())) {
            plugin.messages().send(player, "queue.already-in-match", "<red>You are already in a match.");
            return;
        }
        if (plugin.proxyBridge().requestQueue(player, kit, ranked)) {
            String queueKey = queueKey(kit.id(), ranked);
            queuedKit.put(player.getUniqueId(), queueKey);
            queuedAt.put(player.getUniqueId(), System.currentTimeMillis());
            plugin.messages().send(player, "queue.joined", "<green>Queued for <white>{kit}</white> (<white>{type}</white>).", Map.of("kit", kit.id(), "type", ranked ? "ranked" : "unranked"));
            return;
        }
        if (!plugin.arenas().hasReadyArena(kit.id())) {
            plugin.messages().send(player, "queue.no-arenas", "<red>No arenas are set up for <white>{kit}</white>. Ask an admin to connect a ready arena to this kit.", Map.of("kit", kit.id()));
            return;
        }
        if (!plugin.arenas().hasFreeArena(kit.id())) {
            plugin.messages().send(player, "queue.arenas-busy", "<yellow>All <white>{kit}</white> arenas are currently in use. Try again in a moment.", Map.of("kit", kit.id()));
            return;
        }
        leave(player.getUniqueId());
        String queueKey = queueKey(kit.id(), ranked);
        queues.computeIfAbsent(queueKey, key -> new ArrayDeque<>()).offer(player.getUniqueId());
        queuedKit.put(player.getUniqueId(), queueKey);
        queuedAt.put(player.getUniqueId(), System.currentTimeMillis());
        plugin.messages().send(player, "queue.joined", "<green>Queued for <white>{kit}</white> (<white>{type}</white>).", Map.of("kit", kit.id(), "type", ranked ? "ranked" : "unranked"));
    }

    public void joinParty(Party party, Kit kit) {
        joinParty(party, kit, true);
    }

    public void joinParty(Party party, Kit kit, boolean ranked) {
        if (plugin.proxyBridge().requestPartyQueue(party, kit, ranked)) {
            String queueKey = queueKey(kit.id(), ranked);
            long now = System.currentTimeMillis();
            for (UUID member : party.members()) {
                queuedKit.put(member, queueKey);
                queuedAt.put(member, now);
            }
            plugin.parties().broadcast(party, replace("queue.party-joined", Map.of("kit", kit.id(), "type", ranked ? "ranked" : "unranked")));
            return;
        }
        if (!plugin.arenas().hasReadyArena(kit.id())) {
            plugin.parties().broadcast(party, replace("queue.party-no-arenas", Map.of("kit", kit.id())));
            return;
        }
        if (!plugin.arenas().hasFreeArena(kit.id())) {
            plugin.parties().broadcast(party, replace("queue.party-arenas-busy", Map.of("kit", kit.id())));
            return;
        }
        if (!plugin.parties().allOnlineAndFree(party)) {
            plugin.parties().broadcast(party, replace("queue.party-members-not-ready", Map.of()));
            return;
        }
        for (UUID member : party.members()) leave(member);
        String queueKey = queueKey(kit.id(), ranked);
        partyQueues.computeIfAbsent(queueKey, key -> new ArrayDeque<>()).offer(new PartyQueueEntry(party.members(), kit.id(), System.currentTimeMillis()));
        long now = System.currentTimeMillis();
        for (UUID member : party.members()) {
            queuedKit.put(member, queueKey);
            queuedAt.put(member, now);
        }
        plugin.parties().broadcast(party, replace("queue.party-joined", Map.of("kit", kit.id(), "type", ranked ? "ranked" : "unranked")));
    }

    public void leave(UUID uuid) {
        plugin.proxyBridge().requestQueueLeave(uuid);
        queues.values().forEach(queue -> queue.remove(uuid));
        partyQueues.values().forEach(queue -> queue.removeIf(entry -> entry.members().contains(uuid)));
        clearQueued(uuid);
    }

    private void clearQueued(UUID uuid) {
        queuedKit.remove(uuid);
        queuedAt.remove(uuid);
        pingNoticeAt.remove(uuid);
    }

    public int size(String kitId) {
        int total = 0;
        String prefix = kitId.toLowerCase(Locale.ROOT) + ":";
        for (Map.Entry<String, Deque<UUID>> entry : queues.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(kitId) || entry.getKey().startsWith(prefix)) {
                total += entry.getValue().size();
            }
        }
        return total;
    }

    public int totalPlayersQueued() {
        return queuedKit.size();
    }

    public String status(UUID uuid) {
        if (plugin.ffa().isInFfa(uuid)) return plugin.messages().get("queue.status.ffa", "FFA: {arena}").replace("{arena}", plugin.ffa().session(uuid).map(session -> session.arena().name()).orElse("FFA"));
        if (matches.isInMatch(uuid)) return plugin.messages().get("queue.status.in-match", "In Match");
        String queueKey = queuedKit.get(uuid);
        if (queueKey == null) return plugin.messages().get("queue.status.lobby", "Lobby");
        String[] parts = queueKey.split(":", 2);
        return plugin.messages().get("queue.status.queued", "Queued: {kit} ({type})")
                .replace("{kit}", parts[0])
                .replace("{type}", parts.length > 1 ? parts[1] : "ranked");
    }

    private String queueKey(String kitId, boolean ranked) {
        return kitId.toLowerCase(Locale.ROOT) + ":" + (ranked ? "ranked" : "unranked");
    }

    private String replace(String path, Map<String, String> placeholders) {
        String text = plugin.messages().get(path, "");
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return text;
    }

    private String queueKitId(String queueKey) {
        String[] parts = queueKey.split(":", 2);
        return parts.length > 0 ? parts[0] : queueKey;
    }

    private boolean isRanked(String queueKey) {
        String[] parts = queueKey.split(":", 2);
        return parts.length < 2 || "ranked".equalsIgnoreCase(parts[1]);
    }

    public void start() {
        if (task != null) task.cancel();
        task = plugin.scheduler().runTimer(this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        queues.clear();
        partyQueues.clear();
        queuedKit.clear();
        queuedAt.clear();
        pingNoticeAt.clear();
    }

    private void tick() {
        for (Map.Entry<String, Deque<UUID>> entry : queues.entrySet()) {
            Deque<UUID> queue = entry.getValue();
            while (queue.size() >= 2) {
                UUID firstId = queue.poll();
                UUID secondId = pollCompatibleOpponent(queue, firstId);
                if (firstId == null || secondId == null || firstId.equals(secondId)) break;
                Player first = Bukkit.getPlayer(firstId);
                Player second = Bukkit.getPlayer(secondId);
                String kitId = queueKitId(entry.getKey());
                Optional<Kit> kit = plugin.kits().get(kitId);
                Optional<Arena> arena = kit.isEmpty() ? Optional.empty() : plugin.arenas().firstAvailable(kit.get().id());
                if (first == null || second == null || kit.isEmpty()) {
                    arena.ifPresent(Arena::release);
                    clearQueued(firstId);
                    clearQueued(secondId);
                    continue;
                }
                if (arena.isEmpty()) {
                    queue.offer(firstId);
                    queue.offer(secondId);
                    break;
                }
                clearQueued(firstId);
                clearQueued(secondId);
                matches.start(first, second, kit.get(), arena.get(), isRanked(entry.getKey()));
            }
        }
        for (Map.Entry<String, Deque<PartyQueueEntry>> entry : partyQueues.entrySet()) {
            Deque<PartyQueueEntry> queue = entry.getValue();
            while (queue.size() >= 2) {
                PartyQueueEntry firstEntry = queue.poll();
                PartyQueueEntry secondEntry = queue.poll();
                if (firstEntry == null || secondEntry == null) continue;
                String kitId = queueKitId(entry.getKey());
                Optional<Kit> kit = plugin.kits().get(kitId);
                Optional<Arena> arena = kit.isEmpty() ? Optional.empty() : plugin.arenas().firstAvailable(kit.get().id());
                if (kit.isEmpty()) {
                    arena.ifPresent(Arena::release);
                    continue;
                }
                if (arena.isEmpty()) {
                    queue.offer(firstEntry);
                    queue.offer(secondEntry);
                    break;
                }
                if (!onlineAndFree(firstEntry) || !onlineAndFree(secondEntry)) {
                    arena.get().release();
                    firstEntry.members().forEach(this::clearQueued);
                    secondEntry.members().forEach(this::clearQueued);
                    continue;
                }
                firstEntry.members().forEach(this::clearQueued);
                secondEntry.members().forEach(this::clearQueued);
                matches.startTeams(firstEntry.members(), secondEntry.members(), kit.get(), arena.get(), isRanked(entry.getKey()));
            }
        }
    }

    private UUID pollCompatibleOpponent(Deque<UUID> queue, UUID firstId) {
        if (firstId == null || queue.isEmpty()) return null;
        if (!plugin.getConfig().getBoolean("settings.queue.ping-range.enabled", true)) {
            return queue.poll();
        }
        Player first = Bukkit.getPlayer(firstId);
        if (first == null) return queue.poll();
        int maxDifference = Math.max(0, plugin.getConfig().getInt("settings.queue.ping-range.max-difference", 80));
        long bypassAfterMillis = Math.max(0L, plugin.getConfig().getLong("settings.queue.ping-range.bypass-after-seconds", 30L)) * 1000L;
        long firstQueuedAt = queuedAt.getOrDefault(firstId, System.currentTimeMillis());
        long now = System.currentTimeMillis();

        UUID fallback = null;
        int checked = queue.size();
        for (int i = 0; i < checked; i++) {
            UUID candidateId = queue.poll();
            if (candidateId == null) continue;
            Player candidate = Bukkit.getPlayer(candidateId);
            boolean compatible = candidate == null
                    || Math.abs(first.getPing() - candidate.getPing()) <= maxDifference
                    || now - firstQueuedAt >= bypassAfterMillis
                    || now - queuedAt.getOrDefault(candidateId, now) >= bypassAfterMillis;
            if (compatible && fallback == null) {
                fallback = candidateId;
            } else {
                queue.offer(candidateId);
            }
        }
        if (fallback == null) {
            queue.offerFirst(firstId);
            sendPingWaitNotice(first);
        }
        return fallback;
    }

    private void sendPingWaitNotice(Player player) {
        long now = System.currentTimeMillis();
        long last = pingNoticeAt.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 5000L) return;
        pingNoticeAt.put(player.getUniqueId(), now);
        String message = plugin.messages().get("queue.ping-range-waiting", "<yellow>Waiting for an opponent near your ping.");
        player.sendActionBar(plugin.messages().parse(message));
    }

    private boolean onlineAndFree(PartyQueueEntry entry) {
        for (UUID uuid : entry.members()) {
            if (Bukkit.getPlayer(uuid) == null || matches.isInMatch(uuid) || plugin.ffa().isInFfa(uuid)) return false;
        }
        return true;
    }

    private record PartyQueueEntry(HashSet<UUID> members, String kitId, long queuedAt) {
        private PartyQueueEntry(java.util.Set<UUID> members, String kitId, long queuedAt) {
            this(new HashSet<>(members), kitId, queuedAt);
        }
    }
}
