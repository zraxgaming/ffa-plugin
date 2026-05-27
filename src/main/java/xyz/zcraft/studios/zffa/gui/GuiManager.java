package xyz.zcraft.studios.zffa.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import xyz.zcraft.studios.zffa.ZFfaPlugin;
import xyz.zcraft.studios.zffa.kit.Kit;
import xyz.zcraft.studios.zffa.profile.EloCalculator;
import xyz.zcraft.studios.zffa.profile.PlayerProfile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GuiManager {
    private final ZFfaPlugin plugin;
    private YamlConfiguration menus;
    private Inventory kitTemplate;

    public GuiManager(ZFfaPlugin plugin) {
        this.plugin = plugin;
    }

    public void rebuild() {
        this.menus = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "menus.yml"));
        buildKitTemplate();
    }

    public void giveLobbyItems(Player player) {
        if (!plugin.getConfig().getBoolean("settings.lobby-items-enabled", true)) return;
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("lobby-items");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) continue;
            int slot = section.getInt("slot", -1);
            if (slot < 0 || slot > 35) continue;
            ItemStack item = configuredItem(section, playerPlaceholders(player));
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(Keys.MENU_ACTION, PersistentDataType.STRING, section.getString("action", "").toUpperCase(Locale.ROOT));
            item.setItemMeta(meta);
            player.getInventory().setItem(slot, item);
        }
    }

    public void executeAction(Player player, String action) {
        switch (action.toUpperCase(Locale.ROOT)) {
            case "OPEN_KITS", "OPEN_QUEUE", "QUEUE_SELECTOR" -> openKits(player);
            case "OPEN_STATS", "STATS" -> openStats(player);
            case "OPEN_LEADERBOARD", "LEADERBOARD", "TOP" -> openLeaderboard(player);
            case "LEAVE_QUEUE" -> {
                plugin.queues().leave(player.getUniqueId());
                plugin.messages().send(player, "<yellow>You left the queue.");
            }
            default -> plugin.messages().send(player, "<red>Unknown menu action: <white>" + action + "</white>");
        }
    }

    public void openKits(Player player) {
        Inventory inventory = Bukkit.createInventory(new ZFfaGuiHolder(GuiType.KIT_SELECTOR), kitTemplate.getSize(), title("kit-selector", "<gradient:#21d4fd:#b721ff>Ranked Queue Selector</gradient>"));
        for (int i = 0; i < kitTemplate.getSize(); i++) {
            ItemStack item = kitTemplate.getItem(i);
            if (item != null) inventory.setItem(i, item.clone());
        }
        int slot = 0;
        for (Kit kit : plugin.kits().all()) {
            if (slot >= inventory.getSize()) break;
            inventory.setItem(slot++, kitItem(kit));
        }
        player.openInventory(inventory);
    }

    public void openStats(Player player) {
        PlayerProfile profile = plugin.profiles().getOrCreate(player);
        Inventory inventory = Bukkit.createInventory(new ZFfaGuiHolder(GuiType.STATS), menuSize("stats", 27), title("stats", "<aqua>Your Stats</aqua>"));
        ConfigurationSection items = menus.getConfigurationSection("menus.stats.items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection section = items.getConfigurationSection(key);
                if (section == null) continue;
                int slot = section.getInt("slot", 13);
                inventory.setItem(slot, configuredItem(section, playerPlaceholders(player, profile)));
            }
        } else {
            inventory.setItem(13, configuredItem(Material.PLAYER_HEAD, "<gold>%player%</gold>", List.of(
                    "<gray>Elo: <white>%elo%</white>",
                    "<gray>Rank: <white>%rank%</white>",
                    "<gray>Wins: <white>%wins%</white>",
                    "<gray>Losses: <white>%losses%</white>"
            ), playerPlaceholders(player, profile)));
        }
        player.openInventory(inventory);
    }

    public void openLeaderboard(Player player) {
        Inventory inventory = Bukkit.createInventory(new ZFfaGuiHolder(GuiType.LEADERBOARD), menuSize("leaderboard", 27), title("leaderboard", "<gold>Top Fighters</gold>"));
        ConfigurationSection entry = menus.getConfigurationSection("menus.leaderboard.entry-item");
        int slot = 0;
        int position = 1;
        for (PlayerProfile profile : plugin.profiles().topCached(10)) {
            Map<String, String> placeholders = Map.of(
                    "%position%", String.valueOf(position),
                    "%player%", profile.name(),
                    "%elo%", String.valueOf(profile.elo()),
                    "%rank%", EloCalculator.rank(profile.elo()),
                    "%wins%", String.valueOf(profile.wins()),
                    "%losses%", String.valueOf(profile.losses())
            );
            inventory.setItem(slot++, entry == null
                    ? configuredItem(Material.EMERALD, "<green>#%position% %player%</green>", List.of("<gray>Elo: <white>%elo%</white>", "<gray>Rank: <white>%rank%</white>"), placeholders)
                    : configuredItem(entry, placeholders));
            position++;
        }
        player.openInventory(inventory);
    }

    private void buildKitTemplate() {
        int size = menuSize("kit-selector", Math.max(9, ((plugin.kits().all().size() + 8) / 9) * 9));
        kitTemplate = Bukkit.createInventory(new ZFfaGuiHolder(GuiType.KIT_SELECTOR), size, title("kit-selector", "<gradient:#21d4fd:#b721ff>Ranked Queue Selector</gradient>"));
        ConfigurationSection filler = menus.getConfigurationSection("menus.kit-selector.filler");
        if (filler != null && filler.getBoolean("enabled", false)) {
            ItemStack fillerItem = configuredItem(filler, Map.of());
            ItemMeta meta = fillerItem.getItemMeta();
            meta.getPersistentDataContainer().set(Keys.MENU_ACTION, PersistentDataType.STRING, "FILLER");
            fillerItem.setItemMeta(meta);
            for (int i = 0; i < size; i++) kitTemplate.setItem(i, fillerItem);
        }
    }

    private ItemStack kitItem(Kit kit) {
        ConfigurationSection section = menus.getConfigurationSection("menus.kit-selector.kit-item");
        List<String> lore = section == null ? List.of("<gray>Queued: <white>%queue_size%</white>") : section.getStringList("lore");
        String name = section == null ? "%kit_display%" : section.getString("name", "%kit_display%");
        Map<String, String> placeholders = Map.of(
                "%kit%", kit.id(),
                "%kit_display%", plainDisplayFallback(kit.id()),
                "%queue_size%", String.valueOf(plugin.queues().size(kit.id()))
        );
        ItemStack item = configuredItem(kit.icon(), name, lore, placeholders);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(kit.display());
        meta.getPersistentDataContainer().set(Keys.KIT_ID, PersistentDataType.STRING, kit.id());
        if (section != null && section.getBoolean("glow", false)) addGlow(meta);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack configuredItem(ConfigurationSection section, Map<String, String> placeholders) {
        Material material = material(section.getString("material", "STONE"));
        return configuredItem(material, section.getString("name", "<white>Item</white>"), section.getStringList("lore"), placeholders);
    }

    private ItemStack configuredItem(Material material, String name, List<String> lore, Map<String, String> placeholders) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plugin.messages().parse(replace(name, placeholders)));
        ArrayList<Component> lines = new ArrayList<>();
        for (String line : lore) lines.add(plugin.messages().parse(replace(line, placeholders)));
        meta.lore(lines);
        if (meta instanceof SkullMeta skullMeta && placeholders.containsKey("%player%")) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(placeholders.get("%player%")));
        }
        item.setItemMeta(meta);
        return item;
    }

    private Map<String, String> playerPlaceholders(Player player) {
        return playerPlaceholders(player, plugin.profiles().getOrCreate(player));
    }

    private Map<String, String> playerPlaceholders(Player player, PlayerProfile profile) {
        return Map.of(
                "%player%", player.getName(),
                "%elo%", String.valueOf(profile.elo()),
                "%rank%", EloCalculator.rank(profile.elo()),
                    "%wins%", String.valueOf(profile.wins()),
                    "%losses%", String.valueOf(profile.losses()),
                    "%kills%", String.valueOf(profile.kills()),
                    "%deaths%", String.valueOf(profile.deaths()),
                    "%status%", plugin.queues().status(player.getUniqueId())
        );
    }

    private Component title(String menu, String fallback) {
        return plugin.messages().parse(menus.getString("menus." + menu + ".title", fallback));
    }

    private int menuSize(String menu, int fallback) {
        int size = menus.getInt("menus." + menu + ".size", fallback);
        return Math.max(9, Math.min(54, ((size + 8) / 9) * 9));
    }

    private boolean isFiller(ItemStack item) {
        return item.hasItemMeta() && "FILLER".equals(item.getItemMeta().getPersistentDataContainer().get(Keys.MENU_ACTION, PersistentDataType.STRING));
    }

    private String replace(String input, Map<String, String> placeholders) {
        String output = input == null ? "" : input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) output = output.replace(entry.getKey(), entry.getValue());
        return output;
    }

    private Material material(String raw) {
        try {
            return Material.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Material.STONE;
        }
    }

    private String plainDisplayFallback(String kitId) {
        return "<white>" + kitId + "</white>";
    }

    private void addGlow(ItemMeta meta) {
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }
}
