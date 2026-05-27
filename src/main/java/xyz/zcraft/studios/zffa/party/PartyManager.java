package xyz.zcraft.studios.zffa.party;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xyz.zcraft.studios.zffa.ZFfaPlugin;
import xyz.zcraft.studios.zffa.arena.Arena;
import xyz.zcraft.studios.zffa.kit.Kit;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PartyManager {
    private final ZFfaPlugin plugin;
    private final Map<UUID, Party> partiesByMember = new HashMap<>();
    private final Map<UUID, UUID> invites = new HashMap<>();

    public PartyManager(ZFfaPlugin plugin) {
        this.plugin = plugin;
    }

    public Optional<Party> party(UUID uuid) {
        return Optional.ofNullable(partiesByMember.get(uuid));
    }

    public Party getOrCreate(Player leader) {
        return party(leader.getUniqueId()).orElseGet(() -> {
            Party party = new Party(leader.getUniqueId());
            partiesByMember.put(leader.getUniqueId(), party);
            return party;
        });
    }

    public void invite(Player leader, Player target) {
        Party party = getOrCreate(leader);
        if (!party.isLeader(leader.getUniqueId())) {
            plugin.messages().send(leader, "<red>Only the party leader can invite players.");
            return;
        }
        if (party.contains(target.getUniqueId())) {
            plugin.messages().send(leader, "<yellow>That player is already in your party.");
            return;
        }
        invites.put(target.getUniqueId(), leader.getUniqueId());
        plugin.messages().send(leader, "<green>Invited <white>" + target.getName() + "</white>.");
        plugin.messages().send(target, "<green>" + leader.getName() + " invited you to a party. Use <white>/party accept</white>.");
    }

    public void accept(Player player) {
        UUID leaderId = invites.remove(player.getUniqueId());
        if (leaderId == null) {
            plugin.messages().send(player, "<red>You do not have a party invite.");
            return;
        }
        Player leader = Bukkit.getPlayer(leaderId);
        if (leader == null) {
            plugin.messages().send(player, "<red>That party is no longer available.");
            return;
        }
        Party party = getOrCreate(leader);
        leave(player, false);
        party.add(player.getUniqueId());
        partiesByMember.put(player.getUniqueId(), party);
        broadcast(party, "<green>" + player.getName() + " joined the party.");
    }

    public void leave(Player player, boolean message) {
        Party party = partiesByMember.get(player.getUniqueId());
        if (party == null) return;
        party.remove(player.getUniqueId());
        partiesByMember.remove(player.getUniqueId());
        if (party.size() == 0) return;
        if (message) broadcast(party, "<yellow>" + player.getName() + " left the party.");
    }

    public void disband(Player player) {
        Party party = partiesByMember.get(player.getUniqueId());
        if (party == null || !party.isLeader(player.getUniqueId())) {
            plugin.messages().send(player, "<red>You are not the leader of a party.");
            return;
        }
        broadcast(party, "<yellow>The party was disbanded.");
        for (UUID member : party.members()) partiesByMember.remove(member);
    }

    public boolean allOnlineAndFree(Party party) {
        for (UUID uuid : party.members()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || plugin.matches().isInMatch(uuid) || plugin.ffa().isInFfa(uuid)) return false;
        }
        return true;
    }

    public void joinFfa(Party party, Arena arena, Kit kit) {
        for (UUID uuid : party.members()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) plugin.ffa().join(player, arena, kit);
        }
    }

    public void broadcast(Party party, String message) {
        for (UUID uuid : party.members()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) plugin.messages().send(player, message);
        }
    }
}
