package xyz.zcraft.studios.zffa.arena;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.World;

import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Arena {
    private static final int CLEANUP_PADDING = 64;

    private final String name;
    private Location spawn1;
    private Location spawn2;
    private boolean enabled;
    private boolean vip;
    private final Set<String> allowedKits;
    private final List<Location> ffaSpawns;
    private final AtomicBoolean busy = new AtomicBoolean(false);

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
        cleanupDroppedItems();
    }
    
    public void spawn1(Location location) { this.spawn1 = location; }
    public void spawn2(Location location) { this.spawn2 = location; }
    public void enabled(boolean enabled) { this.enabled = enabled; }
    public void vip(boolean vip) { this.vip = vip; }
    public void addKit(String kitId) { allowedKits.add(kitId.toLowerCase()); }
    public void removeKit(String kitId) { allowedKits.remove(kitId.toLowerCase()); }
    public void addFfaSpawn(Location location) { ffaSpawns.add(location); }
    public void clearFfaSpawns() { ffaSpawns.clear(); }
    
    public void cleanupDroppedItems() {
        Map<World, Bounds> boundsByWorld = new HashMap<>();
        for (Location location : allSpawnLocations()) {
            if (location == null || location.getWorld() == null) continue;
            boundsByWorld.computeIfAbsent(location.getWorld(), ignored -> new Bounds(location)).include(location);
        }

        for (Map.Entry<World, Bounds> entry : boundsByWorld.entrySet()) {
            World world = entry.getKey();
            Bounds bounds = entry.getValue().expanded(CLEANUP_PADDING);
            for (Item item : world.getEntitiesByClass(Item.class)) {
                if (bounds.contains(item.getLocation())) {
                    item.remove();
                }
            }
        }
    }

    private List<Location> allSpawnLocations() {
        ArrayList<Location> locations = new ArrayList<>();
        if (spawn1 != null) locations.add(spawn1);
        if (spawn2 != null) locations.add(spawn2);
        locations.addAll(ffaSpawns);
        return locations;
    }

    private static final class Bounds {
        private final World world;
        private double minX;
        private double maxX;
        private double minY;
        private double maxY;
        private double minZ;
        private double maxZ;

        private Bounds(World world) {
            this.world = world;
        }

        private Bounds(Location initial) {
            this(initial.getWorld());
            this.minX = this.maxX = initial.getX();
            this.minY = this.maxY = initial.getY();
            this.minZ = this.maxZ = initial.getZ();
        }

        private void include(Location location) {
            minX = Math.min(minX, location.getX());
            maxX = Math.max(maxX, location.getX());
            minY = Math.min(minY, location.getY());
            maxY = Math.max(maxY, location.getY());
            minZ = Math.min(minZ, location.getZ());
            maxZ = Math.max(maxZ, location.getZ());
        }

        private Bounds expanded(double padding) {
            Bounds copy = new Bounds(world);
            copy.minX = minX - padding;
            copy.maxX = maxX + padding;
            copy.minY = minY - padding;
            copy.maxY = maxY + padding;
            copy.minZ = minZ - padding;
            copy.maxZ = maxZ + padding;
            return copy;
        }

        private boolean contains(Location location) {
            if (world != null && !world.equals(location.getWorld())) return false;
            return location.getX() >= minX
                    && location.getX() <= maxX
                    && location.getY() >= minY
                    && location.getY() <= maxY
                    && location.getZ() >= minZ
                    && location.getZ() <= maxZ;
        }
    }
}
