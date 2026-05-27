package xyz.zcraft.studios.zffa.duel;

import xyz.zcraft.studios.zffa.arena.Arena;
import xyz.zcraft.studios.zffa.kit.Kit;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DuelMatch {
    private final Set<UUID> teamOne;
    private final Set<UUID> teamTwo;
    private final Set<UUID> eliminated = new HashSet<>();
    private final Kit kit;
    private final Arena arena;
    private final long startedAtMillis;
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicBoolean ended = new AtomicBoolean(false);

    public DuelMatch(UUID playerOne, UUID playerTwo, Kit kit, Arena arena, long startedAtMillis) {
        this(Set.of(playerOne), Set.of(playerTwo), kit, arena, startedAtMillis);
    }

    public DuelMatch(Set<UUID> teamOne, Set<UUID> teamTwo, Kit kit, Arena arena, long startedAtMillis) {
        this.teamOne = new HashSet<>(teamOne);
        this.teamTwo = new HashSet<>(teamTwo);
        this.kit = kit;
        this.arena = arena;
        this.startedAtMillis = startedAtMillis;
    }

    public UUID playerOne() { return teamOne.iterator().next(); }
    public UUID playerTwo() { return teamTwo.iterator().next(); }
    public Set<UUID> teamOne() { return Set.copyOf(teamOne); }
    public Set<UUID> teamTwo() { return Set.copyOf(teamTwo); }
    public Set<UUID> participants() {
        HashSet<UUID> ids = new HashSet<>(teamOne);
        ids.addAll(teamTwo);
        return ids;
    }
    public Kit kit() { return kit; }
    public Arena arena() { return arena; }
    public long startedAtMillis() { return startedAtMillis; }
    public boolean active() { return active.get(); }
    public void activate() { active.set(true); }
    public boolean markEnded() { return ended.compareAndSet(false, true); }

    public boolean contains(UUID uuid) {
        return teamOne.contains(uuid) || teamTwo.contains(uuid);
    }

    public UUID opponent(UUID uuid) {
        Set<UUID> opponents = teamOne.contains(uuid) ? teamTwo : teamOne;
        return opponents.iterator().next();
    }

    public Set<UUID> opposingTeam(UUID uuid) {
        return teamOne.contains(uuid) ? Set.copyOf(teamTwo) : Set.copyOf(teamOne);
    }

    public Set<UUID> teamOf(UUID uuid) {
        return teamOne.contains(uuid) ? Set.copyOf(teamOne) : Set.copyOf(teamTwo);
    }

    public void eliminate(UUID uuid) {
        eliminated.add(uuid);
    }

    public boolean isEliminated(UUID uuid) {
        return eliminated.contains(uuid);
    }

    public boolean isTeamEliminated(Set<UUID> team) {
        return eliminated.containsAll(team);
    }
}
