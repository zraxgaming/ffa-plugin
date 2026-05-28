package xyz.zcraft.studios.zffa.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.zcraft.studios.zffa.ZFfaPlugin;

import java.io.File;
import java.util.Map;

public final class MessageService {
    private final ZFfaPlugin plugin;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private YamlConfiguration messages;
    private String prefix;

    public MessageService(ZFfaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        saveDefaultMessages();
        this.messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
        this.prefix = messages.getString("prefix", plugin.getConfig().getString("settings.prefix", "<aqua>Z-FFA</aqua> <dark_gray>|</dark_gray> "));
    }

    private void saveDefaultMessages() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
    }

    public Component parse(String text) {
        return mini.deserialize(text == null ? "" : text);
    }

    public void send(CommandSender sender, String message) {
        sender.sendMessage(parse(prefix + message));
    }

    public void send(CommandSender sender, String path, String fallback) {
        send(sender, path, fallback, Map.of());
    }

    public void send(CommandSender sender, String path, String fallback, Map<String, String> placeholders) {
        String template = messages.getString(path, fallback);
        sender.sendMessage(parse(prefix + replacePlaceholders(template, placeholders)));
    }

    public String get(String path, String fallback) {
        return replacePlaceholders(messages.getString(path, fallback), Map.of());
    }

    private String replacePlaceholders(String text, Map<String, String> placeholders) {
        if (text == null || placeholders == null || placeholders.isEmpty()) return text == null ? "" : text;
        String replaced = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            replaced = replaced.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return replaced;
    }
}
