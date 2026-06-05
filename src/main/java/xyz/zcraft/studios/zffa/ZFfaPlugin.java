package xyz.zcraft.studios.zffa;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import xyz.zcraft.studios.zffa.arena.ArenaManager;
import xyz.zcraft.studios.zffa.command.FfaCommand;
import xyz.zcraft.studios.zffa.command.LeaveCommand;
import xyz.zcraft.studios.zffa.command.PartyCommand;
import xyz.zcraft.studios.zffa.command.ZffaAdminCommand;
import xyz.zcraft.studios.zffa.config.ConfigUpdater;
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
import xyz.zcraft.studios.zffa.listener.CombatListener;
import xyz.zcraft.studios.zffa.listener.InventoryListener;
import xyz.zcraft.studios.zffa.listener.LobbyItemListener;
import xyz.zcraft.studios.zffa.listener.PlayerConnectionListener;
import xyz.zcraft.studios.zffa.listener.PlayerInteractionListener;
import xyz.zcraft.studios.zffa.listener.ProtectionListener;
import xyz.zcraft.studios.zffa.party.PartyManager;
import xyz.zcraft.studios.zffa.platform.PlatformScheduler;
import xyz.zcraft.studios.zffa.platform.ScheduledTaskHandle;
import xyz.zcraft.studios.zffa.profile.ProfileService;
import xyz.zcraft.studios.zffa.profile.RankManager;
import xyz.zcraft.studios.zffa.proxy.BackendProxyBridge;
import xyz.zcraft.studios.zffa.update.UpdateChecker;

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
    private ProtectionListener protection;
    private BackendProxyBridge proxyBridge;
    private PlatformScheduler platformScheduler;
    private ScheduledTaskHandle menuRefreshTask;
    private ConfigUpdater configUpdater;
    private UpdateChecker updateChecker;
    private final List<Object> retainedServices = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("kits.yml", false);
        saveResource("arenas.yml", false);
        saveResource("menus.yml", false);
        saveResource("messages.yml", false);
        this.configUpdater = new ConfigUpdater(this);
        configUpdater.updateDefaults();
        reloadConfig();
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
        this.platformScheduler = new PlatformScheduler(this);
        getLogger().info(platformScheduler.folia()
                ? "Folia-style scheduler detected; using platform scheduler compatibility mode."
                : "Using Bukkit scheduler compatibility mode.");
        Keys.init(this);
        this.gui = new GuiManager(this);
        this.proxyBridge = new BackendProxyBridge(this);

        integration.init();
        kits.reload();
        arenas.reload();
        gui.rebuild();
        proxyBridge.start();
        profiles.startAutoSave();
        queues.start();
        startMenuRefreshTask();
        this.updateChecker = new UpdateChecker(this);
        updateChecker.checkOnce();

        FfaCommand playerCommand = new FfaCommand(this);
        Objects.requireNonNull(getCommand("ffa")).setExecutor(playerCommand);
        Objects.requireNonNull(getCommand("ffa")).setTabCompleter(playerCommand);
        Objects.requireNonNull(getCommand("ffamenu")).setExecutor(playerCommand);
        Objects.requireNonNull(getCommand("ffastats")).setExecutor(playerCommand);
        Objects.requireNonNull(getCommand("streak")).setExecutor(playerCommand);
        Objects.requireNonNull(getCommand("streak")).setTabCompleter(playerCommand);
        Objects.requireNonNull(getCommand("ffatop")).setExecutor(playerCommand);
        Objects.requireNonNull(getCommand("ffaranks")).setExecutor(playerCommand);
        Objects.requireNonNull(getCommand("ffaitems")).setExecutor(playerCommand);
        Objects.requireNonNull(getCommand("duel")).setExecutor(playerCommand);
        Objects.requireNonNull(getCommand("duel")).setTabCompleter(playerCommand);
        Objects.requireNonNull(getCommand("ranked")).setExecutor(playerCommand);
        Objects.requireNonNull(getCommand("unranked")).setExecutor(playerCommand);
        LeaveCommand leaveCommand = new LeaveCommand(this);
        Objects.requireNonNull(getCommand("leave")).setExecutor(leaveCommand);
        Objects.requireNonNull(getCommand("leavequeue")).setExecutor(leaveCommand);
        Objects.requireNonNull(getCommand("leaveparty")).setExecutor(leaveCommand);
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
        this.protection = new ProtectionListener(this);
        Bukkit.getPluginManager().registerEvents(protection, this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            ZFfaPlaceholders expansion = new ZFfaPlaceholders(this);
            expansion.register();
            retainedServices.add(expansion);
            getLogger().info("PlaceholderAPI expansion registered.");
        }
    }

    @Override
    public void onDisable() {
        if (menuRefreshTask != null) menuRefreshTask.cancel();
        if (proxyBridge != null) proxyBridge.stop();
        if (queues != null) queues.stop();
        if (profiles != null) profiles.saveAllNow();
        if (matches != null) matches.shutdown();
        if (ffa != null) ffa.shutdown();
        if (storage != null) storage.close();
        if (databaseExecutor != null) databaseExecutor.shutdownNow();
        retainedServices.clear();
        printBanner("DISABLED");
    }

    public void reloadCore() {
        try {
            reloadConfig();
            configUpdater.updateDefaults();
            reloadConfig();
            debug("Config reloaded");
        } catch (Exception e) {
            logWarning("Error reloading config", e);
        }
        try {
            messages.reload();
            debug("Messages reloaded");
        } catch (Exception e) {
            logWarning("Error reloading messages", e);
        }
        try {
            integration.init();
            debug("Integrations reloaded");
        } catch (Exception e) {
            logWarning("Error reloading integrations", e);
        }
        try {
            proxyBridge.stop();
            proxyBridge.start();
            debug("Proxy bridge reloaded");
        } catch (Exception e) {
            logWarning("Error reloading proxy bridge", e);
        }
        try {
            ranks.reload();
            debug("Ranks reloaded");
        } catch (Exception e) {
            logWarning("Error reloading ranks", e);
        }
        try {
            kits.reload();
            debug("Kits reloaded");
        } catch (Exception e) {
            logWarning("Error reloading kits", e);
        }
        try {
            arenas.reload();
            debug("Arenas reloaded");
        } catch (Exception e) {
            logWarning("Error reloading arenas", e);
        }
        try {
            gui.rebuild();
            debug("GUI rebuilt");
        } catch (Exception e) {
            logWarning("Error rebuilding GUI", e);
        }
        restartMenuRefreshTask();
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
    public ProtectionListener protection() { return protection; }
    public BackendProxyBridge proxyBridge() { return proxyBridge; }
    public PlatformScheduler scheduler() { return platformScheduler; }

    public boolean debugEnabled() {
        return getConfig().getBoolean("settings.debug-enabled", false);
    }

    public void debug(String message) {
        if (debugEnabled()) getLogger().log(Level.INFO, "[DEBUG] {0}", message);
    }

    private void logWarning(String message, Exception exception) {
        getLogger().log(Level.WARNING, message, exception);
    }

    private void startMenuRefreshTask() {
        long seconds = getConfig().getLong("settings.menu-refresh-seconds", 30L);
        if (seconds <= 0L) {
            menuRefreshTask = null;
            debug("Dynamic menu refresh disabled.");
            return;
        }
        seconds = Math.max(10L, seconds);
        long periodTicks = seconds * 20L;
        menuRefreshTask = platformScheduler.runTimer(() -> {
            if (gui != null) {
                gui.refreshOpenMenus();
            }
        }, periodTicks, periodTicks);
    }

    private void restartMenuRefreshTask() {
        if (menuRefreshTask != null) {
            menuRefreshTask.cancel();
        }
        startMenuRefreshTask();
    }

    private void printBanner(String state) {
        getLogger().info(" ");
        getLogger().info("==================================================");
        getLogger().log(Level.INFO, "  Z-FFA Core - {0}", state);
        getLogger().info("  Brand: ZCraft Studios");
        getLogger().info("  Platform: Paper/Purpur 1.19+ | Java 17+");
        getLogger().info("==================================================");
        getLogger().info(" ");
    }

    private void initBStats() {
        try {
            int pluginId = 31638;
            retainedServices.add(new org.bstats.bukkit.Metrics(this, pluginId));
            getLogger().info("bStats metrics enabled.");
        } catch (Exception e) {
            logWarning("Failed to initialize bStats", e);
        }
    }
}
