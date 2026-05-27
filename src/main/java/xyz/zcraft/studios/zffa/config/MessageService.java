package xyz.zcraft.studios.zffa.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import xyz.zcraft.studios.zffa.ZFfaPlugin;

public final class MessageService {
    private final ZFfaPlugin plugin;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private String prefix;

    public MessageService(ZFfaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.prefix = plugin.getConfig().getString("settings.prefix", "<aqua>Z-FFA</aqua> <dark_gray>|</dark_gray> ");
    }

    public Component parse(String text) {
        return mini.deserialize(text == null ? "" : text);
    }

    public void send(CommandSender sender, String message) {
        sender.sendMessage(parse(prefix + message));
    }
}
