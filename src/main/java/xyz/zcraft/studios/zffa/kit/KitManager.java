package xyz.zcraft.studios.zffa.kit;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.potion.PotionEffect;
import xyz.zcraft.studios.zffa.ZFfaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class KitManager {
    private final ZFfaPlugin plugin;
    private final Map<String, Kit> kits = new LinkedHashMap<>();

    public KitManager(ZFfaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        kits.clear();
        // Try to load from separate kits.yml first
        ConfigurationSection root = loadKitsConfig();
        if (root == null) {
            plugin.debug("Failed to load kits - configuration section is null");
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;
            Material icon = material(section.getString("icon", "CHEST"), Material.CHEST);
            ArrayList<ItemStack> items = new ArrayList<>();
            if (section.isList("inventory")) {
                for (ItemStack stack : itemStackList(section.getList("inventory"))) {
                    if (stack != null && !stack.getType().isAir()) items.add(stack);
                }
            } else {
                for (String raw : section.getStringList("items")) {
                    ItemStack stack = parseLegacyItem(raw);
                    if (stack == null) continue;
                    if (stack.getMaxStackSize() == 1 && stack.getAmount() > 1) {
                        int amount = stack.getAmount();
                        stack.setAmount(1);
                        for (int i = 0; i < amount; i++) items.add(stack.clone());
                    } else {
                        items.add(stack);
                    }
                }
            }
            ConfigurationSection armor = section.getConfigurationSection("armor");
            Kit kit = new Kit(
                    id.toLowerCase(Locale.ROOT),
                    plugin.messages().parse(section.getString("display", id)),
                    icon,
                    section.getBoolean("settings.allow-regen", true),
                    section.getBoolean("settings.allow-hunger", true),
                    section.getDouble("settings.speed-multiplier", 1.0D),
                    section.getDouble("settings.max-health", 20.0D),
                    items,
                    armorItem(armor, "helmet"),
                    armorItem(armor, "chestplate"),
                    armorItem(armor, "leggings"),
                    armorItem(armor, "boots"),
                    potionEffects(section.getList("effects"))
            );
            kits.put(kit.id(), kit);
        }
    }

    public Collection<Kit> all() {
        return kits.values();
    }

    public Optional<Kit> get(String id) {
        return Optional.ofNullable(kits.get(id.toLowerCase(Locale.ROOT)));
    }

    public boolean exists(String id) {
        return kits.containsKey(id.toLowerCase(Locale.ROOT));
    }

    public boolean delete(String id) {
        String key = id.toLowerCase(Locale.ROOT);
        if (!kits.containsKey(key)) return false;
        YamlConfiguration kitsConfig = loadKitsYaml();
        if (kitsConfig == null) return false;
        kitsConfig.set(key, null);
        return saveKitsYaml(kitsConfig);
    }

    public boolean setIcon(String id, Material material) {
        String key = id.toLowerCase(Locale.ROOT);
        if (!kits.containsKey(key)) return false;
        YamlConfiguration kitsConfig = loadKitsYaml();
        if (kitsConfig == null) return false;
        kitsConfig.set(key + ".icon", material.name());
        return saveKitsYaml(kitsConfig);
    }

    public boolean setSetting(String id, String setting, String value) {
        String key = id.toLowerCase(Locale.ROOT);
        if (!kits.containsKey(key)) return false;
        YamlConfiguration kitsConfig = loadKitsYaml();
        if (kitsConfig == null) return false;
        String path = key + ".settings." + setting.toLowerCase(Locale.ROOT);
        switch (setting.toLowerCase(Locale.ROOT)) {
            case "allow-regen", "allow-hunger" -> kitsConfig.set(path, Boolean.parseBoolean(value));
            case "speed-multiplier", "max-health" -> {
                try {
                    kitsConfig.set(path, Double.parseDouble(value));
                } catch (NumberFormatException exception) {
                    return false;
                }
            }
            default -> {
                return false;
            }
        }
        return saveKitsYaml(kitsConfig);
    }

    public void saveFromPlayer(Player player, String id, String display) {
        String key = id.toLowerCase(Locale.ROOT);
        YamlConfiguration kitsConfig = loadKitsYaml();
        if (kitsConfig == null) return;
        kitsConfig.set(key + ".display", display);
        kitsConfig.set(key + ".icon", firstInventoryMaterial(player).name());
        kitsConfig.set(key + ".settings.allow-regen", true);
        kitsConfig.set(key + ".settings.allow-hunger", true);
        kitsConfig.set(key + ".settings.speed-multiplier", 1.0D);
        kitsConfig.set(key + ".settings.max-health", player.getMaxHealth());
        kitsConfig.set(key + ".inventory", Arrays.stream(player.getInventory().getStorageContents())
                .filter(stack -> stack != null && !stack.getType().isAir())
                .map(ItemStack::clone)
                .toList());
        kitsConfig.set(key + ".armor.helmet", cloneOrNull(player.getInventory().getHelmet()));
        kitsConfig.set(key + ".armor.chestplate", cloneOrNull(player.getInventory().getChestplate()));
        kitsConfig.set(key + ".armor.leggings", cloneOrNull(player.getInventory().getLeggings()));
        kitsConfig.set(key + ".armor.boots", cloneOrNull(player.getInventory().getBoots()));
        kitsConfig.set(key + ".effects", new ArrayList<>(player.getActivePotionEffects()));
        saveKitsYaml(kitsConfig);
    }

    private ConfigurationSection loadKitsConfig() {
        YamlConfiguration config = loadKitsYaml();
        if (config == null) return null;
        return config.getConfigurationSection("kits");
    }

    private YamlConfiguration loadKitsYaml() {
        try {
            File file = new File(plugin.getDataFolder(), "kits.yml");
            if (!file.exists()) {
                plugin.saveResource("kits.yml", false);
            }
            return YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load kits.yml: " + e.getMessage());
            return null;
        }
    }

    private boolean saveKitsYaml(YamlConfiguration config) {
        try {
            config.save(new File(plugin.getDataFolder(), "kits.yml"));
            reload();
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save kits.yml: " + e.getMessage());
            return false;
        }
    }

    private List<PotionEffect> potionEffects(List<?> raw) {
        ArrayList<PotionEffect> effects = new ArrayList<>();
        if (raw == null) return effects;
        for (Object value : raw) {
            if (value instanceof PotionEffect effect) effects.add(effect);
        }
        return effects;
    }

    private ItemStack armorItem(ConfigurationSection armor, String key) {
        if (armor == null) return null;
        Object value = armor.get(key);
        if (value instanceof ItemStack stack) return stack;
        return parseLegacyItem(armor.getString(key, ""));
    }

    private List<ItemStack> itemStackList(List<?> raw) {
        ArrayList<ItemStack> stacks = new ArrayList<>();
        if (raw == null) return stacks;
        for (Object value : raw) {
            if (value instanceof ItemStack stack) stacks.add(stack.clone());
        }
        return stacks;
    }

    private ItemStack parseLegacyItem(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String[] parts = raw.split(":");
        Material material = material(parts[0], Material.STONE);
        int amount = 1;
        if (parts.length > 1) {
            try {
                amount = Integer.parseInt(parts[parts.length - 1]);
            } catch (NumberFormatException ignored) {
                amount = 1;
            }
        }
        ItemStack stack = new ItemStack(material, Math.max(1, amount));
        if (material == Material.SPLASH_POTION && parts.length > 1 && "healing".equalsIgnoreCase(parts[1])) {
            PotionMeta meta = (PotionMeta) stack.getItemMeta();
            meta.setBasePotionType(PotionType.HEALING);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private Material firstInventoryMaterial(Player player) {
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && !stack.getType().isAir()) return stack.getType();
        }
        return Material.CHEST;
    }

    private ItemStack cloneOrNull(ItemStack stack) {
        return stack == null || stack.getType().isAir() ? null : stack.clone();
    }

    private Material material(String name, Material fallback) {
        try {
            return Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
