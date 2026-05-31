package xyz.zcraft.studios.zffa.cosmetic;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.scheduler.BukkitTask;
import xyz.zcraft.studios.zffa.ZFfaPlugin;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class CosmeticsManager {
    private final ZFfaPlugin plugin;
    private final Map<String, KillEffect> killEffects = new LinkedHashMap<>();
    private final Map<String, ArmorTrimCosmetic> armorTrims = new LinkedHashMap<>();
    private final Map<UUID, String> selectedKillEffects = new LinkedHashMap<>();
    private final Map<UUID, String> selectedArmorTrims = new LinkedHashMap<>();
    private YamlConfiguration config;
    private File file;
    private BukkitTask pendingSave;

    public record KillEffect(String id, String display, Material icon, Particle particle, Sound sound, int count) {
    }

    public record ArmorTrimCosmetic(String id, String display, Material icon, TrimPattern pattern, TrimMaterial material) {
    }

    public CosmeticsManager(ZFfaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        saveNow();
        killEffects.clear();
        armorTrims.clear();
        selectedKillEffects.clear();
        selectedArmorTrims.clear();
        file = new File(plugin.getDataFolder(), "cosmetics.yml");
        if (!file.exists()) {
            plugin.saveResource("cosmetics.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        loadKillEffects();
        loadArmorTrims();
        loadSelections();
    }

    public Collection<KillEffect> killEffects() {
        return killEffects.values();
    }

    public Collection<ArmorTrimCosmetic> armorTrims() {
        return armorTrims.values();
    }

    public Optional<KillEffect> killEffect(String id) {
        return Optional.ofNullable(killEffects.get(normalize(id)));
    }

    public Optional<ArmorTrimCosmetic> armorTrim(String id) {
        return Optional.ofNullable(armorTrims.get(normalize(id)));
    }

    public String selectedKillEffect(Player player) {
        return selectedKillEffects.getOrDefault(player.getUniqueId(), "none");
    }

    public String selectedArmorTrim(Player player) {
        return selectedArmorTrims.getOrDefault(player.getUniqueId(), "none");
    }

    public boolean canUseKillEffect(Player player, String id) {
        String key = normalize(id);
        return "none".equals(key)
                || player.hasPermission("zf.cosmetic.*")
                || player.hasPermission("zf.cosmetic.killeffect." + key)
                || player.hasPermission("zf.cosmetic.killeffect.*");
    }

    public boolean canUseArmorTrim(Player player, String id) {
        String key = normalize(id);
        return "none".equals(key)
                || player.hasPermission("zf.cosmetic.*")
                || player.hasPermission("zf.cosmetic.armortrim." + key)
                || player.hasPermission("zf.cosmetic.armortrim.*");
    }

    public boolean selectKillEffect(Player player, String id) {
        String key = normalize(id);
        if (!killEffects.containsKey(key) || !canUseKillEffect(player, key)) return false;
        selectedKillEffects.put(player.getUniqueId(), key);
        saveSelection(player.getUniqueId(), "kill-effect", key);
        return true;
    }

    public boolean selectArmorTrim(Player player, String id) {
        String key = normalize(id);
        if (!armorTrims.containsKey(key) || !canUseArmorTrim(player, key)) return false;
        selectedArmorTrims.put(player.getUniqueId(), key);
        saveSelection(player.getUniqueId(), "armor-trim", key);
        if ("none".equals(key)) {
            clearArmorTrim(player);
        } else {
            applyArmorTrim(player);
        }
        return true;
    }

    public void playKillEffect(Player killer, Player victim) {
        if (killer == null || victim == null) return;
        KillEffect effect = killEffects.get(selectedKillEffect(killer));
        if (effect == null || "none".equals(effect.id())) return;
        if (!canUseKillEffect(killer, effect.id())) return;
        victim.getWorld().spawnParticle(effect.particle(), victim.getLocation().add(0, 1, 0), Math.max(1, effect.count()), 0.35, 0.45, 0.35, 0.02);
        if (effect.sound() != null) {
            victim.getWorld().playSound(victim.getLocation(), effect.sound(), 1.0F, 1.0F);
        }
    }

    public void applyArmorTrim(Player player) {
        ArmorTrimCosmetic cosmetic = armorTrims.get(selectedArmorTrim(player));
        if (cosmetic == null || "none".equals(cosmetic.id()) || !canUseArmorTrim(player, cosmetic.id())) return;
        ArmorTrim trim = new ArmorTrim(cosmetic.material(), cosmetic.pattern());
        ItemStack[] armor = player.getInventory().getArmorContents();
        boolean changed = false;
        for (int i = 0; i < armor.length; i++) {
            ItemStack piece = armor[i];
            if (piece == null || piece.getType().isAir()) continue;
            if (!(piece.getItemMeta() instanceof ArmorMeta meta)) continue;
            meta.setTrim(trim);
            piece.setItemMeta(meta);
            armor[i] = piece;
            changed = true;
        }
        if (changed) {
            player.getInventory().setArmorContents(armor);
        }
    }

    private void clearArmorTrim(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        boolean changed = false;
        for (int i = 0; i < armor.length; i++) {
            ItemStack piece = armor[i];
            if (piece == null || piece.getType().isAir()) continue;
            if (!(piece.getItemMeta() instanceof ArmorMeta meta) || !meta.hasTrim()) continue;
            meta.setTrim(null);
            piece.setItemMeta(meta);
            armor[i] = piece;
            changed = true;
        }
        if (changed) {
            player.getInventory().setArmorContents(armor);
        }
    }

    private void loadKillEffects() {
        ConfigurationSection root = config.getConfigurationSection("kill-effects");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;
            String key = normalize(id);
            Particle particle = particle(section.getString("particle", "POOF"));
            Sound sound = sound(section.getString("sound", ""));
            killEffects.put(key, new KillEffect(
                    key,
                    section.getString("display", id),
                    material(section.getString("icon", "FIREWORK_STAR"), Material.FIREWORK_STAR),
                    particle,
                    sound,
                    section.getInt("count", 24)
            ));
        }
    }

    private void loadArmorTrims() {
        ConfigurationSection root = config.getConfigurationSection("armor-trims");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;
            TrimPattern pattern = trimPattern(section.getString("pattern", "sentry"));
            TrimMaterial material = trimMaterial(section.getString("trim-material", "diamond"));
            if (pattern == null || material == null) continue;
            String key = normalize(id);
            armorTrims.put(key, new ArmorTrimCosmetic(
                    key,
                    section.getString("display", id),
                    material(section.getString("icon", "DIAMOND_CHESTPLATE"), Material.DIAMOND_CHESTPLATE),
                    pattern,
                    material
            ));
        }
    }

    private void loadSelections() {
        ConfigurationSection root = config.getConfigurationSection("players");
        if (root == null) return;
        for (String rawUuid : root.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(rawUuid);
                selectedKillEffects.put(uuid, normalize(root.getString(rawUuid + ".kill-effect", "none")));
                selectedArmorTrims.put(uuid, normalize(root.getString(rawUuid + ".armor-trim", "none")));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void saveSelection(UUID uuid, String type, String id) {
        config.set("players." + uuid + "." + type, id);
        queueSave();
    }

    public void saveNow() {
        if (pendingSave != null) {
            pendingSave.cancel();
            pendingSave = null;
        }
        if (config == null || file == null) return;
        try {
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save cosmetic selection: " + e.getMessage());
        }
    }

    private void queueSave() {
        if (pendingSave != null) return;
        pendingSave = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pendingSave = null;
            saveNow();
        }, 40L);
    }

    private Particle particle(String raw) {
        try {
            return Particle.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return Particle.POOF;
        }
    }

    private Sound sound(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Sound.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return null;
        }
    }

    private TrimPattern trimPattern(String raw) {
        return Registry.TRIM_PATTERN.get(NamespacedKey.minecraft(normalize(raw)));
    }

    private TrimMaterial trimMaterial(String raw) {
        return Registry.TRIM_MATERIAL.get(NamespacedKey.minecraft(normalize(raw)));
    }

    private Material material(String raw, Material fallback) {
        try {
            return Material.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String normalize(String raw) {
        if (raw == null || raw.isBlank()) return "none";
        return raw.toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
