package xyz.zcraft.studios.zffa.profile;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xyz.zcraft.studios.zffa.ZFfaPlugin;
import xyz.zcraft.studios.zffa.database.StorageEngine;
import xyz.zcraft.studios.zffa.platform.ScheduledTaskHandle;

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
    private ScheduledTaskHandle autoSaveTask;

    public ProfileService(ZFfaPlugin plugin, StorageEngine storage) {
        this.plugin = plugin;
        this.storage = storage;
        int minutes = plugin.getConfig().getInt("settings.cache-expire-minutes", 20);
        this.cache = Caffeine.newBuilder().expireAfterAccess(Duration.ofMinutes(minutes)).build();
    }

    public void load(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        storage.loadProfile(player.getUniqueId(), player.getName()).thenAccept(profile -> {
            plugin.scheduler().run(() -> {
                Player online = Bukkit.getPlayer(uuid);
                if (online == null || !online.getName().equals(name)) return;
                profile.updateName(online.getName());
                cache.put(uuid, profile);
            });
        });
    }

    public Optional<PlayerProfile> get(UUID uuid) {
        return Optional.ofNullable(cache.getIfPresent(uuid));
    }

    public PlayerProfile getOrCreate(Player player) {
        int startElo = plugin.getConfig().getInt("settings.elo-start", 0);
        return cache.get(player.getUniqueId(), uuid -> PlayerProfile.fresh(uuid, player.getName(), startElo));
    }

    public void save(Player player, boolean remove) {
        PlayerProfile profile = cache.getIfPresent(player.getUniqueId());
        if (profile == null) return;
        storage.saveProfile(profile);
        if (remove) cache.invalidate(player.getUniqueId());
    }

    public void startAutoSave() {
        long period = Math.max(1, plugin.getConfig().getInt("settings.autosave-minutes", 5)) * 60L * 20L;
        autoSaveTask = plugin.scheduler().runTimer(this::flushDirty, period, period);
    }

    public void flushDirty() {
        ArrayList<PlayerProfile> dirty = new ArrayList<>();
        for (PlayerProfile profile : cache.asMap().values()) {
            if (profile.markCleanIfDirty()) {
                dirty.add(profile);
            }
        }
        if (!dirty.isEmpty()) storage.saveProfiles(dirty);
    }

    public void saveAllNow() {
        if (autoSaveTask != null) autoSaveTask.cancel();
        storage.saveProfiles(new ArrayList<>(cache.asMap().values())).join();
    }

    public Collection<PlayerProfile> topCached(int limit) {
        ArrayList<PlayerProfile> profiles = new ArrayList<>(cache.asMap().values());
        profiles.sort(Comparator.comparingInt(PlayerProfile::elo).reversed());
        return profiles.stream().limit(limit).toList();
    }
}
