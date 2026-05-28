package xyz.zcraft.studios.zffa.integration;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import xyz.zcraft.studios.zffa.ZFfaPlugin;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;

public final class IntegrationManager {
    private final ZFfaPlugin plugin;
    private Object vaultEconomy;
    private Class<?> economyClass;
    private Object luckPerms;
    private Class<?> luckPermsClass;
    private Object essentials;
    private Class<?> essentialsClass;

    public IntegrationManager(ZFfaPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        vaultEconomy = null;
        luckPerms = null;
        essentials = null;
        economyClass = null;
        luckPermsClass = null;
        essentialsClass = null;
        loadVault();
        loadLuckPerms();
        loadEssentials();
    }

    public boolean hasPlugin(String name) {
        return Bukkit.getPluginManager().isPluginEnabled(name);
    }

    public boolean hasVault() {
        return vaultEconomy != null;
    }

    public boolean hasLuckPerms() {
        return luckPerms != null;
    }

    public boolean hasEssentials() {
        return essentials != null;
    }

    public double getVaultBalance(OfflinePlayer player) {
        if (vaultEconomy == null || economyClass == null) return 0.0;
        try {
            Method getBalance = economyClass.getMethod("getBalance", OfflinePlayer.class);
            Object result = getBalance.invoke(vaultEconomy, player);
            if (result instanceof Number number) return number.doubleValue();
        } catch (Exception ignored) {
        }
        return 0.0;
    }

    public String getVaultBalanceFormatted(OfflinePlayer player) {
        if (vaultEconomy == null || economyClass == null) return "0";
        try {
            Method format = economyClass.getMethod("format", double.class);
            return String.valueOf(format.invoke(vaultEconomy, getVaultBalance(player)));
        } catch (Exception ignored) {
        }
        return String.valueOf(getVaultBalance(player));
    }

    public String getLuckPermsPrimaryGroup(UUID uuid) {
        if (luckPerms == null || luckPermsClass == null) return "";
        try {
            Method getUserManager = luckPermsClass.getMethod("getUserManager");
            Object userManager = getUserManager.invoke(luckPerms);
            if (userManager == null) return "";
            Method getUser = userManager.getClass().getMethod("getUser", UUID.class);
            Object user = getUser.invoke(userManager, uuid);
            if (user == null) return "";
            Method getPrimaryGroup = user.getClass().getMethod("getPrimaryGroup");
            Object group = getPrimaryGroup.invoke(user);
            if (group == null) return "";
            return String.valueOf(group);
        } catch (Exception ignored) {
        }
        return "";
    }

    public String getEssentialsNickname(OfflinePlayer player) {
        if (essentials == null || essentialsClass == null) return "";
        try {
            Method getUser = essentialsClass.getMethod("getUser", OfflinePlayer.class);
            Object user = getUser.invoke(essentials, player);
            if (user == null) return "";
            Method getNickname = user.getClass().getMethod("getNickname");
            Object nickname = getNickname.invoke(user);
            if (nickname == null) return "";
            return String.valueOf(nickname);
        } catch (Exception ignored) {
        }
        return "";
    }

    private void loadVault() {
        try {
            economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(economyClass);
            if (registration != null) vaultEconomy = registration.getProvider();
        } catch (ClassNotFoundException ignored) {
        }
    }

    private void loadLuckPerms() {
        try {
            luckPermsClass = Class.forName("net.luckperms.api.LuckPerms");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(luckPermsClass);
            if (registration != null) luckPerms = registration.getProvider();
        } catch (ClassNotFoundException ignored) {
        }
    }

    private void loadEssentials() {
        try {
            essentialsClass = Class.forName("com.earth2me.essentials.Essentials");
            Object pluginInstance = Bukkit.getPluginManager().getPlugin("Essentials");
            if (pluginInstance != null && essentialsClass.isInstance(pluginInstance)) {
                essentials = pluginInstance;
            }
        } catch (ClassNotFoundException ignored) {
        }
    }
}
