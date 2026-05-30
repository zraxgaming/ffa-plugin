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
            plugin.messages().send(leader, "party.only-leader-invite", "<red>Only the party leader can invite players.");
            return;
        }
        if (party.contains(target.getUniqueId())) {
            plugin.messages().send(leader, "party.already-in-party", "<yellow>That player is already in your party.");
            return;
        }
        int limit = partyLimit(leader);
        if (party.size() >= limit) {
            plugin.messages().send(leader, "party.full", "<red>Your party has reached its maximum size of {limit}.", Map.of("limit", String.valueOf(limit)));
            return;
        }
        invites.put(target.getUniqueId(), leader.getUniqueId());
        plugin.messages().send(leader, "party.invited", "<green>Invited <white>{target}</white>.", Map.of("target", target.getName()));
        plugin.messages().send(target, "party.invited-target", "<green>{leader} invited you to a party. Use <white>/party accept</white>.", Map.of("leader", leader.getName()));
    }

    public void accept(Player player) {
        UUID leaderId = invites.remove(player.getUniqueId());
        if (leaderId == null) {
            plugin.messages().send(player, "party.no-invite", "<red>You do not have a party invite.");
            return;
        }
        Player leader = Bukkit.getPlayer(leaderId);
        if (leader == null) {
            plugin.messages().send(player, "party.invite-expired", "<red>That party is no longer available.");
            return;
        }
        Party party = getOrCreate(leader);
        int limit = partyLimit(leader);
        if (party.size() >= limit) {
            plugin.messages().send(player, "party.full", "<red>That party is full. Maximum size is {limit}.", Map.of("limit", String.valueOf(limit)));
            return;
        }
        leave(player, false);
        party.add(player.getUniqueId());
        partiesByMember.put(player.getUniqueId(), party);
        broadcast(party, plugin.messages().get("party.joined", "<green>{player} joined the party.").replace("{player}", player.getName()));
    }

    public void leave(Player player, boolean message) {
        Party party = partiesByMember.get(player.getUniqueId());
        if (party == null) return;
        party.remove(player.getUniqueId());
        partiesByMember.remove(player.getUniqueId());
        if (party.size() == 0) return;
        if (message) broadcast(party, plugin.messages().get("party.left", "<yellow>{player} left the party.").replace("{player}", player.getName()));
    }

    public void disband(Player player) {
        Party party = partiesByMember.get(player.getUniqueId());
        if (party == null || !party.isLeader(player.getUniqueId())) {
            plugin.messages().send(player, "party.not-leader", "<red>You are not the leader of a party.");
            return;
        }
        broadcast(party, plugin.messages().get("party.disbanded", "<yellow>The party was disbanded."));
        for (UUID member : party.members()) partiesByMember.remove(member);
    }

    public boolean allOnlineAndFree(Party party) {
        for (UUID uuid : party.members()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || plugin.matches().isInMatch(uuid) || plugin.ffa().isInFfa(uuid)) return false;
        }
        return true;
    }

    public int partyLimit(Player player) {
        int maxSize = 5;
        for (org.bukkit.permissions.PermissionAttachmentInfo attachment : player.getEffectivePermissions()) {
            String permission = attachment.getPermission();
            if (permission != null && permission.toLowerCase().startsWith("zf.party.")) {
                String value = permission.substring("zf.party.".length());
                try {
                    int limit = Integer.parseInt(value);
                    if (limit > maxSize) maxSize = limit;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return maxSize;
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
