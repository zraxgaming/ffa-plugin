package xyz.zcraft.studios.zffa.ffa;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import xyz.zcraft.studios.zffa.ZFfaPlugin;
import xyz.zcraft.studios.zffa.arena.Arena;
import xyz.zcraft.studios.zffa.kit.Kit;
import xyz.zcraft.studios.zffa.profile.PlayerProfile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class FfaManager {
    private final ZFfaPlugin plugin;
    private final Map<UUID, FfaSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, Long>> fightRequests = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastRequestNotice = new ConcurrentHashMap<>();

    public FfaManager(ZFfaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isInFfa(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public Optional<FfaSession> session(UUID uuid) {
        return Optional.ofNullable(sessions.get(uuid));
    }

    public int playerCount() {
        return sessions.size();
    }

    public int playerCount(Arena arena) {
        if (arena == null) return 0;
        return (int) sessions.values().stream()
                .filter(session -> session.arena().name().equalsIgnoreCase(arena.name()))
                .count();
    }

    public void join(Player player, Arena arena, Kit kit) {
        if (!arena.enabled() || !arena.isFfaReady()) {
            plugin.messages().send(player, "ffa.arena-not-ready", "<red>That FFA arena has no FFA spawns or is disabled.");
            return;
        }
        if (!arena.supportsKit(kit.id())) {
            plugin.messages().send(player, "ffa.kit-not-allowed", "<red>That kit is not allowed in this arena.");
            return;
        }
        if (arena.vip() && !player.hasPermission("zf.viparena")) {
            plugin.messages().send(player, "ffa.vip-arena", "<red>This is a VIP arena.");
            return;
        }
        plugin.queues().leave(player.getUniqueId());
        plugin.matches().forfeit(player, "Joined FFA");
        sessions.put(player.getUniqueId(), new FfaSession(arena, kit));
        kit.apply(player);
        teleportRandom(player, arena);
        plugin.messages().send(player, "ffa.joined", "<green>Joined FFA arena <white>{arena}</white> with kit <white>{kit}</white>.", Map.of("arena", arena.name(), "kit", kit.id()));
    }

    public void leave(Player player) {
        FfaSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;
        clearFightRequests(player.getUniqueId());
        plugin.protection().markPlayerExitedMatch(player.getUniqueId());
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setHealth(Math.min(player.getMaxHealth(), 20.0D));
        player.setFoodLevel(20);
        player.setSaturation(20F);
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.setWalkSpeed(0.2F);
        Location lobby = plugin.arenas().lobby();
        if (lobby != null) {
            plugin.scheduler().teleport(player, lobby).thenRun(() -> plugin.scheduler().run(() -> plugin.gui().giveLobbyItems(player)));
        }
        plugin.messages().send(player, "ffa.left", "<yellow>You left FFA.");
    }

    public void remove(UUID uuid) {
        if (sessions.remove(uuid) != null) {
            clearFightRequests(uuid);
            plugin.protection().markPlayerExitedMatch(uuid);
        }
    }

    public void handleKill(Player victim, Player killer) {
        FfaSession victimSession = sessions.get(victim.getUniqueId());
        if (victimSession == null) return;
        PlayerProfile victimProfile = plugin.profiles().getOrCreate(victim);
        victimProfile.applyDeath();
        applyFfaStreakLoss(victim, victimProfile);
        Map<String, String> deathPlaceholders = new java.util.HashMap<>();
        deathPlaceholders.put("victim", victim.getName());
        deathPlaceholders.put("arena", victimSession.arena().name());
        deathPlaceholders.put("kit", victimSession.kit().id());
        if (killer != null && sessions.containsKey(killer.getUniqueId()) && !killer.getUniqueId().equals(victim.getUniqueId())) {
            PlayerProfile killerProfile = plugin.profiles().getOrCreate(killer);
            killerProfile.applyKill();
            killerProfile.setStreak(killerProfile.streak() + 1);
            rewardKiller(killer);
            double heartsLeft = Math.round((killer.getHealth() / 2.0D) * 10.0D) / 10.0D;
            deathPlaceholders.put("killer", killer.getName());
            deathPlaceholders.put("killer_health", String.valueOf(heartsLeft));
            deathPlaceholders.put("killer_streak", String.valueOf(killerProfile.streak()));
            killer.sendActionBar(plugin.messages().parse("<green>You killed <red>" + victim.getName() + "</red>. You gained <red>" + plugin.getConfig().getDouble("settings.ffa.kill-heal-hearts", 20.0D) + "</red> hearts."));
            victim.sendActionBar(plugin.messages().parse("<red>You were killed by <gold>" + killer.getName() + "</gold>. They had <gold>" + heartsLeft + "</gold> hearts left."));
        } else {
            deathPlaceholders.put("killer", "environment");
            deathPlaceholders.put("killer_health", "0");
            deathPlaceholders.put("killer_streak", "0");
        }
        sendDeathMessage(victim, killer, deathPlaceholders);
        plugin.scheduler().runLater(() -> {
            if (!sessions.containsKey(victim.getUniqueId())) return;
            victimSession.kit().apply(victim);
            teleportRandom(victim, victimSession.arena());
        }, 2L);
        plugin.gui().refreshLeaderboardsNow();
    }

    private void applyFfaStreakLoss(Player victim, PlayerProfile profile) {
        profile.resetStreak();
    }

    public Optional<Arena> defaultArena() {
        return plugin.arenas().all().stream().filter(arena -> arena.isFfaReady() && !arena.vip()).min(Comparator.comparing(Arena::name));
    }

    public Optional<Arena> defaultVipArena() {
        return plugin.arenas().all().stream().filter(arena -> arena.isFfaReady() && arena.vip()).min(Comparator.comparing(Arena::name));
    }

    public Optional<Kit> defaultKit(Arena arena) {
        ArrayList<Kit> kits = new ArrayList<>(plugin.kits().all());
        return kits.stream().filter(kit -> arena.supportsKit(kit.id())).findFirst();
    }

    public void joinEvent(Player player) {
        if (!plugin.getConfig().getBoolean("settings.event.enabled", false)) {
            plugin.messages().send(player, "ffa.event-disabled", "<red>The event queue is not enabled.");
            return;
        }
        String arenaName = plugin.getConfig().getString("settings.event.arena", "").trim();
        String kitName = plugin.getConfig().getString("settings.event.kit", "").trim();
        if (arenaName.isEmpty() || kitName.isEmpty()) {
            plugin.messages().send(player, "ffa.event-missing-config", "<red>Event arena or kit is not configured. Ask an admin to set them.");
            return;
        }
        Arena arena = plugin.arenas().get(arenaName).orElse(null);
        if (arena == null || !arena.isFfaReady()) {
            plugin.messages().send(player, "ffa.event-arena-unavailable", "<red>The configured event arena is not available.");
            return;
        }
        Kit kit = plugin.kits().get(kitName).orElse(null);
        if (kit == null) {
            plugin.messages().send(player, "ffa.event-kit-unavailable", "<red>The configured event kit is not available.");
            return;
        }
        if (!arena.supportsKit(kit.id())) {
            plugin.messages().send(player, "ffa.event-kit-invalid", "<red>The event kit is not valid for the selected arena.");
            return;
        }
        join(player, arena, kit);
    }

    public void shutdown() {
        sessions.clear();
        fightRequests.clear();
        lastRequestNotice.clear();
    }

    private void teleportRandom(Player player, Arena arena) {
        if (arena.ffaSpawns().isEmpty()) return;
        int index = ThreadLocalRandom.current().nextInt(arena.ffaSpawns().size());
        plugin.scheduler().teleport(player, arena.ffaSpawns().get(index));
    }

    public boolean canDamage(Player attacker, Player victim) {
        if (!plugin.getConfig().getBoolean("settings.ffa.require-mutual-hit", true)) return true;
        if (!sameArena(attacker, victim)) return false;
        long now = System.currentTimeMillis();
        long expire = plugin.getConfig().getLong("settings.ffa.fight-request-expire-seconds", 10L) * 1000L;
        Long reverse = fightRequests.getOrDefault(victim.getUniqueId(), Map.of()).get(attacker.getUniqueId());
        if (reverse != null && now - reverse <= expire) return true;
        fightRequests.computeIfAbsent(attacker.getUniqueId(), ignored -> new ConcurrentHashMap<>()).put(victim.getUniqueId(), now);
        Long last = lastRequestNotice.get(victim.getUniqueId());
        if (last == null || now - last >= expire) {
            lastRequestNotice.put(victim.getUniqueId(), now);
            plugin.messages().send(victim, "ffa.hit-request", "<green><gold>{attacker}</gold> wants to fight you. Hit them back to start.", Map.of("attacker", attacker.getName()));
            plugin.messages().send(attacker, "ffa.hit-request-sent", "<yellow>Fight request sent to <white>{victim}</white>.", Map.of("victim", victim.getName()));
        }
        return false;
    }

    private boolean sameArena(Player first, Player second) {
        FfaSession firstSession = sessions.get(first.getUniqueId());
        FfaSession secondSession = sessions.get(second.getUniqueId());
        return firstSession != null && secondSession != null && firstSession.arena().name().equals(secondSession.arena().name());
    }

    private void clearFightRequests(UUID uuid) {
        fightRequests.remove(uuid);
        fightRequests.values().forEach(requests -> requests.remove(uuid));
        lastRequestNotice.remove(uuid);
    }

    private void sendDeathMessage(Player victim, Player killer, Map<String, String> placeholders) {
        if (!plugin.getConfig().getBoolean("settings.ffa.death-messages.enabled", true)) return;
        String path = killer == null ? "ffa.death.environment" : "ffa.death.player";
        String fallback = killer == null
                ? "<red>{victim}</red> died in <white>{arena}</white>."
                : "<red>{victim}</red> was killed by <gold>{killer}</gold> in <white>{arena}</white>.";
        String message = plugin.messages().get(path, fallback, placeholders);
        Component component = plugin.messages().parse(message);
        if (plugin.getConfig().getBoolean("settings.ffa.death-messages.broadcast", true)) {
            Bukkit.broadcast(component);
        }
        if (plugin.getConfig().getBoolean("settings.ffa.death-messages.actionbar", true)) {
            victim.sendActionBar(component);
            if (killer != null) killer.sendActionBar(component);
        }
    }

    private void rewardKiller(Player killer) {
        double healHearts = plugin.getConfig().getDouble("settings.ffa.kill-heal-hearts", 20.0D);
        killer.setHealth(Math.min(killer.getMaxHealth(), killer.getHealth() + healHearts * 2.0D));
        if (plugin.getConfig().getBoolean("settings.ffa.refill-hunger-on-kill", true)) {
            killer.setFoodLevel(20);
            killer.setSaturation(20F);
        }
    }
}
