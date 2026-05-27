package xyz.zcraft.studios.zffa.kit;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.potion.PotionEffect;
import xyz.zcraft.studios.zffa.ZFfaPlugin;

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
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("kits");
        if (root == null) return;
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
        plugin.getConfig().set("kits." + key, null);
        plugin.saveConfig();
        reload();
        return true;
    }

    public boolean setIcon(String id, Material material) {
        String key = id.toLowerCase(Locale.ROOT);
        if (!kits.containsKey(key)) return false;
        plugin.getConfig().set("kits." + key + ".icon", material.name());
        plugin.saveConfig();
        reload();
        return true;
    }

    public boolean setSetting(String id, String setting, String value) {
        String key = id.toLowerCase(Locale.ROOT);
        if (!kits.containsKey(key)) return false;
        String path = "kits." + key + ".settings." + setting.toLowerCase(Locale.ROOT);
        switch (setting.toLowerCase(Locale.ROOT)) {
            case "allow-regen", "allow-hunger" -> plugin.getConfig().set(path, Boolean.parseBoolean(value));
            case "speed-multiplier", "max-health" -> {
                try {
                    plugin.getConfig().set(path, Double.parseDouble(value));
                } catch (NumberFormatException exception) {
                    return false;
                }
            }
            default -> {
                return false;
            }
        }
        plugin.saveConfig();
        reload();
        return true;
    }

    public void saveFromPlayer(Player player, String id, String display) {
        String key = id.toLowerCase(Locale.ROOT);
        plugin.getConfig().set("kits." + key + ".display", display);
        plugin.getConfig().set("kits." + key + ".icon", firstInventoryMaterial(player).name());
        plugin.getConfig().set("kits." + key + ".settings.allow-regen", true);
        plugin.getConfig().set("kits." + key + ".settings.allow-hunger", true);
        plugin.getConfig().set("kits." + key + ".settings.speed-multiplier", 1.0D);
        plugin.getConfig().set("kits." + key + ".settings.max-health", player.getMaxHealth());
        plugin.getConfig().set("kits." + key + ".inventory", Arrays.stream(player.getInventory().getStorageContents())
                .filter(stack -> stack != null && !stack.getType().isAir())
                .map(ItemStack::clone)
                .toList());
        plugin.getConfig().set("kits." + key + ".armor.helmet", cloneOrNull(player.getInventory().getHelmet()));
        plugin.getConfig().set("kits." + key + ".armor.chestplate", cloneOrNull(player.getInventory().getChestplate()));
        plugin.getConfig().set("kits." + key + ".armor.leggings", cloneOrNull(player.getInventory().getLeggings()));
        plugin.getConfig().set("kits." + key + ".armor.boots", cloneOrNull(player.getInventory().getBoots()));
        plugin.getConfig().set("kits." + key + ".effects", new ArrayList<>(player.getActivePotionEffects()));
        plugin.saveConfig();
        reload();
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
