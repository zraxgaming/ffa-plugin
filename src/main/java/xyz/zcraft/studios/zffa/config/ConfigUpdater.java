package xyz.zcraft.studios.zffa.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.zcraft.studios.zffa.ZFfaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

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
            int removed = migrateObsolete(resource, current);
            if (added > 0 || removed > 0) {
                current.save(file);
                plugin.getLogger().info("Updated " + resource + " with " + added + " missing default setting(s) and refreshed " + removed + " obsolete setting(s). Existing values were kept where possible.");
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

    private int migrateObsolete(String resource, YamlConfiguration current) {
        Map<String, List<String>> obsolete = Map.of(
                "config.yml", List.of("settings.kill-boost"),
                "messages.yml", List.of("ffa.kill-boost-activated"),
                "menus.yml", List.of("menus.cosmetics", "menus.armor-trims"),
                "cosmetics.yml", List.of("armor-trims")
        );
        int changed = 0;
        for (String path : obsolete.getOrDefault(resource, List.of())) {
            if (!current.contains(path)) continue;
            current.set(path, null);
            changed++;
        }
        if ("config.yml".equals(resource) && containsAny(current.getStringList("lobby-items.stats.lore"), "killboost", "Kill Boost")) {
            current.set("lobby-items.stats.lore", List.of(
                    "<gray>Elo: <white>%elo%</white>",
                    "<gray>Rank: <white>%rank%</white>",
                    "<gray>Wins: <white>%wins%</white> <dark_gray>|</dark_gray> <gray>Losses: <white>%losses%</white>",
                    "<gray>Kills: <white>%kills%</white> <dark_gray>|</dark_gray> <gray>Deaths: <white>%deaths%</white>",
                    "<gray>Streak: <white>%streak%</white>",
                    "<gray>Vouchers: <white>%vouchers%</white>"
            ));
            changed++;
        }
        if ("config.yml".equals(resource) && "AMETHYST_SHARD".equalsIgnoreCase(current.getString("lobby-items.cosmetics.material", ""))) {
            current.set("lobby-items.cosmetics.material", "FIREWORK_STAR");
            current.set("lobby-items.cosmetics.name", "<gold>Kill Effects</gold>");
            changed++;
        }
        if ("menus.yml".equals(resource) && containsAny(current.getStringList("menus.main.items.cosmetics.lore"), "selected_armor_trim", "armor trim", "armor trims")) {
            current.set("menus.main.items.cosmetics.lore", List.of(
                    "<gray>Choose your kill effect.",
                    "<dark_gray>Selected: <white>%selected_kill_effect%</white>"
            ));
            changed++;
        }
        if ("menus.yml".equals(resource) && "AMETHYST_SHARD".equalsIgnoreCase(current.getString("menus.main.items.cosmetics.material", ""))) {
            current.set("menus.main.items.cosmetics.material", "FIREWORK_STAR");
            current.set("menus.main.items.cosmetics.name", "<gold>Kill Effects</gold>");
            changed++;
        }
        return changed;
    }

    private boolean containsAny(List<String> values, String... needles) {
        for (String value : values) {
            for (String needle : needles) {
                if (value.toLowerCase(java.util.Locale.ROOT).contains(needle.toLowerCase(java.util.Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }
}
