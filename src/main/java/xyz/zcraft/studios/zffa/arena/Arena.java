package xyz.zcraft.studios.zffa.arena;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.World;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Arena {
    private final String name;
    private Location spawn1;
    private Location spawn2;
    private boolean enabled;
    private boolean vip;
    private final Set<String> allowedKits;
    private final List<Location> ffaSpawns;
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private static final int CLEANUP_RADIUS = 50; // Cleanup items within 50 blocks of spawns

    public Arena(String name, Location spawn1, Location spawn2) {
        this(name, spawn1, spawn2, List.of());
    }

    public Arena(String name, Location spawn1, Location spawn2, List<String> allowedKits) {
        this.name = name;
        this.spawn1 = spawn1;
        this.spawn2 = spawn2;
        this.enabled = true;
        this.allowedKits = new HashSet<>(allowedKits.stream().map(String::toLowerCase).toList());
        this.ffaSpawns = new ArrayList<>();
    }

    public String name() { return name; }
    public Location spawn1() { return spawn1; }
    public Location spawn2() { return spawn2; }
    public boolean enabled() { return enabled; }
    public boolean vip() { return vip; }
    public Set<String> allowedKits() { return allowedKits; }
    public List<Location> ffaSpawns() { return Collections.unmodifiableList(ffaSpawns); }
    public boolean isReady() { return enabled && spawn1 != null && spawn2 != null; }
    public boolean isFfaReady() { return enabled && !ffaSpawns.isEmpty(); }
    public boolean isBusy() { return busy.get(); }
    public boolean supportsKit(String kitId) { return allowedKits.isEmpty() || allowedKits.contains(kitId.toLowerCase()); }
    public boolean claim(String kitId) { return isReady() && supportsKit(kitId) && busy.compareAndSet(false, true); }
    
    public void release() { 
        busy.set(false);
        cleanup();
    }
    
    public void spawn1(Location location) { this.spawn1 = location; }
    public void spawn2(Location location) { this.spawn2 = location; }
    public void enabled(boolean enabled) { this.enabled = enabled; }
    public void vip(boolean vip) { this.vip = vip; }
    public void addKit(String kitId) { allowedKits.add(kitId.toLowerCase()); }
    public void removeKit(String kitId) { allowedKits.remove(kitId.toLowerCase()); }
    public void addFfaSpawn(Location location) { ffaSpawns.add(location); }
    public void clearFfaSpawns() { ffaSpawns.clear(); }
    
    /**
     * Cleanup dropped items and entity drops in the arena
     */
    private void cleanup() {
        Set<Location> spawnLocations = new HashSet<>();
        if (spawn1 != null) spawnLocations.add(spawn1);
        if (spawn2 != null) spawnLocations.add(spawn2);
        spawnLocations.addAll(ffaSpawns);
        
        for (Location spawn : spawnLocations) {
            if (spawn == null || spawn.getWorld() == null) continue;
            World world = spawn.getWorld();
            
            // Clear items in radius around each spawn
            for (Entity entity : world.getNearbyEntities(spawn, CLEANUP_RADIUS, CLEANUP_RADIUS, CLEANUP_RADIUS)) {
                if (entity instanceof Item item) {
                    item.remove();
                }
            }
        }
    }
}
