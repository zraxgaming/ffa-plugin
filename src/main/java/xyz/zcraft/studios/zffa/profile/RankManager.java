package xyz.zcraft.studios.zffa.profile;

import org.bukkit.configuration.file.FileConfiguration;
import xyz.zcraft.studios.zffa.ZFfaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class RankManager {
    private final ZFfaPlugin plugin;
    private final List<RankEntry> ranks = new ArrayList<>();

    public RankManager(ZFfaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        ranks.clear();
        FileConfiguration config = plugin.getConfig();
        List<?> rawEntries = config.getMapList("ranks");
        if (rawEntries.isEmpty()) {
            loadDefaults();
            return;
        }
        for (Object rawEntry : rawEntries) {
            if (!(rawEntry instanceof Map<?, ?> rawMap)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> entry = (Map<String, Object>) rawMap;
            String name = String.valueOf(entry.getOrDefault("name", "Unknown"));
            int minElo = 0;
            try {
                minElo = Integer.parseInt(String.valueOf(entry.getOrDefault("min-elo", "0")));
            } catch (NumberFormatException ignored) {
            }
            String material = String.valueOf(entry.getOrDefault("material", "STONE"));
            ranks.add(new RankEntry(name, minElo, material));
        }
        if (ranks.isEmpty()) {
            loadDefaults();
        } else {
            ranks.sort(Comparator.comparingInt(RankEntry::minElo));
        }
    }

    public String rankName(int elo) {
        RankEntry rank = ranks.stream()
                .filter(entry -> elo >= entry.minElo())
                .reduce((first, second) -> second)
                .orElse(null);
        return rank == null ? "Unranked" : rank.name();
    }

    public List<RankEntry> all() {
        return Collections.unmodifiableList(ranks);
    }

    private void loadDefaults() {
        ranks.add(new RankEntry("Coal I", 0, "COAL"));
        ranks.add(new RankEntry("Coal II", 500, "COAL"));
        ranks.add(new RankEntry("Coal III", 1000, "COAL"));
        ranks.add(new RankEntry("Iron", 1500, "IRON_INGOT"));
        ranks.add(new RankEntry("Gold", 2000, "GOLD_INGOT"));
        ranks.add(new RankEntry("Diamond", 2500, "DIAMOND"));
        ranks.add(new RankEntry("Netherite", 3000, "NETHERITE_INGOT"));
    }

    public record RankEntry(String name, int minElo, String material) {
    }
}
