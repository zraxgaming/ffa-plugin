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

    public void join(Player player, Arena arena, Kit kit) {
        if (!arena.enabled() || !arena.isFfaReady()) {
            plugin.messages().send(player, "<red>That FFA arena has no FFA spawns or is disabled.");
            return;
        }
        if (!arena.supportsKit(kit.id())) {
            plugin.messages().send(player, "<red>That kit is not allowed in this arena.");
            return;
        }
        if (arena.vip() && !player.hasPermission("zf.viparena")) {
            plugin.messages().send(player, "<red>This is a VIP arena.");
            return;
        }
        plugin.queues().leave(player.getUniqueId());
        plugin.matches().forfeit(player, "Joined FFA");
        sessions.put(player.getUniqueId(), new FfaSession(arena, kit));
        kit.apply(player);
        teleportRandom(player, arena);
        plugin.messages().send(player, "<green>Joined FFA arena <white>" + arena.name() + "</white> with kit <white>" + kit.id() + "</white>.");
    }

    public void leave(Player player) {
        FfaSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        Location lobby = plugin.arenas().lobby();
        if (lobby != null) player.teleportAsync(lobby).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> plugin.gui().giveLobbyItems(player)));
        plugin.messages().send(player, "<yellow>You left FFA.");
    }

    public void handleKill(Player victim, Player killer) {
        FfaSession victimSession = sessions.get(victim.getUniqueId());
        if (victimSession == null) return;
        PlayerProfile victimProfile = plugin.profiles().getOrCreate(victim);
        victimProfile.applyDeath();
        if (killer != null && sessions.containsKey(killer.getUniqueId()) && !killer.getUniqueId().equals(victim.getUniqueId())) {
            PlayerProfile killerProfile = plugin.profiles().getOrCreate(killer);
            killerProfile.applyKill();
            rewardKiller(killer, victim);
            double heartsLeft = Math.round((killer.getHealth() / 2.0D) * 10.0D) / 10.0D;
            killer.sendActionBar(plugin.messages().parse("<green>You killed <red>" + victim.getName() + "</red>. You gained <red>" + plugin.getConfig().getDouble("settings.ffa.kill-heal-hearts", 20.0D) + "</red> hearts."));
            victim.sendActionBar(plugin.messages().parse("<red>You were killed by <gold>" + killer.getName() + "</gold>. They had <gold>" + heartsLeft + "</gold> hearts left."));
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!sessions.containsKey(victim.getUniqueId())) return;
            victimSession.kit().apply(victim);
            teleportRandom(victim, victimSession.arena());
        }, 2L);
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

    public void shutdown() {
        sessions.clear();
    }

    private void teleportRandom(Player player, Arena arena) {
        if (arena.ffaSpawns().isEmpty()) return;
        int index = ThreadLocalRandom.current().nextInt(arena.ffaSpawns().size());
        player.teleportAsync(arena.ffaSpawns().get(index));
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
            plugin.messages().send(victim, "<green><gold>" + attacker.getName() + "</gold> wants to fight you. Hit them back to start.");
            plugin.messages().send(attacker, "<yellow>Fight request sent to <white>" + victim.getName() + "</white>.");
        }
        return false;
    }

    private boolean sameArena(Player first, Player second) {
        FfaSession firstSession = sessions.get(first.getUniqueId());
        FfaSession secondSession = sessions.get(second.getUniqueId());
        return firstSession != null && secondSession != null && firstSession.arena().name().equals(secondSession.arena().name());
    }

    private void rewardKiller(Player killer, Player victim) {
        double healHearts = plugin.getConfig().getDouble("settings.ffa.kill-heal-hearts", 20.0D);
        killer.setHealth(Math.min(killer.getMaxHealth(), killer.getHealth() + healHearts * 2.0D));
        if (plugin.getConfig().getBoolean("settings.ffa.refill-hunger-on-kill", true)) {
            killer.setFoodLevel(20);
            killer.setSaturation(20F);
        }
    }
}
