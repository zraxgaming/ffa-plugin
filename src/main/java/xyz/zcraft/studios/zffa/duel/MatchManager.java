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
    private static final long DUEL_INVITE_DURATION_MS = 30_000L;

    private final ZFfaPlugin plugin;
    private final Map<UUID, DuelMatch> matches = new ConcurrentHashMap<>();
    private final Map<DuelMatch, BukkitTask> timeoutTasks = new ConcurrentHashMap<>();
    private final Map<UUID, DuelInvite> pendingDuelInvites = new ConcurrentHashMap<>();

    private record DuelInvite(UUID challenger, UUID target, String kitId, long expiresAt) {
        boolean expired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

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

    public void sendDuelRequest(Player challenger, String targetName, String kitName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            plugin.messages().send(challenger, "duel.request.player-not-found", "<red>Player not found.");
            return;
        }
        if (target.equals(challenger)) {
            plugin.messages().send(challenger, "permissions.no", "<red>You cannot duel yourself.");
            return;
        }
        if (isInMatch(challenger.getUniqueId()) || isInMatch(target.getUniqueId())) {
            plugin.messages().send(challenger, "duel.request.already-in-match", "<red>Either you or that player is already in a match.");
            return;
        }
        DuelInvite existing = pendingDuelInvites.get(target.getUniqueId());
        if (existing != null && !existing.expired()) {
            plugin.messages().send(challenger, "<red>That player already has a pending duel request.");
            return;
        }

        Kit kit = null;
        if (kitName != null && !kitName.isBlank()) {
            kit = plugin.kits().get(kitName).orElse(null);
            if (kit == null) {
                plugin.messages().send(challenger, "duel.request.kit-not-found", "<red>Kit '{kit}' not found.", Map.of("kit", kitName));
                return;
            }
        }
        if (kit == null) {
            kit = plugin.kits().all().stream().findFirst().orElse(null);
            if (kit == null) {
                plugin.messages().send(challenger, "duel.request.no-kits", "<red>No kits are currently available.");
                return;
            }
        }

        Arena arena = plugin.arenas().firstAvailable(kit.id()).orElse(null);
        if (arena == null || !arena.isFfaReady()) {
            plugin.messages().send(challenger, "duel.request.arena-unavailable", "<red>No available arena supports that kit right now.");
            return;
        }

        pendingDuelInvites.put(target.getUniqueId(), new DuelInvite(challenger.getUniqueId(), target.getUniqueId(), kit.id(), System.currentTimeMillis() + DUEL_INVITE_DURATION_MS));
        plugin.messages().send(challenger, "duel.request.sent", "<green>Duel request sent to {target}!", Map.of("target", target.getName()));
        plugin.messages().send(target, "duel.request.received", "<yellow>{challenger} has challenged you to a duel.", Map.of("challenger", challenger.getName()));
        plugin.messages().send(target, "duel.request.instructions", "<gray>Type <white>/duel accept</white> or <white>/duel decline</white> within 30 seconds.");
    }

    public void acceptDuelRequest(Player target) {
        DuelInvite invite = pendingDuelInvites.remove(target.getUniqueId());
        if (invite == null || invite.expired()) {
            plugin.messages().send(target, "duel.request.no-active", "<red>There is no active duel request to accept.", Map.of("action", "accept"));
            return;
        }
        Player challenger = Bukkit.getPlayer(invite.challenger());
        if (challenger == null) {
            plugin.messages().send(target, "duel.request.not-online", "<red>The challenger is no longer online.");
            return;
        }
        if (isInMatch(challenger.getUniqueId()) || isInMatch(target.getUniqueId())) {
            plugin.messages().send(target, "duel.request.already-in-match", "<red>Either you or the challenger is already in a match.");
            return;
        }
        Kit kit = plugin.kits().get(invite.kitId()).orElse(null);
        if (kit == null) {
            plugin.messages().send(target, "duel.request.kit-unavailable", "<red>The duel kit is unavailable.");
            return;
        }
        Arena arena = plugin.arenas().firstAvailable(kit.id()).orElse(null);
        if (arena == null || !arena.isFfaReady()) {
            plugin.messages().send(target, "duel.request.arena-unavailable", "<red>No arena is available for that duel kit.");
            return;
        }
        plugin.messages().send(challenger, "duel.request.accepted.challenger", "<green>Your duel request was accepted by {target}!", Map.of("target", target.getName()));
        plugin.messages().send(target, "duel.request.accepted.target", "<green>You accepted the duel request from {challenger}!", Map.of("challenger", challenger.getName()));
        start(challenger, target, kit, arena);
    }

    public void declineDuelRequest(Player target) {
        DuelInvite invite = pendingDuelInvites.remove(target.getUniqueId());
        if (invite == null || invite.expired()) {
            plugin.messages().send(target, "duel.request.no-active", "<red>There is no active duel request to decline.", Map.of("action", "decline"));
            return;
        }
        Player challenger = Bukkit.getPlayer(invite.challenger());
        if (challenger != null) {
            plugin.messages().send(challenger, "duel.request.declined.challenger", "<red>Your duel request was declined by {target}.", Map.of("target", target.getName()));
        }
        plugin.messages().send(target, "duel.request.declined.target", "<yellow>You declined the duel request.");
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
        if (match == null && loserId != null) match = matches.get(loserId);
        if (match == null || !match.markEnded()) return;

        Set<UUID> participants = match.participants();
        participants.forEach(matches::remove);
        cancelTimeout(match);
        match.arena().release();

        Set<UUID> winnerTeam = winnerId != null && match.contains(winnerId)
                ? match.teamOf(winnerId)
                : (loserId != null ? match.opposingTeam(loserId) : match.teamOne());
        Set<UUID> loserTeam = loserId != null && match.contains(loserId)
                ? match.teamOf(loserId)
                : (winnerId != null ? match.opposingTeam(winnerId) : match.teamTwo());

        applyMatchStats(winnerTeam, loserTeam);
        awardTeams(winnerTeam, loserTeam, match);

        Location lobby = plugin.arenas().lobby();
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                if (lobby != null) resetAndTeleport(player, lobby);
                if (reason != null) plugin.messages().send(player, "duel.result", "<gray>Result: {reason}</gray>", Map.of("reason", reason));
            }
        }
    }

    private void applyMatchStats(Set<UUID> winners, Set<UUID> losers) {
        for (UUID uuid : winners) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            plugin.profiles().getOrCreate(player).applyKill();
        }
        for (UUID uuid : losers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            plugin.profiles().getOrCreate(player).applyDeath();
        }
    }

    public void draw(DuelMatch match, String reason) {
        if (match == null || !match.markEnded()) return;

        Set<UUID> participants = match.participants();
        participants.forEach(matches::remove);
        cancelTimeout(match);
        match.arena().release();

        Location lobby = plugin.arenas().lobby();
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                plugin.messages().send(player, "duel.draw", "<yellow>Match ended: {reason}</yellow>", Map.of("reason", reason));
                if (lobby != null) resetAndTeleport(player, lobby);
            }
        }
    }

    public void handleDeath(Player loser, String reason) {
        DuelMatch match = matches.get(loser.getUniqueId());
        if (match == null || match.isEliminated(loser.getUniqueId())) return;
        match.eliminate(loser.getUniqueId());
        loser.setInvulnerable(true);
        loser.getInventory().clear();
        loser.getInventory().setArmorContents(null);
        plugin.messages().send(loser, "duel.player-eliminated", "<red>You were eliminated.");
        Location lobby = plugin.arenas().lobby();
        if (lobby != null) resetAndTeleport(loser, lobby);

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
            plugin.messages().send(player, "duel.start.fight", "<green>Fight!");
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
            plugin.messages().send(player, "duel.win", "<green>Your team won +{elo} Elo.</green>", Map.of("elo", String.valueOf(change)));
            applyKillBoostReward(player, match);
        }
        for (UUID uuid : losers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            PlayerProfile profile = plugin.profiles().getOrCreate(player);
            profile.applyLoss(change);
            if (plugin.getConfig().getBoolean("settings.streak.protection.enabled", true) && profile.vouchers() > 0) {
                profile.useVoucher();
                plugin.messages().send(player, "duel.voucher.used", "<green>Your streak was preserved by a voucher! Remaining: <white>{remaining}</white></green>", Map.of("remaining", String.valueOf(profile.vouchers())));
                plugin.debug("Streak voucher consumed for " + player.getName() + "; remaining " + profile.vouchers());
            } else {
                profile.resetStreak();
            }
            plugin.messages().send(player, "duel.loss", "<red>Your team lost -{elo} Elo.</red>", Map.of("elo", String.valueOf(change)));
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

    private void applyKillBoostReward(Player player, DuelMatch match) {
        if (!plugin.getConfig().getBoolean("settings.kill-boost.enabled", false)) return;
        PlayerProfile profile = plugin.profiles().getOrCreate(player);
        if (profile.killBoosts() <= 0) return;
        if (!profile.useKillBoost()) return;

        String victimName = match.opponent(player.getUniqueId()) != null ? Bukkit.getPlayer(match.opponent(player.getUniqueId())) != null ? Bukkit.getPlayer(match.opponent(player.getUniqueId())).getName() : "unknown" : "unknown";
        for (String command : plugin.getConfig().getStringList("settings.kill-boost.reward-commands")) {
            String resolved = command
                    .replace("%player%", player.getName())
                    .replace("%victim%", victimName);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
        }
        plugin.messages().send(player, "<green>Your kill boost activated!</green>");
        plugin.debug("Kill boost used by " + player.getName() + " in match win. Remaining: " + profile.killBoosts());
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
