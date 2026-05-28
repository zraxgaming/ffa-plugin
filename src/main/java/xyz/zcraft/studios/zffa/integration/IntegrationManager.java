package xyz.zcraft.studios.zffa.integration;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import xyz.zcraft.studios.zffa.ZFfaPlugin;

import java.lang.reflect.Method;
import java.util.UUID;

public final class IntegrationManager {

    private final ZFfaPlugin plugin;

    // Vault
    private Economy economy;

    // LuckPerms
    private Object luckPerms;
    private Class<?> luckPermsClass;

    // Essentials
    private Object essentials;
    private Class<?> essentialsClass;

    // CyberLevels
    private Object cyberLevels;
    private Class<?> cyberLevelsClass;

    public IntegrationManager(ZFfaPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        loadVault();
        loadLuckPerms();
        loadEssentials();
        loadCyberLevels();
    }

    public boolean hasPlugin(String name) {
        return Bukkit.getPluginManager().isPluginEnabled(name);
    }

    // =========================
    // Vault
    // =========================

    private void loadVault() {
        try {
            RegisteredServiceProvider<Economy> registration =
                    Bukkit.getServicesManager().getRegistration(Economy.class);

            if (registration != null) {
                economy = registration.getProvider();
                plugin.getLogger().info("Hooked into Vault.");
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to hook into Vault.");
        }
    }

    public boolean hasVault() {
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }

    public double getVaultBalance(OfflinePlayer player) {
        if (economy == null || player == null) {
            return 0.0;
        }

        try {
            return economy.getBalance(player);
        } catch (Exception ignored) {
        }

        return 0.0;
    }

    public String getVaultBalanceFormatted(OfflinePlayer player) {
        if (economy == null || player == null) {
            return "0";
        }

        try {
            return economy.format(getVaultBalance(player));
        } catch (Exception ignored) {
        }

        return String.valueOf(getVaultBalance(player));
    }

    // =========================
    // LuckPerms
    // =========================

    private void loadLuckPerms() {
        try {
            luckPermsClass = Class.forName("net.luckperms.api.LuckPerms");

            RegisteredServiceProvider<?> registration =
                    Bukkit.getServicesManager().getRegistration(luckPermsClass);

            if (registration != null) {
                luckPerms = registration.getProvider();
                plugin.getLogger().info("Hooked into LuckPerms.");
            }

        } catch (Exception ignored) {
            plugin.getLogger().warning("LuckPerms not found.");
        }
    }

    public boolean hasLuckPerms() {
        return luckPerms != null;
    }

    public String getLuckPermsPrimaryGroup(UUID uuid) {

        if (luckPerms == null || luckPermsClass == null) {
            return "";
        }

        try {
            Method getUserManager =
                    luckPermsClass.getMethod("getUserManager");

            Object userManager =
                    getUserManager.invoke(luckPerms);

            if (userManager == null) {
                return "";
            }

            Method getUser =
                    userManager.getClass().getMethod("getUser", UUID.class);

            Object user =
                    getUser.invoke(userManager, uuid);

            if (user == null) {
                return "";
            }

            Method getPrimaryGroup =
                    user.getClass().getMethod("getPrimaryGroup");

            Object group =
                    getPrimaryGroup.invoke(user);

            if (group == null) {
                return "";
            }

            return String.valueOf(group);

        } catch (Exception ignored) {
        }

        return "";
    }

    // =========================
    // Essentials
    // =========================

    private void loadEssentials() {

        try {
            essentialsClass =
                    Class.forName("com.earth2me.essentials.Essentials");

            Object pluginInstance =
                    Bukkit.getPluginManager().getPlugin("Essentials");

            if (pluginInstance != null &&
                    essentialsClass.isInstance(pluginInstance)) {

                essentials = pluginInstance;
                plugin.getLogger().info("Hooked into Essentials.");
            }

        } catch (Exception ignored) {
            plugin.getLogger().warning("Essentials not found.");
        }
    }

    public boolean hasEssentials() {
        return essentials != null;
    }

    public String getEssentialsNickname(OfflinePlayer player) {

        if (essentials == null || essentialsClass == null) {
            return "";
        }

        try {

            Method getUser =
                    essentialsClass.getMethod("getUser", OfflinePlayer.class);

            Object user =
                    getUser.invoke(essentials, player);

            if (user == null) {
                return "";
            }

            Method getNickname =
                    user.getClass().getMethod("getNickname");

            Object nickname =
                    getNickname.invoke(user);

            if (nickname == null) {
                return "";
            }

            return String.valueOf(nickname);

        } catch (Exception ignored) {
        }

        return "";
    }

    // =========================
    // CyberLevels
    // =========================

    private void loadCyberLevels() {

        try {

            cyberLevelsClass =
                    Class.forName("me.cyberknight.cyberlevels.CyberLevelsAPI");

            RegisteredServiceProvider<?> registration =
                    Bukkit.getServicesManager().getRegistration(cyberLevelsClass);

            if (registration != null) {
                cyberLevels = registration.getProvider();
                plugin.getLogger().info("Hooked into CyberLevels.");
            }

        } catch (Exception ignored) {
            plugin.getLogger().warning("CyberLevels not found.");
        }
    }

    public boolean hasCyberLevels() {
        return cyberLevels != null;
    }

    public int getCyberLevel(OfflinePlayer player) {

        if (cyberLevels == null || cyberLevelsClass == null) {
            return 0;
        }

        try {

            Method getLevel =
                    cyberLevelsClass.getMethod("getLevel", OfflinePlayer.class);

            Object result =
                    getLevel.invoke(cyberLevels, player);

            if (result instanceof Number number) {
                return number.intValue();
            }

        } catch (Exception ignored) {
        }

        return 0;
    }
}