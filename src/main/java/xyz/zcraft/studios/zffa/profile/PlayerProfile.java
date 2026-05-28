package xyz.zcraft.studios.zffa.profile;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PlayerProfile {
    private final UUID uuid;
    private String name;
    private int elo;
    private int wins;
    private int losses;
    private int kills;
    private int deaths;
    private int streak;
    private int vouchers;
    private int killBoosts;
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public PlayerProfile(UUID uuid, String name, int elo, int wins, int losses) {
        this(uuid, name, elo, wins, losses, 0, 0, 0, 0, 0);
    }

    public PlayerProfile(UUID uuid, String name, int elo, int wins, int losses, int kills, int deaths) {
        this(uuid, name, elo, wins, losses, kills, deaths, 0, 0, 0);
    }

    public PlayerProfile(UUID uuid, String name, int elo, int wins, int losses, int kills, int deaths, int streak, int vouchers, int killBoosts) {
        this.uuid = uuid;
        this.name = name;
        this.elo = elo;
        this.wins = wins;
        this.losses = losses;
        this.kills = kills;
        this.deaths = deaths;
        this.streak = streak;
        this.vouchers = vouchers;
        this.killBoosts = killBoosts;
    }

    public static PlayerProfile fresh(UUID uuid, String name, int startingElo) {
        return new PlayerProfile(uuid, name, startingElo, 0, 0);
    }

    public static PlayerProfile fresh(UUID uuid, String name) {
        return fresh(uuid, name, 0);
    }

    public UUID uuid() { return uuid; }
    public String name() { return name; }
    public int elo() { return elo; }
    public int wins() { return wins; }
    public int losses() { return losses; }
    public int kills() { return kills; }
    public int deaths() { return deaths; }
    public int streak() { return streak; }
    public int vouchers() { return vouchers; }
    public int killBoosts() { return killBoosts; }

    public void updateName(String name) {
        this.name = name;
        markDirty();
    }

    public void applyWin(int eloGain) {
        wins++;
        elo += eloGain;
        streak++;
        markDirty();
    }

    public void applyLoss(int eloLoss) {
        losses++;
        elo = Math.max(0, elo - eloLoss);
        markDirty();
    }

    public void addElo(int amount) {
        if (amount <= 0) return;
        elo += amount;
        markDirty();
    }

    public void removeElo(int amount) {
        if (amount <= 0) return;
        elo = Math.max(0, elo - amount);
        markDirty();
    }

    public void setElo(int amount) {
        elo = Math.max(0, amount);
        markDirty();
    }

    public void applyKill() {
        kills++;
        markDirty();
    }

    public void applyDeath() {
        deaths++;
        markDirty();
    }

    public void resetStreak() {
        if (streak != 0) {
            streak = 0;
            markDirty();
        }
    }

    public void setStreak(int amount) {
        this.streak = Math.max(0, amount);
        markDirty();
    }

    public void addVouchers(int amount) {
        if (amount <= 0) return;
        vouchers += amount;
        markDirty();
    }

    public void setVouchers(int amount) {
        this.vouchers = Math.max(0, amount);
        markDirty();
    }

    public void removeVouchers(int amount) {
        if (amount <= 0) return;
        vouchers = Math.max(0, vouchers - amount);
        markDirty();
    }

    public boolean useVoucher() {
        if (vouchers <= 0) return false;
        vouchers--;
        markDirty();
        return true;
    }

    public void addKillBoosts(int amount) {
        if (amount <= 0) return;
        killBoosts += amount;
        markDirty();
    }

    public void setKillBoosts(int amount) {
        this.killBoosts = Math.max(0, amount);
        markDirty();
    }

    public void removeKillBoosts(int amount) {
        if (amount <= 0) return;
        killBoosts = Math.max(0, killBoosts - amount);
        markDirty();
    }

    public boolean useKillBoost() {
        if (killBoosts <= 0) return false;
        killBoosts--;
        markDirty();
        return true;
    }

    public boolean markCleanIfDirty() {
        return dirty.getAndSet(false);
    }

    public void markDirty() {
        dirty.set(true);
    }
}
