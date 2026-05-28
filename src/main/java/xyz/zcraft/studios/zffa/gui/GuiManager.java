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
import xyz.zcraft.studios.zffa.party.Party;
import xyz.zcraft.studios.zffa.profile.EloCalculator;
import xyz.zcraft.studios.zffa.profile.PlayerProfile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import xyz.zcraft.studios.zffa.profile.RankManager;

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
            String action = section.getString("action", "").toUpperCase(Locale.ROOT);
            if (action.isBlank()) continue;
            if (action.equals("OPEN_EVENT") && !plugin.getConfig().getBoolean("settings.event.enabled", false)) continue;
            if (action.equals("OPEN_UNRANKED_KITS") && !plugin.getConfig().getBoolean("settings.unranked.enabled", true)) continue;
            ItemStack item = configuredItem(section, playerPlaceholders(player));
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(Keys.MENU_ACTION, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
            player.getInventory().setItem(slot, item);
        }
    }

    public void executeAction(Player player, String action) {
        switch (action.toUpperCase(Locale.ROOT)) {
            case "OPEN_KITS", "OPEN_QUEUE", "QUEUE_SELECTOR", "OPEN_RANKED_KITS" -> openKits(player, true);
            case "OPEN_UNRANKED_KITS" -> openKits(player, false);
            case "OPEN_STATS", "STATS" -> openStats(player);
            case "OPEN_STATS_TARGET" -> plugin.messages().send(player, "<red>Unable to open target stats.");
            case "OPEN_LEADERBOARD", "LEADERBOARD", "TOP" -> openLeaderboard(player);
            case "OPEN_RANKS" -> openRanks(player);
            case "OPEN_PARTY" -> openParty(player);
            case "OPEN_EVENT" -> plugin.ffa().joinEvent(player);
            case "LEAVE_QUEUE" -> {
                plugin.queues().leave(player.getUniqueId());
                plugin.messages().send(player, "<yellow>You left the queue.");
            }
            case "PARTY_LIST" -> plugin.parties().party(player.getUniqueId())
                    .ifPresentOrElse(party -> plugin.messages().send(player, "<gray>Party: <white>" + String.join(", ", party.members().stream().map(uuid -> {
                        String name = Bukkit.getOfflinePlayer(uuid).getName();
                        return name == null ? "Unknown" : name;
                    }).toList()) + "</white>"), () -> plugin.messages().send(player, "<red>You are not in a party."));
            case "PARTY_DETAILS" -> openPartyDetails(player);
            case "PARTY_DUEL" -> {
                Party party = plugin.parties().party(player.getUniqueId()).orElse(null);
                if (party == null || !party.isLeader(player.getUniqueId())) {
                    plugin.messages().send(player, "<red>Only the party leader can queue the party.");
                } else {
                    openKits(player, true);
                }
            }
            case "PARTY_FFA" -> {
                Party party = plugin.parties().party(player.getUniqueId()).orElse(null);
                if (party == null || !party.isLeader(player.getUniqueId())) {
                    plugin.messages().send(player, "<red>Only the party leader can queue the party FFA.");
                } else {
                    openPartyFfaKits(player);
                }
            }
            case "PARTY_LEAVE" -> {
                plugin.parties().leave(player, true);
                plugin.messages().send(player, "<yellow>You left the party.");
            }
            default -> plugin.messages().send(player, "<red>Unknown menu action: <white>" + action + "</white>");
        }
    }

    public void openKits(Player player) {
        openKits(player, true);
    }

    public void openKits(Player player, boolean ranked) {
        openKits(player, ranked, null);
    }

    public void openKits(Player player, boolean ranked, String duelTarget) {
        String titleKey = duelTarget == null ? (ranked ? "kit-selector" : "kit-selector-unranked") : "kit-selector";
        String fallbackTitle = duelTarget == null
                ? (ranked ? "<gradient:#21d4fd:#b721ff>Ranked Queue Selector</gradient>" : "<gradient:#a8ff78:#78ffd6>Unranked Queue Selector</gradient>")
                : "<gradient:#21d4fd:#b721ff>Duel Kit Selector</gradient>";
        Inventory inventory = Bukkit.createInventory(new ZFfaGuiHolder(GuiType.KIT_SELECTOR), kitTemplate.getSize(), title(titleKey, fallbackTitle));
        for (int i = 0; i < kitTemplate.getSize(); i++) {
            ItemStack item = kitTemplate.getItem(i);
            if (item != null) inventory.setItem(i, item.clone());
        }
        int slot = 0;
        for (Kit kit : plugin.kits().all()) {
            if (slot >= inventory.getSize()) break;
            inventory.setItem(slot++, kitItem(kit, ranked, duelTarget));
        }
        player.openInventory(inventory);
    }

    public void openDuelKits(Player player, Player target, boolean ranked) {
        if (target == null) {
            openKits(player, ranked);
            return;
        }
        openKits(player, ranked, target.getName());
    }

    public void openPartyFfaKits(Player player) {
        Inventory inventory = Bukkit.createInventory(new ZFfaGuiHolder(GuiType.KIT_SELECTOR), kitTemplate.getSize(), title("kit-selector", "<gradient:#f7b733:#fc4a1a>Party FFA Kit Selector</gradient>"));
        for (int i = 0; i < kitTemplate.getSize(); i++) {
            ItemStack item = kitTemplate.getItem(i);
            if (item != null) inventory.setItem(i, item.clone());
        }
        int slot = 0;
        for (Kit kit : plugin.kits().all()) {
            if (slot >= inventory.getSize()) break;
            inventory.setItem(slot++, kitItem(kit, true, null, "QUEUE_PARTY_FFA"));
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
                    "%rank%", plugin.ranks().rankName(profile.elo()),
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

    public void openRanks(Player player) {
        Inventory inventory = Bukkit.createInventory(new ZFfaGuiHolder(GuiType.RANKS), menuSize("ranks", 27), title("ranks", "<gold>Rank Progression</gold>"));
        ConfigurationSection entry = menus.getConfigurationSection("menus.ranks.entry-item");
        int slot = 0;
        for (RankManager.RankEntry rank : plugin.ranks().all()) {
            Map<String, String> placeholders = Map.of(
                    "%rank_name%", rank.name(),
                    "%min_elo%", String.valueOf(rank.minElo())
            );
            ItemStack item = entry == null
                    ? configuredItem(material(rank.material()), "<gold>%rank_name%</gold>", List.of("<gray>Minimum Elo: <white>%min_elo%</white>"), placeholders)
                    : configuredItem(entry, placeholders);
            if (slot < inventory.getSize()) {
                inventory.setItem(slot++, item);
            }
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

    private ItemStack kitItem(Kit kit, boolean ranked, String duelTarget) {
        return kitItem(kit, ranked, duelTarget, null);
    }

    private ItemStack kitItem(Kit kit, boolean ranked, String duelTarget, String customAction) {
        String sectionName = ranked ? "menus.kit-selector.kit-item" : "menus.kit-selector-unranked.kit-item";
        ConfigurationSection section = menus.getConfigurationSection(sectionName);
        List<String> lore = section == null ? List.of("<gray>Queued: <white>%queue_size%</white>") : section.getStringList("lore");
        String name = section == null ? "%kit_display%" : section.getString("name", "%kit_display%");
        Map<String, String> placeholders = Map.of(
                "%kit%", kit.id(),
                "%kit_display%", plainDisplayFallback(kit.id()),
                "%queue_size%", String.valueOf(plugin.queues().size(queueKey(kit.id(), ranked)))
        );
        ItemStack item = configuredItem(kit.icon(), name, lore, placeholders);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(kit.display());
        meta.getPersistentDataContainer().set(Keys.KIT_ID, PersistentDataType.STRING, kit.id());
        String action = customAction != null ? customAction : (duelTarget == null
                ? (ranked ? "QUEUE_RANKED" : "QUEUE_UNRANKED")
                : (ranked ? "DUEL_REQUEST_RANKED" : "DUEL_REQUEST_UNRANKED"));
        meta.getPersistentDataContainer().set(Keys.MENU_ACTION, PersistentDataType.STRING, action);
        if (duelTarget != null) {
            meta.getPersistentDataContainer().set(Keys.TARGET_PLAYER, PersistentDataType.STRING, duelTarget);
        }
        if (section != null && section.getBoolean("glow", false)) addGlow(meta);
        item.setItemMeta(meta);
        return item;
    }

    private String queueKey(String kitId, boolean ranked) {
        return kitId.toLowerCase(Locale.ROOT) + ":" + (ranked ? "ranked" : "unranked");
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

    public void openParty(Player player) {
        Inventory inventory = Bukkit.createInventory(new ZFfaGuiHolder(GuiType.PARTY), menuSize("party", 27), title("party", "<gold>Party Hub</gold>"));
        ConfigurationSection items = menus.getConfigurationSection("menus.party.items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection section = items.getConfigurationSection(key);
                if (section == null) continue;
                int slot = section.getInt("slot", 13);
                inventory.setItem(slot, configuredItem(section, playerPlaceholders(player)));
            }
        } else {
            Party party = plugin.parties().party(player.getUniqueId()).orElse(null);
            ItemStack info = configuredItem(Material.PLAYER_HEAD, "<green>Party Info</green>", List.of(
                    party == null ? "<gray>You are not in a party." : "<gray>Members: <white>" + party.members().stream().map(uuid -> {
                        String name = Bukkit.getOfflinePlayer(uuid).getName();
                        return name == null ? "Unknown" : name;
                    }).toList() + "</white>",
                    "<gray>Click to show party details."), playerPlaceholders(player));
            ItemMeta infoMeta = info.getItemMeta();
            infoMeta.getPersistentDataContainer().set(Keys.MENU_ACTION, PersistentDataType.STRING, "PARTY_DETAILS");
            info.setItemMeta(infoMeta);
            inventory.setItem(11, info);
            ItemStack leave = configuredItem(Material.BARRIER, "<red>Leave Party</red>", List.of("<gray>Leave your active party."), playerPlaceholders(player));
            ItemMeta leaveMeta = leave.getItemMeta();
            leaveMeta.getPersistentDataContainer().set(Keys.MENU_ACTION, PersistentDataType.STRING, "PARTY_LEAVE");
            leave.setItemMeta(leaveMeta);
            inventory.setItem(15, leave);
        }
        player.openInventory(inventory);
    }

    public void openPartyDetails(Player player) {
        Inventory inventory = Bukkit.createInventory(new ZFfaGuiHolder(GuiType.PARTY), menuSize("party", 27), title("party", "<gold>Party Details</gold>"));
        Party party = plugin.parties().party(player.getUniqueId()).orElse(null);
        if (party == null) {
            inventory.setItem(13, configuredItem(Material.BARRIER, "<red>No Active Party</red>", List.of("<gray>You are not currently in a party."), playerPlaceholders(player)));
            player.openInventory(inventory);
            return;
        }

        String members = String.join(", ", party.members().stream().map(uuid -> {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            return name == null ? "Unknown" : name;
        }).toList());

        inventory.setItem(11, configuredItem(Material.PLAYER_HEAD, "<gold>Party Members</gold>", List.of(
                "<gray>Leader: <white>" + Bukkit.getOfflinePlayer(party.leader()).getName() + "</white>",
                "<gray>Size: <white>" + party.size() + "</white>",
                "<gray>Members: <white>" + members + "</white>"
        ), playerPlaceholders(player)));

        ItemStack duel = configuredItem(Material.CROSSBOW, "<green>Queue Party Duel</green>", List.of("<gray>Leader only: choose a kit and queue party.", "<gray>Split into teams and fight."), playerPlaceholders(player));
        ItemMeta duelMeta = duel.getItemMeta();
        duelMeta.getPersistentDataContainer().set(Keys.MENU_ACTION, PersistentDataType.STRING, "PARTY_DUEL");
        duel.setItemMeta(duelMeta);
        inventory.setItem(13, duel);

        ItemStack ffa = configuredItem(Material.FIREWORK_ROCKET, "<aqua>Party FFA</aqua>", List.of("<gray>Leader only: choose a kit for party FFA.", "<gray>Everyone joins the FFA arena."), playerPlaceholders(player));
        ItemMeta ffaMeta = ffa.getItemMeta();
        ffaMeta.getPersistentDataContainer().set(Keys.MENU_ACTION, PersistentDataType.STRING, "PARTY_FFA");
        ffa.setItemMeta(ffaMeta);
        inventory.setItem(15, ffa);

        ItemStack leaveParty = configuredItem(Material.BARRIER, "<red>Leave Party</red>", List.of("<gray>Leave the party immediately."), playerPlaceholders(player));
        ItemMeta leavePartyMeta = leaveParty.getItemMeta();
        leavePartyMeta.getPersistentDataContainer().set(Keys.MENU_ACTION, PersistentDataType.STRING, "PARTY_LEAVE");
        leaveParty.setItemMeta(leavePartyMeta);
        inventory.setItem(17, leaveParty);

        player.openInventory(inventory);
    }

    private Map<String, String> playerPlaceholders(Player player) {
        return playerPlaceholders(player, plugin.profiles().getOrCreate(player));
    }

    private Map<String, String> playerPlaceholders(Player player, PlayerProfile profile) {
        return Map.of(
                "%player%", player.getName(),
                "%elo%", String.valueOf(profile.elo()),
                "%rank%", plugin.ranks().rankName(profile.elo()),
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



public void openPlayerMenu(Player viewer, Player target) {
    Inventory inv = Bukkit.createInventory(
            new ZFfaGuiHolder(GuiType.PLAYER_MENU),
            27,
            Component.text("Player Menu")
    );

    PlayerProfile profile = plugin.profiles().getOrCreate(target);

    Map<String, String> placeholders = Map.of(
            "%player%", target.getName(),
            "%elo%", String.valueOf(profile.elo()),
            "%rank%", plugin.ranks().rankName(profile.elo()),
            "%wins%", String.valueOf(profile.wins()),
            "%losses%", String.valueOf(profile.losses()),
            "%kills%", String.valueOf(profile.kills()),
            "%deaths%", String.valueOf(profile.deaths())
    );

    ItemStack duel = configuredItem(Material.CROSSBOW, "<green>Duel %player%</green>", List.of(
            "<gray>Challenge this player to a duel.",
            "<gray>Open the duel kit selector."
    ), placeholders);
    ItemMeta duelMeta = duel.getItemMeta();
    duelMeta.getPersistentDataContainer().set(Keys.MENU_ACTION, PersistentDataType.STRING, "DUEL_PLAYER");
    duelMeta.getPersistentDataContainer().set(Keys.TARGET_PLAYER, PersistentDataType.STRING, target.getName());
    duel.setItemMeta(duelMeta);
    inv.setItem(11, duel);

    ItemStack info = configuredItem(
            Material.PLAYER_HEAD,
            "<gold>%player%</gold>",
            List.of(
                    "<gray>Elo: <white>%elo%</white>",
                    "<gray>Rank: <white>%rank%</white>",
                    "<gray>Wins: <white>%wins%</white>",
                    "<gray>Losses: <white>%losses%</white>",
                    "<gray>Kills: <white>%kills%</white>",
                    "<gray>Deaths: <white>%deaths%</white>"
            ),
            placeholders
    );
    inv.setItem(13, info);

    ItemStack stats = configuredItem(Material.BOOK, "<aqua>View Stats</aqua>", List.of(
            "<gray>See detailed stats for %player%."
    ), placeholders);
    ItemMeta statsMeta = stats.getItemMeta();
    statsMeta.getPersistentDataContainer().set(Keys.MENU_ACTION, PersistentDataType.STRING, "OPEN_STATS_TARGET");
    statsMeta.getPersistentDataContainer().set(Keys.TARGET_PLAYER, PersistentDataType.STRING, target.getName());
    stats.setItemMeta(statsMeta);
    inv.setItem(15, stats);

    viewer.openInventory(inv);
}
}


