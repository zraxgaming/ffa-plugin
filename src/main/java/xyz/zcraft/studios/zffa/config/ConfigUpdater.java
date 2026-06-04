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
        for (String resource : List.of("config.yml", "messages.yml", "menus.yml", "kits.yml")) {
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
                "config.yml", List.of("settings.kill-boost", "settings.streak.protection", "lobby-items.cosmetics"),
                "messages.yml", List.of("ffa.kill-boost-activated", "duel.voucher.used"),
                "menus.yml", List.of("menus.cosmetics", "menus.armor-trims", "menus.main.items.cosmetics", "menus.kill-effects")
        );
        int changed = 0;
        for (String path : obsolete.getOrDefault(resource, List.of())) {
            if (!current.contains(path)) continue;
            current.set(path, null);
            changed++;
        }
        if ("config.yml".equals(resource) && containsAny(current.getStringList("lobby-items.stats.lore"), "killboost", "Kill Boost", "voucher", "%vouchers%")) {
            current.set("lobby-items.stats.lore", List.of(
                    "<gray>Elo: <white>%elo%</white>",
                    "<gray>Rank: <white>%rank%</white>",
                    "<gray>Wins: <white>%wins%</white> <dark_gray>|</dark_gray> <gray>Losses: <white>%losses%</white>",
                    "<gray>Kills: <white>%kills%</white> <dark_gray>|</dark_gray> <gray>Deaths: <white>%deaths%</white>",
                    "<gray>Streak: <white>%streak%</white>"
            ));
            changed++;
        }
        if ("config.yml".equals(resource)) {
            long menuRefresh = current.getLong("settings.menu-refresh-seconds", 30L);
            if (menuRefresh > 0L && menuRefresh < 30L) {
                current.set("settings.menu-refresh-seconds", 30L);
                changed++;
            }
        }
        if ("menus.yml".equals(resource)) {
            List<Integer> defaultKitSlots = List.of(10, 11, 12, 13, 14, 15, 16, 20, 21, 22, 23, 24);
            if (current.getIntegerList("menus.kit-selector.item-slots").isEmpty()) {
                current.set("menus.kit-selector.item-slots", defaultKitSlots);
                changed++;
            }
            if (current.getIntegerList("menus.kit-selector-unranked.item-slots").isEmpty()) {
                current.set("menus.kit-selector-unranked.item-slots", defaultKitSlots);
                changed++;
            }
            if (current.getInt("menus.kit-selector.size", 27) < 36) {
                current.set("menus.kit-selector.size", 36);
                changed++;
            }
            if (current.getInt("menus.kit-selector-unranked.size", 27) < 36) {
                current.set("menus.kit-selector-unranked.size", 36);
                changed++;
            }
            if (current.getInt("menus.kit-editor.size", 27) < 36) {
                current.set("menus.kit-editor.size", 36);
                changed++;
            }
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
