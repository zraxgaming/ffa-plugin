package xyz.zcraft.studios.zffa.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import xyz.zcraft.studios.zffa.ZFfaPlugin;
import xyz.zcraft.studios.zffa.arena.Arena;
import xyz.zcraft.studios.zffa.kit.Kit;
import xyz.zcraft.studios.zffa.party.Party;

import java.util.List;

public final class PartyCommand implements CommandExecutor, TabCompleter {
    private final ZFfaPlugin plugin;

    public PartyCommand(ZFfaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "command.player-only", "<red>Player-only command.");
            return true;
        }
        if (args.length == 0) {
            help(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "create" -> {
                plugin.parties().getOrCreate(player);
                plugin.messages().send(player, "party.created", "<green>Party created.");
            }
            case "invite" -> {
                if (args.length < 2) {
                    plugin.messages().send(player, "<red>Usage: /party invite <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    plugin.messages().send(player, "<red>That player is not online.");
                    return true;
                }
                plugin.parties().invite(player, target);
            }
            case "accept" -> plugin.parties().accept(player);
            case "leave" -> {
                if (plugin.parties().party(player.getUniqueId()).isEmpty()) {
                    plugin.messages().send(player, "party.not-in-party", "<red>You are not in a party.");
                    return true;
                }
                plugin.parties().leave(player, true);
                plugin.messages().send(player, "party.left-self", "<yellow>You left the party.");
            }
            case "disband" -> plugin.parties().disband(player);
            case "list" -> {
                Party party = plugin.parties().party(player.getUniqueId()).orElse(null);
                if (party == null) {
                    plugin.messages().send(player, "party.not-in-party", "<red>You are not in a party.");
                    return true;
                }
                String members = party.members().stream()
                        .map(Bukkit::getOfflinePlayer)
                        .map(offline -> offline.getName() == null ? "Unknown" : offline.getName())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("none");
                plugin.messages().send(player, "<gray>Party: <white>" + members + "</white>");
            }
            case "duel" -> {
                Party party = requireLeaderParty(player);
                if (party == null) return true;
                if (args.length < 2) {
                    plugin.messages().send(player, "<red>Usage: /party duel <kit>");
                    return true;
                }
                Kit kit = plugin.kits().get(args[1]).orElse(null);
                if (kit == null) {
                    plugin.messages().send(player, "<red>Kit not found.");
                    return true;
                }
                plugin.queues().joinParty(party, kit);
            }
            case "ffa" -> {
                Party party = requireLeaderParty(player);
                if (party == null) return true;
                Arena arena = args.length >= 2 ? plugin.arenas().get(args[1]).orElse(null) : plugin.ffa().defaultArena().orElse(null);
                if (arena == null) {
                    plugin.messages().send(player, "<red>FFA arena not found.");
                    return true;
                }
                Kit kit = args.length >= 3 ? plugin.kits().get(args[2]).orElse(null) : plugin.ffa().defaultKit(arena).orElse(null);
                if (kit == null) {
                    plugin.messages().send(player, "ffa.no-compatible-kit", "<red>No compatible kit found for that arena.");
                    return true;
                }
                if (!plugin.parties().allOnlineAndFree(party)) {
                    plugin.messages().send(player, "<red>Every party member must be online and out of matches/FFA.");
                    return true;
                }
                plugin.parties().joinFfa(party, arena, kit);
            }
            default -> help(player);
        }
        return true;
    }

    private Party requireLeaderParty(Player player) {
        Party party = plugin.parties().party(player.getUniqueId()).orElse(null);
        if (party == null) {
            plugin.messages().send(player, "party.not-in-party", "<red>You are not in a party.");
            return null;
        }
        if (!party.isLeader(player.getUniqueId())) {
            plugin.messages().send(player, "party.no-leader-action", "<red>Only the party leader can do that.");
            return null;
        }
        return party;
    }

    private void help(Player player) {
        plugin.messages().send(player, "<yellow>/party create</yellow>");
        plugin.messages().send(player, "<yellow>/party invite <player></yellow>");
        plugin.messages().send(player, "<yellow>/party accept</yellow>");
        plugin.messages().send(player, "<yellow>/party duel <kit></yellow>");
        plugin.messages().send(player, "<yellow>/party ffa [arena] [kit]</yellow>");
        plugin.messages().send(player, "<yellow>/party leave</yellow>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("create", "invite", "accept", "leave", "disband", "list", "duel", "ffa").stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("duel")) {
            return plugin.kits().all().stream().map(Kit::id).filter(id -> id.startsWith(args[1].toLowerCase())).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("ffa")) {
            return plugin.arenas().all().stream().filter(Arena::isFfaReady).map(Arena::name).filter(id -> id.startsWith(args[1].toLowerCase())).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("ffa")) {
            return plugin.kits().all().stream().map(Kit::id).filter(id -> id.startsWith(args[2].toLowerCase())).toList();
        }
        return List.of();
    }
}
