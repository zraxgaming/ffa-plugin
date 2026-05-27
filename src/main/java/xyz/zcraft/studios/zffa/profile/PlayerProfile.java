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
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public PlayerProfile(UUID uuid, String name, int elo, int wins, int losses) {
        this(uuid, name, elo, wins, losses, 0, 0);
    }

    public PlayerProfile(UUID uuid, String name, int elo, int wins, int losses, int kills, int deaths) {
        this.uuid = uuid;
        this.name = name;
        this.elo = elo;
        this.wins = wins;
        this.losses = losses;
        this.kills = kills;
        this.deaths = deaths;
    }

    public static PlayerProfile fresh(UUID uuid, String name) {
        return new PlayerProfile(uuid, name, 1000, 0, 0);
    }

    public UUID uuid() { return uuid; }
    public String name() { return name; }
    public int elo() { return elo; }
    public int wins() { return wins; }
    public int losses() { return losses; }
    public int kills() { return kills; }
    public int deaths() { return deaths; }

    public void updateName(String name) {
        this.name = name;
        markDirty();
    }

    public void applyWin(int eloGain) {
        wins++;
        elo += eloGain;
        markDirty();
    }

    public void applyLoss(int eloLoss) {
        losses++;
        elo = Math.max(0, elo - eloLoss);
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

    public boolean markCleanIfDirty() {
        return dirty.getAndSet(false);
    }

    public void markDirty() {
        dirty.set(true);
    }
}
