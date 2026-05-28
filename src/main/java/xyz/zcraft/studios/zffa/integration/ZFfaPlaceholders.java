package xyz.zcraft.studios.zffa.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import xyz.zcraft.studios.zffa.ZFfaPlugin;
import xyz.zcraft.studios.zffa.profile.EloCalculator;
import xyz.zcraft.studios.zffa.profile.PlayerProfile;

public final class ZFfaPlaceholders extends PlaceholderExpansion {
    private final ZFfaPlugin plugin;

    public ZFfaPlaceholders(ZFfaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "zf";
    }

    @Override
    public String getAuthor() {
        return "ZCraft Studios";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || player.getUniqueId() == null) return "";
        PlayerProfile profile = plugin.profiles().get(player.getUniqueId()).orElse(null);
        if (profile == null) return "";
        return switch (params.toLowerCase()) {
            case "elo" -> String.valueOf(profile.elo());
            case "rank" -> EloCalculator.rank(profile.elo());
            case "wins" -> String.valueOf(profile.wins());
            case "losses" -> String.valueOf(profile.losses());
            case "kills" -> String.valueOf(profile.kills());
            case "deaths" -> String.valueOf(profile.deaths());
            case "streak" -> String.valueOf(profile.streak());
            case "vouchers" -> String.valueOf(profile.vouchers());
            case "killboost" -> String.valueOf(profile.killBoosts());
            case "status" -> plugin.queues().status(player.getUniqueId());
            case "vault_balance" -> plugin.integrations().hasVault() ? String.valueOf(plugin.integrations().getVaultBalance(player)) : "0";
            case "vault_balance_formatted" -> plugin.integrations().hasVault() ? plugin.integrations().getVaultBalanceFormatted(player) : "0";
            case "group" -> plugin.integrations().hasLuckPerms() ? plugin.integrations().getLuckPermsPrimaryGroup(player.getUniqueId()) : "";
            case "nickname" -> plugin.integrations().hasEssentials() ? plugin.integrations().getEssentialsNickname(player) : "";
            default -> null;
        };
    }
}
