package xyz.zcraft.studios.zffa.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.zcraft.studios.zffa.ZFfaPlugin;

import java.util.Locale;

public final class LeaveCommand implements CommandExecutor {
    private final ZFfaPlugin plugin;

    public LeaveCommand(ZFfaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "command.player-only", "<red>Player-only command.");
            return true;
        }

        String commandName = command.getName().toLowerCase(Locale.ROOT);
        switch (commandName) {
            case "leavequeue" -> leaveQueue(player);
            case "leaveparty" -> leaveParty(player);
            case "leave" -> leaveAny(player);
            default -> plugin.messages().send(player, "leave.unknown", "<red>Unknown command.");
        }
        return true;
    }

    private void leaveQueue(Player player) {
        String before = plugin.queues().status(player.getUniqueId());
        plugin.queues().leave(player.getUniqueId());
        if (before != null && before.toLowerCase(Locale.ROOT).startsWith("queued")) {
            plugin.messages().send(player, "queue.left", "<yellow>You left the queue.");
        } else {
            plugin.messages().send(player, "queue.not-queued", "<gray>You are not currently queued.");
        }
    }

    private void leaveParty(Player player) {
        if (plugin.parties().party(player.getUniqueId()).isEmpty()) {
            plugin.messages().send(player, "party.not-in-party", "<red>You are not in a party.");
            return;
        }
        plugin.parties().leave(player, true);
        plugin.messages().send(player, "party.left-self", "<yellow>You left the party.");
    }

    private void leaveAny(Player player) {
        if (plugin.matches().isInMatch(player.getUniqueId())) {
            plugin.queues().leave(player.getUniqueId());
            plugin.matches().forfeit(player, "Forfeit");
            plugin.messages().send(player, "leave.match", "<yellow>You left the match.");
            return;
        }

        if (plugin.ffa().isInFfa(player.getUniqueId())) {
            plugin.ffa().leave(player);
            plugin.messages().send(player, "leave.ffa", "<yellow>You left FFA.");
            return;
        }

        String before = plugin.queues().status(player.getUniqueId());
        if (before != null && before.toLowerCase(Locale.ROOT).startsWith("queued")) {
            plugin.queues().leave(player.getUniqueId());
            plugin.messages().send(player, "queue.left", "<yellow>You left the queue.");
            return;
        }

        plugin.messages().send(player, "leave.nothing", "<gray>Nothing to leave.");
    }
}
