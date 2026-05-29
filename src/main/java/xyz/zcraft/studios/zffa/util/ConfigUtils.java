package xyz.zcraft.studios.zffa.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.zcraft.studios.zffa.ZFfaPlugin;

import java.util.List;
import java.util.Locale;

/**
 * Utility class for configuration validation and fallback logic.
 * Provides safe methods to retrieve config values with sensible defaults.
 */
public final class ConfigUtils {
    private static ZFfaPlugin plugin;

    public ConfigUtils(ZFfaPlugin plugin) {
        ConfigUtils.plugin = plugin;
    }

    /**
     * Gets a string value from config with fallback
     */
    public static String getString(ConfigurationSection section, String key, String fallback) {
        try {
            if (section == null) return fallback;
            String value = section.getString(key);
            return value != null && !value.isBlank() ? value : fallback;
        } catch (Exception e) {
            logError("getString", key, e);
            return fallback;
        }
    }

    /**
     * Gets an int value from config with fallback
     */
    public static int getInt(ConfigurationSection section, String key, int fallback) {
        try {
            if (section == null) return fallback;
            return section.contains(key) ? section.getInt(key) : fallback;
        } catch (Exception e) {
            logError("getInt", key, e);
            return fallback;
        }
    }

    /**
     * Gets a double value from config with fallback
     */
    public static double getDouble(ConfigurationSection section, String key, double fallback) {
        try {
            if (section == null) return fallback;
            return section.contains(key) ? section.getDouble(key) : fallback;
        } catch (Exception e) {
            logError("getDouble", key, e);
            return fallback;
        }
    }

    /**
     * Gets a boolean value from config with fallback
     */
    public static boolean getBoolean(ConfigurationSection section, String key, boolean fallback) {
        try {
            if (section == null) return fallback;
            return section.contains(key) ? section.getBoolean(key) : fallback;
        } catch (Exception e) {
            logError("getBoolean", key, e);
            return fallback;
        }
    }

    /**
     * Gets a list from config with fallback
     */
    public static List<String> getStringList(ConfigurationSection section, String key, List<String> fallback) {
        try {
            if (section == null) return fallback;
            List<String> value = section.getStringList(key);
            return value != null && !value.isEmpty() ? value : fallback;
        } catch (Exception e) {
            logError("getStringList", key, e);
            return fallback;
        }
    }

    /**
     * Gets a ConfigurationSection with null safety
     */
    public static ConfigurationSection getSection(ConfigurationSection section, String key) {
        try {
            if (section == null) return null;
            return section.getConfigurationSection(key);
        } catch (Exception e) {
            logError("getSection", key, e);
            return null;
        }
    }

    /**
     * Safely validates and returns a Material
     */
    public static Material getMaterial(String name, Material fallback) {
        try {
            if (name == null || name.isBlank()) return fallback;
            return Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            if (plugin != null) {
                plugin.debug("Invalid material: " + name + ", using fallback: " + fallback.name());
            }
            return fallback;
        }
    }

    /**
     * Safely creates an ItemStack with fallback
     */
    public static ItemStack createItem(Material material, int amount) {
        try {
            if (material == null || material == Material.AIR) {
                material = Material.STONE;
            }
            ItemStack item = new ItemStack(material, Math.max(1, amount));
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                meta = Bukkit.getItemFactory().getItemMeta(item.getType());
                if (meta != null) {
                    item.setItemMeta(meta);
                }
            }
            return item;
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().warning("Error creating item: " + e.getMessage());
            }
            return new ItemStack(Material.STONE);
        }
    }

    /**
     * Safely gets ItemMeta with creation fallback
     */
    public static ItemMeta getOrCreateMeta(ItemStack item) {
        try {
            if (item == null) return null;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                meta = Bukkit.getItemFactory().getItemMeta(item.getType());
            }
            return meta;
        } catch (Exception e) {
            if (plugin != null) {
                plugin.debug("Error getting ItemMeta: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * Validates slot number is within inventory range
     */
    public static boolean isValidSlot(int slot) {
        return slot >= 0 && slot <= 35;
    }

    /**
     * Helper method to log errors consistently
     */
    private static void logError(String method, String key, Exception e) {
        if (plugin != null) {
            plugin.debug("ConfigUtils." + method + "(" + key + ") error: " + e.getMessage());
        }
    }
}
