package xyz.zcraft.studios.zffa.database;

import xyz.zcraft.studios.zffa.profile.PlayerProfile;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StorageEngine extends AutoCloseable {
    CompletableFuture<Void> init();
    CompletableFuture<PlayerProfile> loadProfile(UUID uuid, String name);
    CompletableFuture<Void> saveProfile(PlayerProfile profile);
    @Override
    void close();
}
