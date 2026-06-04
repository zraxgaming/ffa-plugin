package xyz.zcraft.studios.zffa.database;

import xyz.zcraft.studios.zffa.profile.PlayerProfile;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StorageEngine extends AutoCloseable {
    CompletableFuture<Void> init();
    CompletableFuture<PlayerProfile> loadProfile(UUID uuid, String name);
    CompletableFuture<Void> saveProfile(PlayerProfile profile);
    default CompletableFuture<Void> saveProfiles(Collection<PlayerProfile> profiles) {
        CompletableFuture<?>[] saves = profiles.stream().map(this::saveProfile).toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(saves);
    }
    @Override
    void close();
}
