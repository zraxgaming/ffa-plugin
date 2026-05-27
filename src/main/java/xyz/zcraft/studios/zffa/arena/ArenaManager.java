package xyz.zcraft.studios.zffa.arena;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.zcraft.studios.zffa.ZFfaPlugin;
import xyz.zcraft.studios.zffa.util.LocationCodec;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ArenaManager {
    private final ZFfaPlugin plugin;
    private final File file;
    private YamlConfiguration config;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private Location lobby;

    public ArenaManager(ZFfaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(file);
        this.lobby = LocationCodec.deserialize(config.getString("global-lobby", ""));
        arenas.clear();
        ConfigurationSection root = config.getConfigurationSection("arenas");
        if (root == null) return;
        for (String name : root.getKeys(false)) {
            Arena arena = new Arena(
                    name.toLowerCase(),
                    LocationCodec.deserialize(root.getString(name + ".spawn1", "")),
                    LocationCodec.deserialize(root.getString(name + ".spawn2", "")),
                    root.getStringList(name + ".kits")
            );
            arena.enabled(root.getBoolean(name + ".enabled", true));
            arena.vip(root.getBoolean(name + ".vip", false));
            for (String raw : root.getStringList(name + ".ffa-spawns")) {
                Location location = LocationCodec.deserialize(raw);
                if (location != null) arena.addFfaSpawn(location);
            }
            arenas.put(name.toLowerCase(), arena);
        }
    }

    public Collection<Arena> all() {
        return arenas.values();
    }

    public Optional<Arena> firstAvailable(String kitId) {
        return arenas.values().stream().filter(arena -> arena.claim(kitId)).findFirst();
    }

    public boolean hasReadyArena(String kitId) {
        return arenas.values().stream().anyMatch(arena -> arena.enabled() && arena.isReady() && arena.supportsKit(kitId));
    }

    public boolean hasFreeArena(String kitId) {
        return arenas.values().stream().anyMatch(arena -> arena.enabled() && arena.isReady() && arena.supportsKit(kitId) && !arena.isBusy());
    }

    public Location lobby() {
        return lobby == null ? LocationCodec.deserialize(plugin.getConfig().getString("settings.default-spawn", "")) : lobby;
    }

    public void setLobby(Location location) {
        this.lobby = location;
        config.set("global-lobby", LocationCodec.serialize(location));
        save();
    }

    public Arena arena(String name) {
        return arenas.computeIfAbsent(name.toLowerCase(), key -> new Arena(key, null, null));
    }

    public Optional<Arena> get(String name) {
        return Optional.ofNullable(arenas.get(name.toLowerCase()));
    }

    public boolean exists(String name) {
        return arenas.containsKey(name.toLowerCase());
    }

    public Arena create(String name) {
        String key = name.toLowerCase();
        Arena arena = arenas.computeIfAbsent(key, value -> new Arena(value, null, null));
        saveArena(arena);
        return arena;
    }

    public boolean delete(String name) {
        String key = name.toLowerCase();
        Arena arena = arenas.get(key);
        if (arena == null || arena.isBusy()) return false;
        arenas.remove(key);
        config.set("arenas." + key, null);
        save();
        return true;
    }

    public void saveArena(Arena arena) {
        config.set("arenas." + arena.name() + ".enabled", arena.enabled());
        config.set("arenas." + arena.name() + ".vip", arena.vip());
        config.set("arenas." + arena.name() + ".spawn1", LocationCodec.serialize(arena.spawn1()));
        config.set("arenas." + arena.name() + ".spawn2", LocationCodec.serialize(arena.spawn2()));
        config.set("arenas." + arena.name() + ".ffa-spawns", arena.ffaSpawns().stream().map(LocationCodec::serialize).toList());
        config.set("arenas." + arena.name() + ".kits", arena.allowedKits().stream().sorted().toList());
        save();
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Unable to save arenas.yml: " + exception.getMessage());
        }
    }
}
