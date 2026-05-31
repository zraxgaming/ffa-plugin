package xyz.zcraft.studios.zffa.cosmetic;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
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
    private final Map<UUID, String> selectedKillEffects = new LinkedHashMap<>();
    private YamlConfiguration config;
    private File file;
    private BukkitTask pendingSave;

    public record KillEffect(String id, String display, Material icon, Particle particle, Sound sound, int count) {
    }

    public CosmeticsManager(ZFfaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        saveNow();
        killEffects.clear();
        selectedKillEffects.clear();
        file = new File(plugin.getDataFolder(), "cosmetics.yml");
        if (!file.exists()) {
            plugin.saveResource("cosmetics.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        loadKillEffects();
        loadSelections();
    }

    public Collection<KillEffect> killEffects() {
        return killEffects.values();
    }

    public Optional<KillEffect> killEffect(String id) {
        return Optional.ofNullable(killEffects.get(normalize(id)));
    }

    public String selectedKillEffect(Player player) {
        return selectedKillEffects.getOrDefault(player.getUniqueId(), "none");
    }

    public String selectedKillEffect(UUID uuid) {
        return selectedKillEffects.getOrDefault(uuid, "none");
    }

    public boolean canUseKillEffect(Player player, String id) {
        String key = normalize(id);
        return "none".equals(key)
                || player.hasPermission("zf.cosmetic.*")
                || player.hasPermission("zf.cosmetic.killeffect." + key)
                || player.hasPermission("zf.cosmetic.killeffect.*");
    }

    public boolean selectKillEffect(Player player, String id) {
        String key = normalize(id);
        if (!killEffects.containsKey(key) || !canUseKillEffect(player, key)) return false;
        selectedKillEffects.put(player.getUniqueId(), key);
        saveSelection(player.getUniqueId(), "kill-effect", key);
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

    private void loadSelections() {
        ConfigurationSection root = config.getConfigurationSection("players");
        if (root == null) return;
        for (String rawUuid : root.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(rawUuid);
                selectedKillEffects.put(uuid, normalize(root.getString(rawUuid + ".kill-effect", "none")));
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
