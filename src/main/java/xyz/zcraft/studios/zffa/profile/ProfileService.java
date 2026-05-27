package xyz.zcraft.studios.zffa.profile;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import xyz.zcraft.studios.zffa.ZFfaPlugin;
import xyz.zcraft.studios.zffa.database.StorageEngine;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

public final class ProfileService {
    private final ZFfaPlugin plugin;
    private final StorageEngine storage;
    private final Cache<UUID, PlayerProfile> cache;
    private BukkitTask autoSaveTask;

    public ProfileService(ZFfaPlugin plugin, StorageEngine storage) {
        this.plugin = plugin;
        this.storage = storage;
        int minutes = plugin.getConfig().getInt("settings.cache-expire-minutes", 20);
        this.cache = Caffeine.newBuilder().expireAfterAccess(Duration.ofMinutes(minutes)).build();
    }

    public void load(Player player) {
        storage.loadProfile(player.getUniqueId(), player.getName()).thenAccept(profile -> {
            profile.updateName(player.getName());
            cache.put(player.getUniqueId(), profile);
        });
    }

    public Optional<PlayerProfile> get(UUID uuid) {
        return Optional.ofNullable(cache.getIfPresent(uuid));
    }

    public PlayerProfile getOrCreate(Player player) {
        return cache.get(player.getUniqueId(), uuid -> PlayerProfile.fresh(uuid, player.getName()));
    }

    public void save(Player player, boolean remove) {
        PlayerProfile profile = cache.getIfPresent(player.getUniqueId());
        if (profile == null) return;
        storage.saveProfile(profile);
        if (remove) cache.invalidate(player.getUniqueId());
    }

    public void startAutoSave() {
        long period = Math.max(1, plugin.getConfig().getInt("settings.autosave-minutes", 5)) * 60L * 20L;
        autoSaveTask = Bukkit.getScheduler().runTaskTimer(plugin, this::flushDirty, period, period);
    }

    public void flushDirty() {
        for (PlayerProfile profile : cache.asMap().values()) {
            if (profile.markCleanIfDirty()) {
                storage.saveProfile(profile);
            }
        }
    }

    public void saveAllNow() {
        if (autoSaveTask != null) autoSaveTask.cancel();
        for (PlayerProfile profile : cache.asMap().values()) {
            storage.saveProfile(profile).join();
        }
    }

    public Collection<PlayerProfile> topCached(int limit) {
        ArrayList<PlayerProfile> profiles = new ArrayList<>(cache.asMap().values());
        profiles.sort(Comparator.comparingInt(PlayerProfile::elo).reversed());
        return profiles.stream().limit(limit).toList();
    }
}
