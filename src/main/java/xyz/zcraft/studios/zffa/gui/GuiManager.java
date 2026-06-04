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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import xyz.zcraft.studios.zffa.ZFfaPlugin;
import xyz.zcraft.studios.zffa.arena.Arena;
import xyz.zcraft.studios.zffa.kit.Kit;
import xyz.zcraft.studios.zffa.party.Party;
import xyz.zcraft.studios.zffa.profile.EloCalculator;
import xyz.zcraft.studios.zffa.profile.PlayerProfile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
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
        if (root == null) {
            plugin.debug("No lobby-items section found in config");
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) continue;
            int slot = section.getInt("slot", -1);
            if (slot < 0 || slot > 35) continue;
            String action = section.getString("action", "").toUpperCase(Locale.ROOT);
            if (action.isBlank()) continue;
            if (action.equals("OPEN_EVENT") && !plugin.getConfig().getBoolean("settings.event.enabled", false)) continue;
            if (action.equals("OPEN_UNRANKED_KITS") && !plugin.getConfig().getBoolean("settings.unranked.enabled", true)) continue;
            try {
                ItemStack item = configuredItem(section, playerPlaceholders(player));
                if (item == null || item.getType().isAir()) {
                    plugin.debug("Skipping null or air lobby item: " + key);
                    continue;
                }
                ItemMeta meta = item.getItemMeta();
                if (meta == null) {
                    plugin.debug("Warning: ItemMeta is null for lobby item: " + key + ". Creating new meta...");
                    meta = Bukkit.getItemFactory().getItemMeta(item.getType());
                    if (meta == null) continue;
                    item.setItemMeta(meta);
                }
                meta.getPersistentDataContainer().set(Keys.MENU_ACTION, PersistentDataType.STRING, action);
                meta.getPersistentDataContainer().set(Keys.ITEM_SOURCE, PersistentDataType.STRING, "LOBBY");
                item.setItemMeta(meta);
                player.getInventory().setItem(slot, item);
            } catch (Exception e) {
                plugin.getLogger().warning("Error setting lobby item '" + key + "': " + e.getMessage());
                plugin.debug("Lobby item error type: " + e.getClass().getName());
            }
        }
    }

    public void executeAction(Player player, String action) {
        switch (action.toUpperCase(Locale.ROOT)) {
            case "OPEN_MAIN", "MAIN", "MENU" -> openMainMenu(player);
            case "OPEN_MANAGEMENT", "MANAGEMENT" -> openManagement(player);
            case "OPEN_KIT_EDITOR", "KIT_EDITOR" -> openKitEditor(player);
            case "OPEN_KITS", "OPEN_QUEUE", "QUEUE_SELECTOR", "OPEN_RANKED_KITS", "OPEN_RANKED", "RANKED" -> openKits(player, true);
            case "OPEN_UNRANKED_KITS", "OPEN_UNRANKED", "UNRANKED" -> openKits(player, false);
            case "OPEN_FFA", "OPEN_FFA_ARENAS", "FFA_ARENAS" -> openFfaArenas(player);
            case "OPEN_STATS", "STATS" -> openStats(player);
            case "OPEN_STATS_TARGET" -> plugin.messages().send(player, "gui.stats-target-unavailable", "<red>Unable to open target stats.");
            case "OPEN_LEADERBOARD", "LEADERBOARD", "TOP" -> openLeaderboard(player);
            case "OPEN_RANKS" -> openRanks(player);
            case "OPEN_PARTY" -> openParty(player);
            case "OPEN_EVENT" -> plugin.ffa().joinEvent(player);
            case "LEAVE_QUEUE" -> {
                plugin.queues().leave(player.getUniqueId());
                plugin.messages().send(player, "gui.queue-left", "<yellow>You left the queue.");
            }
            case "PARTY_LIST" -> plugin.parties().party(player.getUniqueId())
                    .ifPresentOrElse(party -> {
                        String members = String.join(", ", party.members().stream().map(uuid -> {
                            return cachedPlayerName(uuid);
                        }).toList());
                        plugin.messages().send(player, "gui.party-members", "<gray>Party: <white>{members}</white>", Map.of("members", members));
                    }, () -> plugin.messages().send(player, "gui.party-not-in-party", "<red>You are not in a party."));
            case "PARTY_DETAILS" -> openPartyDetails(player);
            case "PARTY_DUEL" -> {
                Party party = plugin.parties().party(player.getUniqueId()).orElse(null);
                if (party == null || !party.isLeader(player.getUniqueId())) {
                    plugin.messages().send(player, "gui.party-leader-only-duel", "<red>Only the party leader can queue the party.");
                } else {
                    openKits(player, true);
                }
            }
            case "PARTY_FFA" -> {
                Party party = plugin.parties().party(player.getUniqueId()).orElse(null);
                if (party == null || !party.isLeader(player.getUniqueId())) {
                    plugin.messages().send(player, "gui.party-leader-only-ffa", "<red>Only the party leader can queue the party FFA.");
                } else {
                    openPartyFfaKits(player);
                }
            }
            case "PARTY_LEAVE" -> {
                plugin.parties().leave(player, true);
                plugin.messages().send(player, "gui.party-left", "<yellow>You left the party.");
            }
            default -> plugin.messages().send(player, "gui.unknown-action", "<red>Unknown menu action: <white>{action}</white>", Map.of("action", action));
        }
    }

    public void openKits(Player player) {
        openKits(player, true);
    }

    public void openMainMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new ZFfaGuiHolder(GuiType.MAIN), menuSize("main", 27), title("main", "<gold>Z-FFA</gold>"));
        applyFiller(inventory, "main");
        ConfigurationSection items = menus.getConfigurationSection("menus.main.items");
        if (items != null) {
            Map<String, String> placeholders = globalPlaceholders(player);
            for (String key : items.getKeys(false)) {
                ConfigurationSection section = items.getConfigurationSection(key);
                if (section == null) continue;
                ItemStack item = configuredItem(section, placeholders);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    String action = section.getString("action", "");
                    if (!action.isBlank()) {
                        meta.getPersistentDataContainer().set(Keys.MENU_ACTION, PersistentDataType.STRING, action.toUpperCase(Locale.ROOT));
                    }
                    item.setItemMeta(meta);
                }
                inventory.setItem(boundedSlot(section.getInt("slot", 13), inventory.getSize()), item);
            }
        } else {
            addActionItem(inventory, 10, Material.DIAMOND_SWORD, "<aqua>Ranked Queue</aqua>", List.of("<gray>Choose a kit and queue ranked."), "OPEN_RANKED", player);
            addActionItem(inventory, 12, Material.IRON_SWORD, "<green>Unranked Queue</green>", List.of("<gray>Choose a kit and queue casual."), "OPEN_UNRANKED", player);
            addActionItem(inventory, 14, Material.GRASS_BLOCK, "<gold>FFA Arenas</gold>", List.of("<gray>Join open FFA arenas."), "OPEN_FFA_ARENAS", player);
        }
        player.openInventory(inventory);
    }

    public void openManagement(Player player) {
        if (!player.hasPermission("zf.admin")) {
            plugin.messages().send(player, "permissions.no", "<red>No permission.");
            return;
        }
        Inventory inventory = Bukkit.createInventory(new ZFfaGuiHolder(GuiType.MANAGEMENT), menuSize("management", 27), title("management", "<gold>Z-FFA Management</gold>"));
        applyFiller(inventory, "management");
        addActionItem(inventory, 10, Material.CHEST, "<aqua>Kit Editor</aqua>", List.of("<gray>Left-click kits to preview.", "<gray>Right-click kits to overwrite.", "<gray>Shift-right-click kits to delete."), "OPEN_KIT_EDITOR", player);
        addActionItem(inventory, 12, Material.GRASS_BLOCK, "<green>FFA Arenas</green>", List.of("<gray>View configured FFA arenas."), "OPEN_FFA_ARENAS", player);
        addActionItem(inventory, 14, Material.EMERALD, "<gold>Leaderboard</gold>", List.of("<gray>View live cached leaderboard."), "OPEN_LEADERBOARD", player);
        addActionItem(inventory, 16, Material.NETHER_STAR, "<yellow>Main Menu</yellow>", List.of("<gray>Open the player hub."), "OPEN_MAIN", player);
        player.openInventory(inventory);
    }

    public void openKitEditor(Player player) {
        if (!player.hasPermission("zf.admin")) {
            plugin.messages().send(player, "permissions.no", "<red>No permission.");
            return;
        }
        List<Kit> kits = List.copyOf(plugin.kits().all());
        Inventory inventory = Bukkit.createInventory(new ZFfaGuiHolder(GuiType.KIT_EDITOR), menuSize("kit-editor", Math.max(27, ((kits.size() + 8) / 9) * 9)), title("kit-editor", "<aqua>Kit Editor</aqua>"));
        applyFiller(inventory, "kit-editor");
        List<Integer> slots = itemSlots("kit-editor", inventory.getSize(), kits.size());
        for (int i = 0; i < kits.size() && i < slots.size(); i++) {
            inventory.setItem(slots.get(i), kitEditorItem(kits.get(i)));
        }
        player.openInventory(inventory);
    }

    public void openFfaArenas(Player player) {
        List<Arena> arenas = plugin.arenas().all().stream()
                .filter(Arena::isFfaReady)
                .filter(arena -> !arena.vip() || player.hasPermission("zf.viparena"))
                .toList();
        if (arenas.isEmpty()) {
            player.closeInventory();
            plugin.messages().send(player, "ffa.no-ready-arenas", "<red>No FFA arenas are ready. Ask an admin to add FFA spawns first.");
            return;
        }
        Inventory inventory = Bukkit.createInventory(new ZFfaGuiHolder(GuiType.FFA_ARENAS), menuSize("ffa-arenas", Math.max(27, ((arenas.size() + 8) / 9) * 9)), title("ffa-arenas", "<green>FFA Arenas</green>"));
        applyFiller(inventory, "ffa-arenas");
        List<Integer> slots = itemSlots("ffa-arenas", inventory.getSize(), arenas.size());
        for (int i = 0; i < arenas.size() && i < slots.size(); i++) {
            inventory.setItem(slots.get(i), arenaItem(arenas.get(i), player));
        }
        player.openInventory(inventory);
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
        List<Kit> kits = List.copyOf(plugin.kits().all());
        List<Integer> slots = itemSlots(titleKey, inventory.getSize(), kits.size());
        int index = 0;
        try {
            for (Kit kit : kits) {
                if (index >= slots.size()) break;
                inventory.setItem(slots.get(index++), kitItem(kit, ranked, duelTarget));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error opening kit selector: " + e.getMessage());
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
        List<Kit> kits = List.copyOf(plugin.kits().all());
        List<Integer> slots = itemSlots("kit-selector", inventory.getSize(), kits.size());
        int index = 0;
        for (Kit kit : kits) {
            if (index >= slots.size()) break;
            inventory.setItem(slots.get(index++), kitItem(kit, true, null, "QUEUE_PARTY_FFA"));
        }
        player.openInventory(inventory);
    }

    public void openStats(Player player) {
        openStats(player, player);
    }

    public void openStats(Player viewer, Player target) {
        PlayerProfile profile = plugin.profiles().getOrCreate(target);
        Inventory inventory = Bukkit.createInventory(new ZFfaGuiHolder(GuiType.STATS), menuSize("stats", 27), title("stats", viewer.equals(target) ? "<aqua>Your Stats</aqua>" : "<aqua>Player Stats</aqua>"));
        ConfigurationSection items = menus.getConfigurationSection("menus.stats.items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection section = items.getConfigurationSection(key);
                if (section == null) continue;
                int slot = boundedSlot(section.getInt("slot", 13), inventory.getSize());
                inventory.setItem(slot, configuredItem(section, playerPlaceholders(target, profile)));
            }
        } else {
            inventory.setItem(13, configuredItem(Material.PLAYER_HEAD, "<gold>%player%</gold>", List.of(
                    "<gray>Elo: <white>%elo%</white>",
                    "<gray>Rank: <white>%rank%</white>",
                    "<gray>Wins: <white>%wins%</white>",
                    "<gray>Losses: <white>%losses%</white>"
            ), playerPlaceholders(target, profile)));
        }
        viewer.openInventory(inventory);
    }

    public void openLeaderboard(Player player) {
        Inventory inventory = Bukkit.createInventory(new ZFfaGuiHolder(GuiType.LEADERBOARD), menuSize("leaderboard", 27), title("leaderboard", "<gold>Top Fighters</gold>"));
        populateLeaderboard(inventory);
        player.openInventory(inventory);
    }

    private void populateLeaderboard(Inventory inventory) {
        inventory.clear();
        ConfigurationSection entry = menus.getConfigurationSection("menus.leaderboard.entry-item");
        int slot = 0;
        int position = 1;
        for (PlayerProfile profile : plugin.profiles().topCached(10)) {
            if (slot >= inventory.getSize()) break;
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
        int neededSize = Math.max(27, ((plugin.kits().all().size() + 17) / 9) * 9);
        int size = menuSize("kit-selector", neededSize);
        kitTemplate = Bukkit.createInventory(new ZFfaGuiHolder(GuiType.KIT_SELECTOR), size, title("kit-selector", "<gradient:#21d4fd:#b721ff>Ranked Queue Selector</gradient>"));
        ConfigurationSection filler = menus.getConfigurationSection("menus.kit-selector.filler");
        if (filler != null && filler.getBoolean("enabled", false)) {
            ItemStack fillerItem = configuredItem(filler, Map.of());
            ItemMeta meta = fillerItem.getItemMeta();
            if (meta == null) return;
            meta.getPersistentDataContainer().set(Keys.MENU_ACTION, PersistentDataType.STRING, "FILLER");
            fillerItem.setItemMeta(meta);
            for (int i = 0; i < size; i++) kitTemplate.setItem(i, fillerItem);
        }
    }

    private void applyFiller(Inventory inventory, String menu) {
        ConfigurationSection filler = menus.getConfigurationSection("menus." + menu + ".filler");
        if (filler == null || !filler.getBoolean("enabled", false)) return;
        ItemStack fillerItem = configuredItem(filler, Map.of());
        ItemMeta meta = fillerItem.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(Keys.MENU_ACTION, PersistentDataType.STRING, "FILLER");
            fillerItem.setItemMeta(meta);
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, fillerItem);
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
        if (meta == null) return item;
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

    private ItemStack kitEditorItem(Kit kit) {
        ItemStack item = configuredItem(kit.icon(), "<aqua>%kit%</aqua>", List.of(
                "<gray>Left-click: preview/apply kit.",
                "<gray>Right-click: overwrite from inventory.",
                "<gray>Shift-right-click: delete kit.",
                "<dark_gray>ID: %kit%"
        ), Map.of("%kit%", kit.id()));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(kit.display());
        meta.getPersistentDataContainer().set(Keys.MENU_ACTION, PersistentDataType.STRING, "EDIT_KIT");
        meta.getPersistentDataContainer().set(Keys.KIT_ID, PersistentDataType.STRING, kit.id());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack arenaItem(Arena arena, Player player) {
        ConfigurationSection section = menus.getConfigurationSection("menus.ffa-arenas.arena-item");
        Map<String, String> placeholders = arenaPlaceholders(player, arena);
        Material icon = section == null ? (arena.vip() ? Material.EMERALD_BLOCK : Material.GRASS_BLOCK) : material(section.getString("material", arena.vip() ? "EMERALD_BLOCK" : "GRASS_BLOCK"));
        String name = section == null ? "<green>%arena%</green>" : section.getString("name", "<green>%arena%</green>");
        List<String> lore = section == null ? List.of(
                "<gray>Players: <white>%arena_players%</white>",
                "<gray>Kit: <white>%default_kit%</white>",
                "<gray>Click to join."
        ) : section.getStringList("lore");
        ItemStack item = configuredItem(icon, name, lore, placeholders);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.getPersistentDataContainer().set(Keys.MENU_ACTION, PersistentDataType.STRING, "JOIN_FFA_ARENA");
        meta.getPersistentDataContainer().set(Keys.ARENA_ID, PersistentDataType.STRING, arena.name());
        if (arena.vip() || (section != null && section.getBoolean("glow", false))) addGlow(meta);
        item.setItemMeta(meta);
        return item;
    }

    private void addActionItem(Inventory inventory, int slot, Material material, String name, List<String> lore, String action, Player player) {
        ItemStack item = configuredItem(material, name, lore, globalPlaceholders(player));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(Keys.MENU_ACTION, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        inventory.setItem(boundedSlot(slot, inventory.getSize()), item);
    }

    private String queueKey(String kitId, boolean ranked) {
        return kitId.toLowerCase(Locale.ROOT) + ":" + (ranked ? "ranked" : "unranked");
    }

    private ItemStack configuredItem(ConfigurationSection section, Map<String, String> placeholders) {
        if (section == null) {
            plugin.debug("ConfigurationSection is null in configuredItem");
            return new ItemStack(Material.STONE);
        }
        Material material = material(section.getString("material", "STONE"));
        String name = section.getString("name", "<white>Item</white>");
        List<String> lore = section.getStringList("lore");
        if (lore == null || lore.isEmpty()) {
            lore = List.of("<gray>No description</gray>");
        }
        return configuredItem(material, name, lore, placeholders);
    }

    private ItemStack configuredItem(Material material, String name, List<String> lore, Map<String, String> placeholders) {
        try {
            ItemStack item = new ItemStack(material != null && material != Material.AIR ? material : Material.STONE);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                meta = Bukkit.getItemFactory().getItemMeta(item.getType());
                if (meta == null) return item;
            }
            String displayName = name != null ? name : "<white>Item</white>";
            meta.displayName(plugin.messages().parse(replace(displayName, placeholders)));
            ArrayList<Component> lines = new ArrayList<>();
            if (lore != null) {
                for (String line : lore) {
                    lines.add(plugin.messages().parse(replace(line, placeholders)));
                }
            }
            meta.lore(lines);
            item.setItemMeta(meta);
            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("Error creating configured item: " + e.getMessage());
            return new ItemStack(Material.STONE);
        }
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
            String partyMessage = party == null
                    ? plugin.messages().get("gui.party-not-in-party", "<gray>You are not in a party.")
                    : "<gray>Members: <white>" + String.join(", ", party.members().stream().map(uuid -> {
                        return cachedPlayerName(uuid);
                    }).toList()) + "</white>";
            ItemStack info = configuredItem(Material.PLAYER_HEAD, "<green>Party Info</green>", List.of(
                    partyMessage,
                    "<gray>Click to show party details."), playerPlaceholders(player));
            ItemMeta infoMeta = info.getItemMeta();
            if (infoMeta == null) return;
            infoMeta.getPersistentDataContainer().set(Keys.MENU_ACTION, PersistentDataType.STRING, "PARTY_DETAILS");
            info.setItemMeta(infoMeta);
            inventory.setItem(11, info);
            ItemStack leave = configuredItem(Material.BARRIER, "<red>Leave Party</red>", List.of("<gray>Leave your active party."), playerPlaceholders(player));
            ItemMeta leaveMeta = leave.getItemMeta();
            if (leaveMeta == null) return;
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
            return cachedPlayerName(uuid);
        }).toList());

        inventory.setItem(11, configuredItem(Material.PLAYER_HEAD, "<gold>Party Members</gold>", List.of(
                "<gray>Leader: <white>" + cachedPlayerName(party.leader()) + "</white>",
                "<gray>Size: <white>" + party.size() + "</white>",
                "<gray>Members: <white>" + members + "</white>"
        ), playerPlaceholders(player)));

        ItemStack duel = configuredItem(Material.CROSSBOW, "<green>Queue Party Duel</green>", List.of("<gray>Leader only: choose a kit and queue party.", "<gray>Split into teams and fight."), playerPlaceholders(player));
        ItemMeta duelMeta = duel.getItemMeta();
        if (duelMeta == null) return;
        duelMeta.getPersistentDataContainer().set(Keys.MENU_ACTION, PersistentDataType.STRING, "PARTY_DUEL");
        duel.setItemMeta(duelMeta);
        inventory.setItem(13, duel);

        ItemStack ffa = configuredItem(Material.FIREWORK_ROCKET, "<aqua>Party FFA</aqua>", List.of("<gray>Leader only: choose a kit for party FFA.", "<gray>Everyone joins the FFA arena."), playerPlaceholders(player));
        ItemMeta ffaMeta = ffa.getItemMeta();
        if (ffaMeta == null) return;
        ffaMeta.getPersistentDataContainer().set(Keys.MENU_ACTION, PersistentDataType.STRING, "PARTY_FFA");
        ffa.setItemMeta(ffaMeta);
        inventory.setItem(15, ffa);

        ItemStack leaveParty = configuredItem(Material.BARRIER, "<red>Leave Party</red>", List.of("<gray>Leave the party immediately."), playerPlaceholders(player));
        ItemMeta leavePartyMeta = leaveParty.getItemMeta();
        if (leavePartyMeta == null) return;
        leavePartyMeta.getPersistentDataContainer().set(Keys.MENU_ACTION, PersistentDataType.STRING, "PARTY_LEAVE");
        leaveParty.setItemMeta(leavePartyMeta);
        inventory.setItem(17, leaveParty);

        player.openInventory(inventory);
    }

    private Map<String, String> playerPlaceholders(Player player) {
        return playerPlaceholders(player, plugin.profiles().getOrCreate(player));
    }

    private Map<String, String> playerPlaceholders(Player player, PlayerProfile profile) {
        // Use profile parameter directly to avoid redundant lookups
        if (profile == null) profile = plugin.profiles().getOrCreate(player);
        String status = plugin.queues().status(player.getUniqueId());
        Map<String, String> placeholders = new java.util.LinkedHashMap<>();
        placeholders.put("%player%", player.getName() != null ? player.getName() : "Unknown");
        placeholders.put("%elo%", String.valueOf(Math.max(0, profile.elo())));
        placeholders.put("%rank%", plugin.ranks().rankName(profile.elo()));
        placeholders.put("%wins%", String.valueOf(Math.max(0, profile.wins())));
        placeholders.put("%losses%", String.valueOf(Math.max(0, profile.losses())));
        placeholders.put("%kills%", String.valueOf(Math.max(0, profile.kills())));
        placeholders.put("%deaths%", String.valueOf(Math.max(0, profile.deaths())));
        placeholders.put("%streak%", String.valueOf(Math.max(0, profile.streak())));
        placeholders.put("%status%", status != null ? status : "Unknown");
        return placeholders;
    }

    private String cachedPlayerName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        return plugin.profiles().get(uuid).map(PlayerProfile::name).orElse("Unknown");
    }

    private Map<String, String> globalPlaceholders(Player player) {
        Map<String, String> placeholders = new java.util.LinkedHashMap<>(playerPlaceholders(player));
        placeholders.put("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
        placeholders.put("%queued%", String.valueOf(plugin.queues().totalPlayersQueued()));
        placeholders.put("%ffa_players%", String.valueOf(plugin.ffa().playerCount()));
        placeholders.put("%arenas%", String.valueOf(plugin.arenas().all().size()));
        return placeholders;
    }

    private Map<String, String> arenaPlaceholders(Player player, Arena arena) {
        Map<String, String> placeholders = new java.util.LinkedHashMap<>(globalPlaceholders(player));
        String defaultKit = plugin.ffa().defaultKit(arena).map(Kit::id).orElse("none");
        placeholders.put("%arena%", arena.name());
        placeholders.put("%arena_players%", String.valueOf(plugin.ffa().playerCount(arena)));
        placeholders.put("%arena_vip%", arena.vip() ? "true" : "false");
        placeholders.put("%default_kit%", defaultKit);
        placeholders.put("%arena_status%", arena.enabled() && arena.isFfaReady() ? "<green>Open</green>" : "<red>Unavailable</red>");
        return placeholders;
    }

    private Component title(String menu, String fallback) {
        return plugin.messages().parse(menus.getString("menus." + menu + ".title", fallback));
    }

    private int menuSize(String menu, int fallback) {
        int size = menus.getInt("menus." + menu + ".size", fallback);
        return Math.max(9, Math.min(54, ((size + 8) / 9) * 9));
    }

    private List<Integer> itemSlots(String menu, int inventorySize, int itemCount) {
        List<Integer> configured = menus.getIntegerList("menus." + menu + ".item-slots");
        if (!configured.isEmpty()) {
            return configured.stream()
                    .filter(slot -> slot >= 0 && slot < inventorySize)
                    .toList();
        }
        if (!menus.getBoolean("menus." + menu + ".center-items", true)) {
            return java.util.stream.IntStream.range(0, inventorySize).boxed().toList();
        }
        List<Integer> slots = new ArrayList<>();
        int remaining = Math.min(itemCount, inventorySize);
        int row = 0;
        while (remaining > 0 && row * 9 < inventorySize) {
            int inRow = Math.min(9, remaining);
            int start = row * 9 + (9 - inRow) / 2;
            for (int offset = 0; offset < inRow && start + offset < inventorySize; offset++) {
                slots.add(start + offset);
            }
            remaining -= inRow;
            row++;
        }
        return slots;
    }

    private int boundedSlot(int slot, int inventorySize) {
        return Math.max(0, Math.min(inventorySize - 1, slot));
    }

    private boolean isFiller(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && "FILLER".equals(meta.getPersistentDataContainer().get(Keys.MENU_ACTION, PersistentDataType.STRING));
    }

    private String replace(String input, Map<String, String> placeholders) {
        if (input == null || input.isBlank() || placeholders == null || placeholders.isEmpty()) {
            return input != null ? input : "";
        }
        String output = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            if (entry.getValue() != null) {
                output = output.replace(entry.getKey(), entry.getValue());
            }
        }
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
        Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
        if (enchantment == null) enchantment = Enchantment.getByName("DURABILITY");
        if (enchantment == null) enchantment = Enchantment.getByName("UNBREAKING");
        if (enchantment == null) return;
        meta.addEnchant(enchantment, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }

    public void refreshOpenMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof ZFfaGuiHolder holder)) continue;
            try {
                switch (holder.type()) {
                    case KIT_SELECTOR -> refreshKitSelectorItems(player);
                    case FFA_ARENAS -> refreshFfaArenaItems(player);
                    case LEADERBOARD -> populateLeaderboard(player.getOpenInventory().getTopInventory());
                    case MAIN, PARTY, PLAYER_MENU, MANAGEMENT, KIT_EDITOR, STATS, RANKS, DUEL_SELECTOR -> {
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error refreshing menu for " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    public void refreshLeaderboardsNow() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof ZFfaGuiHolder holder
                    && holder.type() == GuiType.LEADERBOARD) {
                populateLeaderboard(player.getOpenInventory().getTopInventory());
            }
        }
    }

    private void refreshKitSelectorItems(Player player) {
        Inventory inventory = player.getOpenInventory().getTopInventory();
        boolean partyFfa = containsAction(inventory, "QUEUE_PARTY_FFA");
        boolean ranked = !containsActionContaining(inventory, "UNRANKED");
        String targetName = findTargetPlayer(inventory);
        String customAction = partyFfa ? "QUEUE_PARTY_FFA" : null;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || !item.hasItemMeta()) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            String kitId = meta.getPersistentDataContainer().get(Keys.KIT_ID, PersistentDataType.STRING);
            if (kitId == null || kitId.isBlank()) continue;
            int targetSlot = slot;
            plugin.kits().get(kitId).ifPresent(kit -> inventory.setItem(targetSlot, kitItem(kit, ranked, targetName, customAction)));
        }
    }

    private void refreshFfaArenaItems(Player player) {
        Inventory inventory = player.getOpenInventory().getTopInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || !item.hasItemMeta()) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            String arenaId = meta.getPersistentDataContainer().get(Keys.ARENA_ID, PersistentDataType.STRING);
            if (arenaId == null || arenaId.isBlank()) continue;
            int targetSlot = slot;
            plugin.arenas().get(arenaId).ifPresent(arena -> inventory.setItem(targetSlot, arenaItem(arena, player)));
        }
    }

    private boolean containsAction(Inventory inventory, String expectedAction) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || !item.hasItemMeta()) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            String action = meta.getPersistentDataContainer().get(Keys.MENU_ACTION, PersistentDataType.STRING);
            if (expectedAction.equalsIgnoreCase(action)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsActionContaining(Inventory inventory, String expectedFragment) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || !item.hasItemMeta()) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            String action = meta.getPersistentDataContainer().get(Keys.MENU_ACTION, PersistentDataType.STRING);
            if (action != null && action.toUpperCase(Locale.ROOT).contains(expectedFragment.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String findTargetPlayer(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || !item.hasItemMeta()) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            String target = meta.getPersistentDataContainer().get(Keys.TARGET_PLAYER, PersistentDataType.STRING);
            if (target != null && !target.isBlank()) {
                return target;
            }
        }
        return null;
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
        if (duelMeta == null) return;
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
        if (statsMeta == null) return;
        statsMeta.getPersistentDataContainer().set(Keys.MENU_ACTION, PersistentDataType.STRING, "OPEN_STATS_TARGET");
        statsMeta.getPersistentDataContainer().set(Keys.TARGET_PLAYER, PersistentDataType.STRING, target.getName());
        stats.setItemMeta(statsMeta);
        inv.setItem(15, stats);

        viewer.openInventory(inv);
    }
}


