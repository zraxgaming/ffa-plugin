package xyz.zcraft.studios.zffa.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.zcraft.studios.zffa.ZFfaPlugin;
import xyz.zcraft.studios.zffa.profile.PlayerProfile;
import xyz.zcraft.studios.zffa.arena.Arena;
import xyz.zcraft.studios.zffa.kit.Kit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ZffaAdminCommand implements CommandExecutor, TabCompleter {
    private final ZFfaPlugin plugin;

    public ZffaAdminCommand(ZFfaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("zf.admin")) {
            plugin.messages().send(sender, "<red>No permission.");
            return true;
        }
        if (args.length == 0) {
            help(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadCore();
                plugin.messages().send(sender, "<green>Z-FFA reloaded.");
            }
            case "setlobby" -> requirePlayer(sender).ifPresent(player -> {
                plugin.arenas().setLobby(player.getLocation());
                plugin.messages().send(player, "<green>Global lobby set.");
            });
            case "items" -> requirePlayer(sender).ifPresent(player -> {
                plugin.gui().giveLobbyItems(player);
                plugin.messages().send(player, "<green>Lobby items refreshed.");
            });
            case "debug" -> handleDebug(sender, args);
            case "voucher" -> handleVoucher(sender, args);
            case "killboost" -> handleKillBoost(sender, args);
            case "streak" -> handleStreak(sender, args);
            case "arena" -> handleArena(sender, args);
            case "kit" -> handleKit(sender, args);
            default -> help(sender);
        }
        return true;
    }

    private void handleDebug(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().send(sender, "<yellow>/zffa debug status</yellow>");
            plugin.messages().send(sender, "<yellow>/zffa debug set <true|false></yellow>");
            return;
        }
        if ("status".equalsIgnoreCase(args[1])) {
            plugin.messages().send(sender, "<gray>Debug enabled: <white>" + plugin.getConfig().getBoolean("settings.debug-enabled", false) + "</white>");
            return;
        }
        if ("set".equalsIgnoreCase(args[1]) && args.length >= 3) {
            boolean enabled = Boolean.parseBoolean(args[2]);
            plugin.getConfig().set("settings.debug-enabled", enabled);
            plugin.saveConfig();
            plugin.messages().send(sender, "<green>Debug mode set to <white>" + enabled + "</white>.");
            return;
        }
        plugin.messages().send(sender, "<red>Usage: /zffa debug status|set <true|false>");
    }

    private void handleVoucher(CommandSender sender, String[] args) {
        if (args.length < 4) {
            plugin.messages().send(sender, "<yellow>/zffa voucher give <player> <amount></yellow>");
            plugin.messages().send(sender, "<yellow>/zffa voucher set <player> <amount></yellow>");
            plugin.messages().send(sender, "<yellow>/zffa voucher remove <player> <amount></yellow>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            plugin.messages().send(sender, "<red>Player must be online.");
            return;
        }
        PlayerProfile profile = plugin.profiles().getOrCreate(target);
        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            plugin.messages().send(sender, "<red>Amount must be a number.");
            return;
        }
        if (amount < 0) {
            plugin.messages().send(sender, "<red>Amount must be positive.");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "give" -> {
                profile.addVouchers(amount);
                plugin.messages().send(sender, "<green>Gave <white>" + amount + "</white> streak voucher(s) to <white>" + target.getName() + "</white>.");
            }
            case "set" -> {
                profile.setVouchers(amount);
                plugin.messages().send(sender, "<green>Set <white>" + target.getName() + "</white> voucher total to <white>" + profile.vouchers() + "</white>.");
            }
            case "remove" -> {
                profile.removeVouchers(amount);
                plugin.messages().send(sender, "<yellow>Removed <white>" + amount + "</white> voucher(s) from <white>" + target.getName() + "</white>.");
            }
            default -> plugin.messages().send(sender, "<red>Usage: /zffa voucher give|set|remove <player> <amount>");
        }
    }

    private void handleKillBoost(CommandSender sender, String[] args) {
        if (args.length < 4) {
            plugin.messages().send(sender, "<yellow>/zffa killboost give <player> <amount></yellow>");
            plugin.messages().send(sender, "<yellow>/zffa killboost set <player> <amount></yellow>");
            plugin.messages().send(sender, "<yellow>/zffa killboost remove <player> <amount></yellow>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            plugin.messages().send(sender, "<red>Player must be online.");
            return;
        }
        PlayerProfile profile = plugin.profiles().getOrCreate(target);
        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            plugin.messages().send(sender, "<red>Amount must be a number.");
            return;
        }
        if (amount < 0) {
            plugin.messages().send(sender, "<red>Amount must be positive.");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "give" -> {
                profile.addKillBoosts(amount);
                plugin.messages().send(sender, "<green>Gave <white>" + amount + "</white> kill boost(s) to <white>" + target.getName() + "</white>.");
            }
            case "set" -> {
                profile.setKillBoosts(amount);
                plugin.messages().send(sender, "<green>Set <white>" + target.getName() + "</white> kill boosts to <white>" + profile.killBoosts() + "</white>.");
            }
            case "remove" -> {
                profile.removeKillBoosts(amount);
                plugin.messages().send(sender, "<yellow>Removed <white>" + amount + "</white> kill boost(s) from <white>" + target.getName() + "</white>.");
            }
            default -> plugin.messages().send(sender, "<red>Usage: /zffa killboost give|set|remove <player> <amount>");
        }
    }

    private void handleStreak(CommandSender sender, String[] args) {
        if (args.length < 4) {
            plugin.messages().send(sender, "<yellow>/zffa streak set <player> <amount></yellow>");
            plugin.messages().send(sender, "<yellow>/zffa streak reset <player></yellow>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            plugin.messages().send(sender, "<red>Player must be online.");
            return;
        }
        PlayerProfile profile = plugin.profiles().getOrCreate(target);
        switch (args[1].toLowerCase()) {
            case "set" -> {
                int amount;
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    plugin.messages().send(sender, "<red>Amount must be a number.");
                    return;
                }
                if (amount < 0) {
                    plugin.messages().send(sender, "<red>Amount must be positive.");
                    return;
                }
                profile.setStreak(amount);
                plugin.messages().send(sender, "<green>Set <white>" + target.getName() + "</white> streak to <white>" + profile.streak() + "</white>.");
            }
            case "reset" -> {
                profile.resetStreak();
                plugin.messages().send(sender, "<green>Reset <white>" + target.getName() + "</white> streak.</green>");
            }
            default -> plugin.messages().send(sender, "<red>Usage: /zffa streak set <player> <amount> | reset <player>");
        }
    }

    private void handleArena(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().send(sender, "<yellow>/zffa arena create <name></yellow>");
            plugin.messages().send(sender, "<yellow>/zffa arena <name> setspawn1|setspawn2</yellow>");
            return;
        }
        if ("create".equalsIgnoreCase(args[1])) {
            if (args.length < 3) {
                plugin.messages().send(sender, "<red>Usage: /zffa arena create <name>");
                return;
            }
            boolean existed = plugin.arenas().exists(args[2]);
            Arena arena = plugin.arenas().create(args[2]);
            plugin.messages().send(sender, existed
                    ? "<yellow>Arena <white>" + arena.name() + "</white> already exists."
                    : "<green>Arena <white>" + arena.name() + "</white> created.");
            return;
        }
        if ("list".equalsIgnoreCase(args[1])) {
            String arenas = plugin.arenas().all().stream()
                    .map(arena -> arena.name() + (arena.enabled() ? "" : " (disabled)") + (arena.isBusy() ? " (busy)" : ""))
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("none");
            plugin.messages().send(sender, "<gray>Arenas: <white>" + arenas + "</white>");
            return;
        }
        if (args.length < 3) {
            plugin.messages().send(sender, "<red>Usage: /zffa arena <name> setspawn1|setspawn2|addkit|removekit|info");
            return;
        }
        if (!plugin.arenas().exists(args[1])) {
            plugin.messages().send(sender, "<red>Arena does not exist. Use <yellow>/zffa arena create " + args[1] + "</yellow> first.");
            return;
        }
        Arena arena = plugin.arenas().arena(args[1]);
        switch (args[2].toLowerCase()) {
            case "setspawn1", "setspawn2" -> requirePlayer(sender).ifPresent(player -> {
                if ("setspawn1".equalsIgnoreCase(args[2])) arena.spawn1(player.getLocation());
                else arena.spawn2(player.getLocation());
                plugin.arenas().saveArena(arena);
                plugin.messages().send(player, "<green>Saved <white>" + args[2].toLowerCase() + "</white> for arena <white>" + arena.name() + "</white>.");
            });
            case "addffaspawn" -> requirePlayer(sender).ifPresent(player -> {
                arena.addFfaSpawn(player.getLocation());
                plugin.arenas().saveArena(arena);
                plugin.messages().send(player, "<green>Added FFA spawn #" + arena.ffaSpawns().size() + " to arena <white>" + arena.name() + "</white>.");
            });
            case "clearffaspawns" -> {
                arena.clearFfaSpawns();
                plugin.arenas().saveArena(arena);
                plugin.messages().send(sender, "<yellow>Cleared FFA spawns for arena <white>" + arena.name() + "</white>.");
            }
            case "addkit" -> {
                if (args.length < 4) {
                    plugin.messages().send(sender, "<red>Usage: /zffa arena <name> addkit <kit>");
                    return;
                }
                if (!plugin.kits().exists(args[3])) {
                    plugin.messages().send(sender, "<red>Kit does not exist.");
                    return;
                }
                arena.addKit(args[3]);
                plugin.arenas().saveArena(arena);
                plugin.messages().send(sender, "<green>Added kit <white>" + args[3].toLowerCase() + "</white> to arena <white>" + arena.name() + "</white>.");
            }
            case "removekit" -> {
                if (args.length < 4) {
                    plugin.messages().send(sender, "<red>Usage: /zffa arena <name> removekit <kit>");
                    return;
                }
                arena.removeKit(args[3]);
                plugin.arenas().saveArena(arena);
                plugin.messages().send(sender, "<yellow>Removed kit <white>" + args[3].toLowerCase() + "</white> from arena <white>" + arena.name() + "</white>.");
            }
            case "enable", "disable" -> {
                arena.enabled("enable".equalsIgnoreCase(args[2]));
                plugin.arenas().saveArena(arena);
                plugin.messages().send(sender, "<green>Arena <white>" + arena.name() + "</white> " + (arena.enabled() ? "enabled" : "disabled") + ".");
            }
            case "vip" -> {
                if (args.length < 4) {
                    plugin.messages().send(sender, "<red>Usage: /zffa arena <name> vip true|false");
                    return;
                }
                arena.vip(Boolean.parseBoolean(args[3]));
                plugin.arenas().saveArena(arena);
                plugin.messages().send(sender, "<green>Arena <white>" + arena.name() + "</white> VIP: <white>" + arena.vip() + "</white>.");
            }
            case "delete" -> {
                if (arena.isBusy()) {
                    plugin.messages().send(sender, "<red>That arena is currently in use.");
                    return;
                }
                plugin.arenas().delete(arena.name());
                plugin.messages().send(sender, "<yellow>Deleted arena <white>" + arena.name() + "</white>.");
            }
            case "info" -> {
                plugin.messages().send(sender, "<gray>Arena: <white>" + arena.name() + "</white>");
                plugin.messages().send(sender, "<gray>Enabled: <white>" + arena.enabled() + "</white> VIP: <white>" + arena.vip() + "</white> Ready: <white>" + arena.isReady() + "</white> Busy: <white>" + arena.isBusy() + "</white>");
                plugin.messages().send(sender, "<gray>FFA Ready: <white>" + arena.isFfaReady() + "</white> FFA Spawns: <white>" + arena.ffaSpawns().size() + "</white>");
                plugin.messages().send(sender, "<gray>Kits: <white>" + (arena.allowedKits().isEmpty() ? "ALL" : String.join(", ", arena.allowedKits())) + "</white>");
            }
            default -> plugin.messages().send(sender, "<red>Usage: /zffa arena <name> setspawn1|setspawn2|addffaspawn|clearffaspawns|addkit|removekit|vip|enable|disable|delete|info");
        }
    }

    private void handleKit(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().send(sender, "<yellow>/zffa kit create <name> [display]</yellow>");
            plugin.messages().send(sender, "<yellow>/zffa kit list</yellow>");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "create", "save" -> requirePlayer(sender).ifPresent(player -> {
                if (args.length < 3) {
                    plugin.messages().send(player, "<red>Usage: /zffa kit create <name> [display]");
                    return;
                }
                String display = args.length >= 4 ? join(args, 3) : "<white>" + args[2].toLowerCase() + "</white>";
                plugin.kits().saveFromPlayer(player, args[2], display);
                plugin.gui().rebuild();
                plugin.messages().send(player, "<green>Saved kit <white>" + args[2].toLowerCase() + "</white> from your inventory and armor.");
            });
            case "list" -> {
                String kits = plugin.kits().all().stream().map(Kit::id).reduce((a, b) -> a + ", " + b).orElse("none");
                plugin.messages().send(sender, "<gray>Kits: <white>" + kits + "</white>");
            }
            case "delete" -> {
                if (args.length < 3) {
                    plugin.messages().send(sender, "<red>Usage: /zffa kit delete <name>");
                    return;
                }
                if (!plugin.kits().delete(args[2])) {
                    plugin.messages().send(sender, "<red>Kit does not exist.");
                    return;
                }
                plugin.gui().rebuild();
                plugin.messages().send(sender, "<yellow>Deleted kit <white>" + args[2].toLowerCase() + "</white>.");
            }
            case "seticon" -> {
                if (args.length < 4) {
                    plugin.messages().send(sender, "<red>Usage: /zffa kit seticon <name> <material>");
                    return;
                }
                Material material = material(args[3]);
                if (material == null || !plugin.kits().setIcon(args[2], material)) {
                    plugin.messages().send(sender, "<red>Invalid kit or material.");
                    return;
                }
                plugin.gui().rebuild();
                plugin.messages().send(sender, "<green>Updated kit icon.");
            }
            case "setting" -> {
                if (args.length < 5) {
                    plugin.messages().send(sender, "<red>Usage: /zffa kit setting <name> <allow-regen|allow-hunger|speed-multiplier|max-health> <value>");
                    return;
                }
                if (!plugin.kits().setSetting(args[2], args[3], args[4])) {
                    plugin.messages().send(sender, "<red>Invalid kit, setting, or value.");
                    return;
                }
                plugin.messages().send(sender, "<green>Updated kit setting.");
            }
            default -> plugin.messages().send(sender, "<red>Usage: /zffa kit create|save|delete|seticon|setting|list");
        }
    }

    private void help(CommandSender sender) {
        plugin.messages().send(sender, "<yellow>/zffa reload</yellow>");
        plugin.messages().send(sender, "<yellow>/zffa setlobby</yellow>");
        plugin.messages().send(sender, "<yellow>/zffa kit create <name> [display]</yellow>");
        plugin.messages().send(sender, "<yellow>/zffa kit list</yellow>");
        plugin.messages().send(sender, "<yellow>/zffa kit delete|seticon|setting ...</yellow>");
        plugin.messages().send(sender, "<yellow>/zffa arena create <name></yellow>");
        plugin.messages().send(sender, "<yellow>/zffa arena <name> setspawn1|setspawn2</yellow>");
        plugin.messages().send(sender, "<yellow>/zffa debug status|set <true|false></yellow>");
        plugin.messages().send(sender, "<yellow>/zffa voucher give|set|remove <player> <amount></yellow>");
        plugin.messages().send(sender, "<yellow>/zffa killboost give|set|remove <player> <amount></yellow>");
        plugin.messages().send(sender, "<yellow>/zffa streak set <player> <amount></yellow>");
        plugin.messages().send(sender, "<yellow>/zffa streak reset <player></yellow>");
        plugin.messages().send(sender, "<yellow>/zffa arena <name> addffaspawn|clearffaspawns</yellow>");
        plugin.messages().send(sender, "<yellow>/zffa arena <name> addkit|removekit <kit></yellow>");
        plugin.messages().send(sender, "<yellow>/zffa arena <name> enable|disable|delete|info</yellow>");
    }

    private Optional<Player> requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) return Optional.of(player);
        plugin.messages().send(sender, "<red>Player-only command.");
        return Optional.empty();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("zf.admin")) return List.of();
        if (args.length == 1) return filter(List.of("reload", "setlobby", "items", "debug", "voucher", "killboost", "streak", "arena", "kit"), args[0]);
        if (args.length == 2 && "debug".equalsIgnoreCase(args[0])) {
            return filter(List.of("status", "set"), args[1]);
        }
        if (args.length == 2 && "voucher".equalsIgnoreCase(args[0])) {
            return filter(List.of("give", "set", "remove"), args[1]);
        }
        if (args.length == 2 && "killboost".equalsIgnoreCase(args[0])) {
            return filter(List.of("give", "set", "remove"), args[1]);
        }
        if (args.length == 2 && "streak".equalsIgnoreCase(args[0])) {
            return filter(List.of("set", "reset"), args[1]);
        }
        if (args.length == 2 && "arena".equalsIgnoreCase(args[0])) {
            ArrayList<String> values = new ArrayList<>();
            values.add("create");
            values.add("list");
            values.addAll(plugin.arenas().all().stream().map(Arena::name).toList());
            return filter(values, args[1]);
        }
        if (args.length == 3 && "arena".equalsIgnoreCase(args[0]) && !"create".equalsIgnoreCase(args[1])) {
            return filter(List.of("setspawn1", "setspawn2", "addffaspawn", "clearffaspawns", "addkit", "removekit", "vip", "enable", "disable", "delete", "info"), args[2]);
        }
        if (args.length == 4 && "arena".equalsIgnoreCase(args[0]) && (args[2].equalsIgnoreCase("addkit") || args[2].equalsIgnoreCase("removekit"))) {
            return filter(plugin.kits().all().stream().map(Kit::id).toList(), args[3]);
        }
        if (args.length == 2 && "kit".equalsIgnoreCase(args[0])) {
            return filter(List.of("create", "save", "list", "delete", "seticon", "setting"), args[1]);
        }
        if (args.length == 3 && "kit".equalsIgnoreCase(args[0]) && (args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("seticon") || args[1].equalsIgnoreCase("setting"))) {
            return filter(plugin.kits().all().stream().map(Kit::id).toList(), args[2]);
        }
        if (args.length == 4 && "kit".equalsIgnoreCase(args[0]) && args[1].equalsIgnoreCase("setting")) {
            return filter(List.of("allow-regen", "allow-hunger", "speed-multiplier", "max-health"), args[3]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String token) {
        return values.stream().filter(value -> value.startsWith(token.toLowerCase())).toList();
    }

    private String join(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(args[i]);
        }
        return builder.toString();
    }

    private Material material(String raw) {
        try {
            return Material.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
