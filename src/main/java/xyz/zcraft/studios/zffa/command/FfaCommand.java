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
import xyz.zcraft.studios.zffa.profile.EloCalculator;
import xyz.zcraft.studios.zffa.profile.PlayerProfile;

import java.util.ArrayList;
import java.util.List;

public final class FfaCommand implements CommandExecutor, TabCompleter {
    private final ZFfaPlugin plugin;

    public FfaCommand(ZFfaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "<red>Player-only command.");
            return true;
        }
        if (!player.hasPermission("zf.player")) {
            plugin.messages().send(player, "<red>No permission.");
            return true;
        }

        String lowerLabel = label.toLowerCase();
        if (lowerLabel.equals("duel")) {
            handleDuelCommand(player, args);
            return true;
        }

        String sub = args.length == 0 ? "join" : args[0].toLowerCase();
        switch (sub) {
            case "setspawn" -> {
                if (!player.hasPermission("zf.admin") && !player.hasPermission("ffa.setspawn")) {
                    plugin.messages().send(player, "<red>No permission.");
                    return true;
                }
                plugin.arenas().setLobby(player.getLocation());
                plugin.messages().send(player, "<green>FFA spawn/lobby set.");
            }
            case "setarena", "setviparena" -> {
                if (!player.hasPermission("zf.admin") && !player.hasPermission("ffa." + sub)) {
                    plugin.messages().send(player, "<red>No permission.");
                    return true;
                }
                if (args.length < 2) {
                    plugin.messages().send(player, "<red>Usage: /ffa " + sub + " <name>");
                    return true;
                }
                Arena arena = plugin.arenas().create(args[1]);
                if ("setviparena".equals(sub)) arena.vip(true);
                arena.addFfaSpawn(player.getLocation());
                plugin.arenas().saveArena(arena);
                plugin.messages().send(player, "<green>Saved " + (arena.vip() ? "VIP " : "") + "FFA arena <white>" + arena.name() + "</white> spawn #" + arena.ffaSpawns().size() + ".");
            }
            case "createkit" -> {
                if (!player.hasPermission("zf.admin") && !player.hasPermission("ffa.createkit")) {
                    plugin.messages().send(player, "<red>No permission.");
                    return true;
                }
                if (args.length < 2) {
                    plugin.messages().send(player, "<red>Usage: /ffa createkit <name> [display]");
                    return true;
                }
                String display = args.length >= 3 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "<white>" + args[1].toLowerCase() + "</white>";
                plugin.kits().saveFromPlayer(player, args[1], display);
                plugin.gui().rebuild();
                plugin.messages().send(player, "<green>Saved kit <white>" + args[1].toLowerCase() + "</white>.");
            }
            case "join", "queue", "kits", "play" -> plugin.gui().openKits(player);
            case "unranked" -> plugin.gui().openKits(player, false);
            case "arena" -> {
                if (!plugin.getConfig().getBoolean("commands.arena.enabled", false)) {
                    plugin.messages().send(player, "<red>This command is disabled.");
                    return true;
                }
                joinFfa(player, args);
            }
            case "viparena" -> {
                if (!plugin.getConfig().getBoolean("commands.viparena.enabled", false)) {
                    plugin.messages().send(player, "<red>This command is disabled.");
                    return true;
                }
                if (!player.hasPermission("zf.viparena")) {
                    plugin.messages().send(player, "<red>No permission for VIP arenas.");
                    return true;
                }
                Arena arena = plugin.ffa().defaultVipArena().orElse(null);
                if (arena == null) {
                    plugin.messages().send(player, "<red>No VIP FFA arena is configured.");
                    return true;
                }
                Kit kit = plugin.ffa().defaultKit(arena).orElse(null);
                if (kit == null) {
                    plugin.messages().send(player, "<red>No compatible kit found for the VIP arena.");
                    return true;
                }
                plugin.ffa().join(player, arena, kit);
            }
            case "kit" -> {
                if (args.length < 2) {
                    plugin.gui().openKits(player);
                    return true;
                }
                plugin.kits().get(args[1]).ifPresentOrElse(kit -> {
                    if (!player.hasPermission("zf.kit." + kit.id()) && !player.hasPermission("zf.kit.*")) {
                        plugin.messages().send(player, "<red>You do not have permission for that kit.");
                        return;
                    }
                    kit.apply(player);
                    plugin.messages().send(player, "<green>Equipped kit <white>" + kit.id() + "</white>.");
                }, () -> plugin.messages().send(player, "<red>Kit not found."));
            }
            case "leave", "quit" -> {
                plugin.queues().leave(player.getUniqueId());
                plugin.matches().forfeit(player, "Forfeit");
                plugin.ffa().leave(player);
                plugin.messages().send(player, "<yellow>You left the queue or match.");
            }
            case "stats" -> plugin.gui().openStats(player);
            case "top", "leaderboard" -> plugin.gui().openLeaderboard(player);
            case "items" -> {
                plugin.gui().giveLobbyItems(player);
                plugin.messages().send(player, "<green>Lobby items refreshed.");
            }
            case "spawn", "lobby" -> {
                if (plugin.matches().isInMatch(player.getUniqueId())) {
                    plugin.matches().forfeit(player, "Forfeit");
                } else if (plugin.ffa().isInFfa(player.getUniqueId())) {
                    plugin.ffa().leave(player);
                } else if (plugin.arenas().lobby() != null) {
                    player.teleportAsync(plugin.arenas().lobby());
                }
            }
            case "status" -> plugin.messages().send(player, "<gray>Status: <white>" + plugin.queues().status(player.getUniqueId()) + "</white>");
            case "whoami" -> {
                PlayerProfile profile = plugin.profiles().getOrCreate(player);
                plugin.messages().send(player, "<gray>Elo: <white>" + profile.elo() + "</white> Rank: <white>" + plugin.ranks().rankName(profile.elo()) + "</white>");
            }
            default -> plugin.messages().send(player, "<yellow>/" + label + "</yellow> <gray>join, leave, stats, top, items, spawn, status</gray>");
        }
        return true;
    }

    private void joinFfa(Player player, String[] args) {
        Arena arena = args.length >= 2
                ? plugin.arenas().get(args[1]).orElse(null)
                : plugin.ffa().defaultArena().orElse(null);
        if (arena == null) {
            plugin.messages().send(player, "<red>FFA arena not found.");
            return;
        }
        Kit kit = args.length >= 3
                ? plugin.kits().get(args[2]).orElse(null)
                : plugin.ffa().defaultKit(arena).orElse(null);
        if (kit == null) {
            plugin.messages().send(player, "<red>No compatible kit found for that arena.");
            return;
        }
        if (!player.hasPermission("zf.kit." + kit.id()) && !player.hasPermission("zf.kit.*")) {
            plugin.messages().send(player, "<red>You do not have permission for that kit.");
            return;
        }
        plugin.ffa().join(player, arena, kit);
    }

    private void handleDuelCommand(Player player, String[] args) {
        if (args.length == 0) {
            plugin.messages().send(player, "<yellow>Usage: /duel <player> | /duel accept | /duel decline");
            return;
        }
        String action = args[0].toLowerCase();
        if (action.equals("accept")) {
            plugin.matches().acceptDuelRequest(player);
            return;
        }
        if (action.equals("decline")) {
            plugin.matches().declineDuelRequest(player);
            return;
        }
        String targetName = args[0];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            plugin.messages().send(player, "<red>Player not found.");
            return;
        }
        if (target.equals(player)) {
            plugin.messages().send(player, "<red>You cannot duel yourself.");
            return;
        }
        if (args.length == 1) {
            plugin.gui().openDuelKits(player, target, true);
            return;
        }
        String kitName = args[1];
        plugin.matches().sendDuelRequest(player, targetName, kitName);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (alias.equalsIgnoreCase("duel")) {
            if (args.length == 1) {
                List<String> options = new ArrayList<>();
                options.add("accept");
                options.add("decline");
                Bukkit.getOnlinePlayers().stream().map(Player::getName).forEach(options::add);
                return options.stream()
                        .filter(option -> option.toLowerCase().startsWith(args[0].toLowerCase()))
                        .toList();
            }
            if (args.length == 2) {
                return plugin.kits().all().stream().map(Kit::id).filter(name -> name.startsWith(args[1].toLowerCase())).toList();
            }
            return List.of();
        }
        if (args.length == 1) {
            return List.of("join", "unranked", "arena", "viparena", "kit", "leave", "stats", "top", "items", "spawn", "status").stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("arena")) {
            return plugin.arenas().all().stream().filter(Arena::isFfaReady).map(Arena::name).filter(name -> name.startsWith(args[1].toLowerCase())).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("arena")) {
            return plugin.kits().all().stream().map(Kit::id).filter(name -> name.startsWith(args[2].toLowerCase())).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("kit")) {
            return plugin.kits().all().stream().map(Kit::id).filter(name -> name.startsWith(args[1].toLowerCase())).toList();
        }
        return List.of();
    }
}
