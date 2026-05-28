package xyz.zcraft.studios.zffa.duel;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import xyz.zcraft.studios.zffa.ZFfaPlugin;
import xyz.zcraft.studios.zffa.arena.Arena;
import xyz.zcraft.studios.zffa.kit.Kit;
import xyz.zcraft.studios.zffa.party.Party;

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
    private BukkitTask task;

    public QueueManager(ZFfaPlugin plugin, MatchManager matches) {
        this.plugin = plugin;
        this.matches = matches;
    }

    public void join(Player player, Kit kit) {
        join(player, kit, true);
    }

    public void join(Player player, Kit kit, boolean ranked) {
        if (matches.isInMatch(player.getUniqueId())) {
            plugin.messages().send(player, "<red>You are already in a match.");
            return;
        }
        if (!plugin.arenas().hasReadyArena(kit.id())) {
            plugin.messages().send(player, "<red>No arenas are set up for <white>" + kit.id() + "</white>. Ask an admin to connect a ready arena to this kit.");
            return;
        }
        if (!plugin.arenas().hasFreeArena(kit.id())) {
            plugin.messages().send(player, "<yellow>All <white>" + kit.id() + "</white> arenas are currently in use. Try again in a moment.");
            return;
        }
        leave(player.getUniqueId());
        String queueKey = queueKey(kit.id(), ranked);
        queues.computeIfAbsent(queueKey, key -> new ArrayDeque<>()).offer(player.getUniqueId());
        queuedKit.put(player.getUniqueId(), queueKey);
        plugin.messages().send(player, "<green>Queued for " + kit.id() + " (" + (ranked ? "ranked" : "unranked") + ").</green>");
    }

    public void joinParty(Party party, Kit kit) {
        joinParty(party, kit, true);
    }

    public void joinParty(Party party, Kit kit, boolean ranked) {
        if (!plugin.arenas().hasReadyArena(kit.id())) {
            plugin.parties().broadcast(party, "<red>No arenas are set up for <white>" + kit.id() + "</white>.");
            return;
        }
        if (!plugin.arenas().hasFreeArena(kit.id())) {
            plugin.parties().broadcast(party, "<yellow>All <white>" + kit.id() + "</white> arenas are currently in use. Try again in a moment.");
            return;
        }
        if (!plugin.parties().allOnlineAndFree(party)) {
            plugin.parties().broadcast(party, "<red>Every party member must be online and out of matches/FFA.");
            return;
        }
        for (UUID member : party.members()) leave(member);
        String queueKey = queueKey(kit.id(), ranked);
        partyQueues.computeIfAbsent(queueKey, key -> new ArrayDeque<>()).offer(new PartyQueueEntry(party.members(), kit.id(), System.currentTimeMillis()));
        for (UUID member : party.members()) queuedKit.put(member, queueKey);
        plugin.parties().broadcast(party, "<green>Your party queued for <white>" + kit.id() + "</white> (" + (ranked ? "ranked" : "unranked") + ").");
    }

    public void leave(UUID uuid) {
        queues.values().forEach(queue -> queue.remove(uuid));
        partyQueues.values().forEach(queue -> queue.removeIf(entry -> entry.members().contains(uuid)));
        queuedKit.remove(uuid);
    }

    public int size(String kitId) {
        Deque<UUID> queue = queues.get(kitId);
        return queue == null ? 0 : queue.size();
    }

    public String status(UUID uuid) {
        if (plugin.ffa().isInFfa(uuid)) return plugin.ffa().session(uuid).map(session -> "FFA: " + session.arena().name()).orElse("FFA");
        if (matches.isInMatch(uuid)) return "In Match";
        String queueKey = queuedKit.get(uuid);
        if (queueKey == null) return "Lobby";
        String[] parts = queueKey.split(":", 2);
        return "Queued: " + parts[0] + " (" + (parts.length > 1 ? parts[1] : "ranked") + ")";
    }

    private String queueKey(String kitId, boolean ranked) {
        return kitId.toLowerCase(Locale.ROOT) + ":" + (ranked ? "ranked" : "unranked");
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    private void tick() {
        for (Map.Entry<String, Deque<UUID>> entry : queues.entrySet()) {
            Deque<UUID> queue = entry.getValue();
            while (queue.size() >= 2) {
                UUID firstId = queue.poll();
                UUID secondId = queue.poll();
                if (firstId == null || secondId == null || firstId.equals(secondId)) continue;
                Player first = Bukkit.getPlayer(firstId);
                Player second = Bukkit.getPlayer(secondId);
                Optional<Kit> kit = plugin.kits().get(entry.getKey());
                Optional<Arena> arena = kit.isEmpty() ? Optional.empty() : plugin.arenas().firstAvailable(kit.get().id());
                if (first == null || second == null || kit.isEmpty()) {
                    arena.ifPresent(Arena::release);
                    queuedKit.remove(firstId);
                    queuedKit.remove(secondId);
                    continue;
                }
                if (arena.isEmpty()) {
                    queue.offer(firstId);
                    queue.offer(secondId);
                    break;
                }
                matches.start(first, second, kit.get(), arena.get());
            }
        }
        for (Map.Entry<String, Deque<PartyQueueEntry>> entry : partyQueues.entrySet()) {
            Deque<PartyQueueEntry> queue = entry.getValue();
            while (queue.size() >= 2) {
                PartyQueueEntry firstEntry = queue.poll();
                PartyQueueEntry secondEntry = queue.poll();
                if (firstEntry == null || secondEntry == null) continue;
                Optional<Kit> kit = plugin.kits().get(entry.getKey());
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
                    firstEntry.members().forEach(queuedKit::remove);
                    secondEntry.members().forEach(queuedKit::remove);
                    continue;
                }
                matches.startTeams(firstEntry.members(), secondEntry.members(), kit.get(), arena.get());
            }
        }
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
