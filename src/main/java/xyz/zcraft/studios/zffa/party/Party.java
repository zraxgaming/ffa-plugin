package xyz.zcraft.studios.zffa.party;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class Party {
    private UUID leader;
    private final Set<UUID> members = new LinkedHashSet<>();

    public Party(UUID leader) {
        this.leader = leader;
        this.members.add(leader);
    }

    public UUID leader() { return leader; }
    public Set<UUID> members() { return Set.copyOf(members); }
    public boolean isLeader(UUID uuid) { return leader.equals(uuid); }
    public boolean contains(UUID uuid) { return members.contains(uuid); }
    public int size() { return members.size(); }

    public void add(UUID uuid) {
        members.add(uuid);
    }

    public void remove(UUID uuid) {
        members.remove(uuid);
        if (leader.equals(uuid) && !members.isEmpty()) leader = members.iterator().next();
    }
}
