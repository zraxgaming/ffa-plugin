package xyz.zcraft.studios.zffa.duel;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import xyz.zcraft.studios.zffa.ZFfaPlugin;
import xyz.zcraft.studios.zffa.arena.Arena;
import xyz.zcraft.studios.zffa.kit.Kit;
import xyz.zcraft.studios.zffa.profile.EloCalculator;
import xyz.zcraft.studios.zffa.profile.PlayerProfile;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MatchManager {
    private final ZFfaPlugin plugin;
    private final Map<UUID, DuelMatch> matches = new ConcurrentHashMap<>();
    private final Map<DuelMatch, BukkitTask> timeoutTasks = new ConcurrentHashMap<>();

    public MatchManager(ZFfaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isInMatch(UUID uuid) {
        return matches.containsKey(uuid);
    }

    public boolean isActive(UUID uuid) {
        DuelMatch match = matches.get(uuid);
        return match != null && match.active();
    }

    public Optional<DuelMatch> match(UUID uuid) {
        return Optional.ofNullable(matches.get(uuid));
    }

    public boolean sameMatch(Player first, Player second) {
        DuelMatch match = matches.get(first.getUniqueId());
        return match != null && match.contains(second.getUniqueId());
    }

    public void start(Player first, Player second, Kit kit, Arena arena) {
        startTeams(Set.of(first.getUniqueId()), Set.of(second.getUniqueId()), kit, arena);
    }

    public void startTeams(Set<UUID> teamOne, Set<UUID> teamTwo, Kit kit, Arena arena) {
        DuelMatch match = new DuelMatch(teamOne, teamTwo, kit, arena, System.currentTimeMillis());
        for (UUID uuid : match.participants()) matches.put(uuid, match);
        for (UUID uuid : teamOne) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            prepare(player, kit);
            player.teleportAsync(arena.spawn1());
        }
        for (UUID uuid : teamTwo) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            prepare(player, kit);
            player.teleportAsync(arena.spawn2());
        }
        startCountdown(match);
        scheduleTimeout(match);
    }

    public void end(UUID winnerId, UUID loserId, String reason) {
        DuelMatch match = matches.get(winnerId);
        if (match == null || !match.markEnded()) return;
        for (UUID uuid : match.participants()) matches.remove(uuid);
        cancelTimeout(match);
        match.arena().release();

        Player winner = Bukkit.getPlayer(winnerId);
        Player loser = Bukkit.getPlayer(loserId);
        PlayerProfile winnerProfile = winner == null ? null : plugin.profiles().getOrCreate(winner);
        PlayerProfile loserProfile = loser == null ? null : plugin.profiles().getOrCreate(loser);
        awardTeams(match.teamOf(winnerId), match.teamOf(loserId), match);

        Location lobby = plugin.arenas().lobby();
        for (UUID uuid : match.participants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && lobby != null) resetAndTeleport(player, lobby);
            if (player != null && reason != null) plugin.messages().send(player, "<gray>Result: " + reason + "</gray>");
        }
    }

    public void draw(DuelMatch match, String reason) {
        if (match == null || !match.markEnded()) return;
        matches.remove(match.playerOne());
        matches.remove(match.playerTwo());
        cancelTimeout(match);
        match.arena().release();
        Location lobby = plugin.arenas().lobby();
        Player first = Bukkit.getPlayer(match.playerOne());
        Player second = Bukkit.getPlayer(match.playerTwo());
        if (first != null) {
            plugin.messages().send(first, "<yellow>Match ended: " + reason + "</yellow>");
            if (lobby != null) resetAndTeleport(first, lobby);
        }
        if (second != null) {
            plugin.messages().send(second, "<yellow>Match ended: " + reason + "</yellow>");
            if (lobby != null) resetAndTeleport(second, lobby);
        }
    }

    public void handleDeath(Player loser, String reason) {
        DuelMatch match = matches.get(loser.getUniqueId());
        if (match == null || match.isEliminated(loser.getUniqueId())) return;
        match.eliminate(loser.getUniqueId());
        loser.setInvulnerable(true);
        loser.getInventory().clear();
        loser.getInventory().setArmorContents(null);
        plugin.messages().send(loser, "<red>You were eliminated.");
        if (match.isTeamEliminated(match.teamOf(loser.getUniqueId()))) {
            UUID winner = match.opposingTeam(loser.getUniqueId()).iterator().next();
            end(winner, loser.getUniqueId(), reason);
        }
    }

    public void forfeit(Player player, String reason) {
        DuelMatch match = matches.get(player.getUniqueId());
        if (match == null) return;
        UUID opponent = match.opponent(player.getUniqueId());
        if (Bukkit.getPlayer(opponent) == null) draw(match, "Both players unavailable");
        else end(opponent, player.getUniqueId(), reason);
    }

    public void shutdown() {
        for (BukkitTask task : timeoutTasks.values()) task.cancel();
        timeoutTasks.clear();
        for (DuelMatch match : matches.values()) match.arena().release();
        matches.clear();
    }

    private void prepare(Player player, Kit kit) {
        player.closeInventory();
        player.setGameMode(GameMode.SURVIVAL);
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.setInvulnerable(true);
        kit.apply(player);
    }

    private void startCountdown(DuelMatch match) {
        int seconds = Math.max(0, plugin.getConfig().getInt("settings.match-countdown-seconds", 5));
        if (seconds == 0) {
            activate(match);
            return;
        }
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!match.participants().stream().allMatch(matches::containsKey)) {
                task.cancel();
                return;
            }
            long elapsed = (System.currentTimeMillis() - match.startedAtMillis()) / 1000L;
            int remaining = (int) (seconds - elapsed);
            if (remaining <= 0) {
                task.cancel();
                activate(match);
                return;
            }
            Component message = plugin.messages().parse("<yellow>Starting in <white>" + remaining + "</white>...");
            for (UUID uuid : match.participants()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) player.sendActionBar(message);
            }
        }, 0L, 20L);
    }

    private void activate(DuelMatch match) {
        match.activate();
        for (UUID uuid : match.participants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            player.setInvulnerable(false);
            plugin.messages().send(player, "<green>Fight!");
        }
    }

    private void awardTeams(Set<UUID> winners, Set<UUID> losers, DuelMatch match) {
        int winnerAverage = averageElo(winners);
        int loserAverage = averageElo(losers);
        int change = EloCalculator.calculateEloChange(winnerAverage, loserAverage);
        for (UUID uuid : winners) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            PlayerProfile profile = plugin.profiles().getOrCreate(player);
            profile.applyWin(change);
            plugin.messages().send(player, "<green>Your team won +" + change + " Elo.</green>");
        }
        for (UUID uuid : losers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            PlayerProfile profile = plugin.profiles().getOrCreate(player);
            profile.applyLoss(change);
            plugin.messages().send(player, "<red>Your team lost -" + change + " Elo.</red>");
        }
    }

    private int averageElo(Set<UUID> players) {
        int total = 0;
        int count = 0;
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            total += plugin.profiles().getOrCreate(player).elo();
            count++;
        }
        return count == 0 ? 1000 : total / count;
    }

    private void scheduleTimeout(DuelMatch match) {
        int minutes = plugin.getConfig().getInt("settings.match-timeout-minutes", 10);
        if (minutes <= 0) return;
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> draw(match, "Time limit reached"), minutes * 60L * 20L);
        timeoutTasks.put(match, task);
    }

    private void cancelTimeout(DuelMatch match) {
        BukkitTask task = timeoutTasks.remove(match);
        if (task != null) task.cancel();
    }

    private void resetAndTeleport(Player player, Location lobby) {
        player.setInvulnerable(false);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setWalkSpeed(0.2F);
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.setFoodLevel(20);
        player.setSaturation(20F);
        player.setHealth(Math.min(player.getMaxHealth(), 20.0D));
        player.teleportAsync(lobby).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> plugin.gui().giveLobbyItems(player)));
    }
}
