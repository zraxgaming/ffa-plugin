package xyz.zcraft.studios.zffa.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class LocationCodec {
    private LocationCodec() {
    }

    public static String serialize(Location location) {
        if (location == null || location.getWorld() == null) return "";
        return "%s, %.3f, %.3f, %.3f, %.2f, %.2f".formatted(
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    public static Location deserialize(String input) {
        if (input == null || input.isBlank()) return null;
        String[] parts = input.split(",");
        if (parts.length < 4) return null;
        World world = Bukkit.getWorld(parts[0].trim());
        if (world == null) return null;
        double x = Double.parseDouble(parts[1].trim());
        double y = Double.parseDouble(parts[2].trim());
        double z = Double.parseDouble(parts[3].trim());
        float yaw = parts.length > 4 ? Float.parseFloat(parts[4].trim()) : 0F;
        float pitch = parts.length > 5 ? Float.parseFloat(parts[5].trim()) : 0F;
        return new Location(world, x, y, z, yaw, pitch);
    }
}
