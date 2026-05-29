package xyz.zcraft.studios.zffa;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.zcraft.studios.zffa.arena.ArenaManager;
import xyz.zcraft.studios.zffa.command.FfaCommand;
import xyz.zcraft.studios.zffa.command.PartyCommand;
import xyz.zcraft.studios.zffa.command.ZffaAdminCommand;
import xyz.zcraft.studios.zffa.config.MessageService;
import xyz.zcraft.studios.zffa.database.HikariStorage;
import xyz.zcraft.studios.zffa.database.StorageEngine;
import xyz.zcraft.studios.zffa.duel.MatchManager;
import xyz.zcraft.studios.zffa.duel.QueueManager;
import xyz.zcraft.studios.zffa.ffa.FfaManager;
import xyz.zcraft.studios.zffa.gui.GuiManager;
import xyz.zcraft.studios.zffa.gui.Keys;
import xyz.zcraft.studios.zffa.integration.IntegrationManager;
import xyz.zcraft.studios.zffa.integration.ZFfaPlaceholders;
import xyz.zcraft.studios.zffa.kit.KitManager;
import xyz.zcraft.studios.zffa.profile.RankManager;
import xyz.zcraft.studios.zffa.listener.CombatListener;
import xyz.zcraft.studios.zffa.listener.InventoryListener;
import xyz.zcraft.studios.zffa.listener.LobbyItemListener;
import xyz.zcraft.studios.zffa.listener.PlayerConnectionListener;
import xyz.zcraft.studios.zffa.listener.PlayerInteractionListener;
import xyz.zcraft.studios.zffa.listener.ProtectionListener;
import xyz.zcraft.studios.zffa.profile.ProfileService;
import xyz.zcraft.studios.zffa.party.PartyManager;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ZFfaPlugin extends JavaPlugin {
    private ExecutorService databaseExecutor;
    private MessageService messages;
    private StorageEngine storage;
    private ProfileService profiles;
    private KitManager kits;
    private ArenaManager arenas;
    private QueueManager queues;
    private MatchManager matches;
    private FfaManager ffa;
    private IntegrationManager integration;
    private PartyManager parties;
    private RankManager ranks;
    private GuiManager gui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("arenas.yml", false);
        saveResource("menus.yml", false);
        saveResource("messages.yml", false);
        printBanner("ENABLING");
        initBStats();

        this.databaseExecutor = Executors.newFixedThreadPool(4, task -> {
            Thread thread = new Thread(task, "zffa-storage");
            thread.setDaemon(true);
            return thread;
        });
        this.messages = new MessageService(this);
        this.storage = new HikariStorage(this, databaseExecutor);
        this.storage.init().join();

        this.profiles = new ProfileService(this, storage);
        this.kits = new KitManager(this);
        this.arenas = new ArenaManager(this);
        this.matches = new MatchManager(this);
        this.queues = new QueueManager(this, matches);
        this.ffa = new FfaManager(this);
        this.integration = new IntegrationManager(this);
        this.parties = new PartyManager(this);
        this.ranks = new RankManager(this);
        Keys.init(this);
        this.gui = new GuiManager(this);

        integration.init();
        kits.reload();
        arenas.reload();
        gui.rebuild();
        profiles.startAutoSave();
        queues.start();

        FfaCommand playerCommand = new FfaCommand(this);
        Objects.requireNonNull(getCommand("ffa")).setExecutor(playerCommand);
        Objects.requireNonNull(getCommand("ffa")).setTabCompleter(playerCommand);
        Objects.requireNonNull(getCommand("duel")).setExecutor(playerCommand);
        Objects.requireNonNull(getCommand("duel")).setTabCompleter(playerCommand);
        PartyCommand partyCommand = new PartyCommand(this);
        Objects.requireNonNull(getCommand("party")).setExecutor(partyCommand);
        Objects.requireNonNull(getCommand("party")).setTabCompleter(partyCommand);
        ZffaAdminCommand adminCommand = new ZffaAdminCommand(this);
        Objects.requireNonNull(getCommand("zffa")).setExecutor(adminCommand);
        Objects.requireNonNull(getCommand("zffa")).setTabCompleter(adminCommand);

        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new InventoryListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new LobbyItemListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerInteractionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ProtectionListener(this), this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new ZFfaPlaceholders(this).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }
    }

    @Override
    public void onDisable() {
        if (queues != null) queues.stop();
        if (profiles != null) profiles.saveAllNow();
        if (matches != null) matches.shutdown();
        if (ffa != null) ffa.shutdown();
        if (storage != null) storage.close();
        if (databaseExecutor != null) databaseExecutor.shutdownNow();
        printBanner("DISABLED");
    }

    public void reloadCore() {
        reloadConfig();
        messages.reload();
        integration.init();
        ranks.reload();
        kits.reload();
        arenas.reload();
        gui.rebuild();
    }

    public MessageService messages() { return messages; }
    public ProfileService profiles() { return profiles; }
    public KitManager kits() { return kits; }
    public ArenaManager arenas() { return arenas; }
    public QueueManager queues() { return queues; }
    public MatchManager matches() { return matches; }
    public FfaManager ffa() { return ffa; }
    public IntegrationManager integrations() { return integration; }
    public PartyManager parties() { return parties; }
    public RankManager ranks() { return ranks; }
    public GuiManager gui() { return gui; }

    public boolean debugEnabled() {
        return getConfig().getBoolean("settings.debug-enabled", false);
    }

    public void debug(String message) {
        if (debugEnabled()) getLogger().info("[DEBUG] " + message);
    }

    private void printBanner(String state) {
        getLogger().info(" ");
        getLogger().info("==================================================");
        getLogger().info("  Z-FFA Core - " + state);
        getLogger().info("  Brand: ZCraft Studios");
        getLogger().info("  Platform: Paper/Purpur 1.21.x | Java 21");
        getLogger().info("==================================================");
        getLogger().info(" ");
    }

    private void initBStats() {
        try {
            int pluginId = 31638;
            new org.bstats.bukkit.Metrics(this, pluginId);
            getLogger().info("bStats metrics enabled.");
        } catch (Exception e) {
            getLogger().warning("Failed to initialize bStats: " + e.getMessage());
        }
    }
}
