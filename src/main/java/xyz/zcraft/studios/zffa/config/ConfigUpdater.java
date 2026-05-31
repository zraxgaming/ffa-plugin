package xyz.zcraft.studios.zffa.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.zcraft.studios.zffa.ZFfaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ConfigUpdater {
    private final ZFfaPlugin plugin;

    public ConfigUpdater(ZFfaPlugin plugin) {
        this.plugin = plugin;
    }

    public void updateDefaults() {
        if (!plugin.getConfig().getBoolean("settings.auto-update-configs", true)) return;
        for (String resource : List.of("config.yml", "messages.yml", "menus.yml", "cosmetics.yml", "kits.yml")) {
            mergeDefaults(resource);
        }
    }

    private void mergeDefaults(String resource) {
        File file = new File(plugin.getDataFolder(), resource);
        if (!file.exists()) {
            plugin.saveResource(resource, false);
            return;
        }
        try (InputStream stream = plugin.getResource(resource)) {
            if (stream == null) return;
            YamlConfiguration current = YamlConfiguration.loadConfiguration(file);
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
            int added = mergeSection(current, defaults, "");
            if (added > 0) {
                current.save(file);
                plugin.getLogger().info("Updated " + resource + " with " + added + " missing default setting(s). Existing values were kept.");
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Unable to update " + resource + ": " + exception.getMessage());
        }
    }

    private int mergeSection(YamlConfiguration current, ConfigurationSection defaults, String prefix) {
        int added = 0;
        for (String key : defaults.getKeys(false)) {
            String path = prefix.isBlank() ? key : prefix + "." + key;
            Object value = defaults.get(key);
            if (value instanceof ConfigurationSection section) {
                if (!current.isConfigurationSection(path)) {
                    current.createSection(path);
                }
                added += mergeSection(current, section, path);
            } else if (!current.contains(path)) {
                current.set(path, value);
                added++;
            }
        }
        return added;
    }
}
