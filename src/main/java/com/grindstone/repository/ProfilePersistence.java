package com.grindstone.repository;

import com.google.gson.JsonObject;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.Value;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.Filepath;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
@Singleton
public final class ProfilePersistence {
    public static final int MAX_SYNC_CHARACTERS = 262000;
    public static final String STORAGE_GROUP = "grindstoneData";
    private final GrindstoneRepository repository;
    private final Executor executor;
    private final SyncedProfiles syncedProfiles;
    private final Map<Long, String> profileKeys = new ConcurrentHashMap<>();
    private final Map<Long, String> expectedRemote = new HashMap<>();
    private CompletableFuture<Void> tail = CompletableFuture.completedFuture(null);
    private Directory directory;

    @Inject
    public ProfilePersistence(GrindstoneRepository repository, ScheduledExecutorService executor, ConfigManager configManager) {
        this(repository, executor, new SyncedProfiles() {
            @Override
            public String prepareProfile() {
                if (configManager.getRSProfileKey() == null) {
                    configManager.setRSProfileConfiguration(STORAGE_GROUP, "version", 1);
                }
                return configManager.getRSProfileKey();
            }

            @Override
            public String read(String profileKey) {
                return configManager.getConfiguration(STORAGE_GROUP, profileKey, "profile");
            }

            @Override
            public void write(String profileKey, String text) {
                configManager.setConfiguration(STORAGE_GROUP, profileKey, "profile", text);
            }

            @Override
            public void flush() {
                configManager.sendConfig();
            }
        });
    }

    ProfilePersistence(GrindstoneRepository repository, Executor executor) {
        this(repository, executor, null);
    }

    ProfilePersistence(GrindstoneRepository repository, Executor executor, SyncedProfiles syncedProfiles) {
        this.repository = repository;
        this.executor = executor;
        this.syncedProfiles = syncedProfiles;
    }

    public synchronized void configure(Directory directory) {
        this.directory = directory;
    }

    public CompletableFuture<GrindstoneProfile> load(long hash) {
        Directory root = getDirectory();
        String profileKey = syncedProfiles == null ? null : syncedProfiles.prepareProfile();
        if (profileKey != null) {
            profileKeys.put(hash, profileKey);
        }
        return enqueue(() -> {
            Filepath profiles = root.get().joinSegment("profiles");
            String saved = null;
            if (syncedProfiles != null) {
                requireProfileKey(profileKey);
                saved = syncedProfiles.read(profileKey);
                if (profiles.joinSegment(hash + ".sync-pending").exists()) {
                    if (!profiles.joinSegment(hash + ".json").exists() && !profiles.joinSegment(hash + ".json.bak").exists()) {
                        throw new IOException("The pending local profile is missing. Synced data has not been changed.");
                    }
                    JsonObject marker;
                    try {
                        marker = new JsonParser().parse(repository.readText(profiles.joinSegment(hash + ".sync-pending"), GrindstoneRepository.MAX_PROFILE_BYTES)).getAsJsonObject();
                    } catch (RuntimeException invalid) {
                        throw new SyncConflictException();
                    }
                    String base = marker.has("base") && !marker.get("base").isJsonNull() ? marker.get("base").getAsString() : null;
                    expectedRemote.put(hash, base);
                    if (!Objects.equals(base, saved)) {
                        throw new SyncConflictException();
                    }
                    return repository.load(profiles, hash);
                }
                expectedRemote.put(hash, saved);
                if (saved != null) {
                    if (saved.length() > MAX_SYNC_CHARACTERS) {
                        throw new IOException("The synced profile exceeds RuneLite's storage limit.");
                    }
                    GrindstoneProfile loaded = repository.readProfile(saved, hash);
                    repository.save(profiles, repository.toJson(loaded));
                    return loaded;
                }
            }
            GrindstoneProfile loaded = repository.load(profiles, hash);
            if (syncedProfiles != null) {
                String text = repository.toJson(loaded).toString();
                if (text.length() <= MAX_SYNC_CHARACTERS) {
                    publish(hash, profileKey, text);
                }
            }
            return loaded;
        });
    }

    public CompletableFuture<Void> save(JsonObject snapshot) {
        Directory root = getDirectory();
        long hash = snapshot.get("hash").getAsLong();
        String profileKey = profileKeys.get(hash);
        return enqueue(() -> {
            Filepath profiles = root.get().joinSegment("profiles");
            Filepath pending = profiles.joinSegment(hash + ".sync-pending");
            String text = repository.save(profiles, snapshot);
            if (syncedProfiles != null) {
                JsonObject marker = new JsonObject();
                marker.add("base", expectedRemote.get(hash) == null ? JsonNull.INSTANCE : new JsonPrimitive(expectedRemote.get(hash)));
                pending.write(marker.toString());
                requireProfileKey(profileKey);
                if (text.length() > MAX_SYNC_CHARACTERS) {
                    throw new SyncLimitException();
                }
                publish(hash, profileKey, text);
                pending.deleteIfExists();
            }
            return null;
        });
    }

    private void publish(long hash, String profileKey, String text) throws IOException {
        String current = syncedProfiles.read(profileKey);
        if (!expectedRemote.containsKey(hash) || !Objects.equals(expectedRemote.get(hash), current) && !Objects.equals(text, current)) {
            throw new SyncConflictException();
        }
        syncedProfiles.write(profileKey, text);
        expectedRemote.put(hash, text);
    }

    public CompletableFuture<Boolean> checkForConflict(long hash) {
        return enqueue(() -> {
            String key = profileKeys.get(hash);
            if (syncedProfiles == null || key == null || !expectedRemote.containsKey(hash)) {
                return false;
            }
            return !Objects.equals(expectedRemote.get(hash), syncedProfiles.read(key));
        });
    }

    public CompletableFuture<RecoveryState> getRecoveryState(long hash) {
        Directory root = getDirectory();
        return enqueue(() -> {
            Filepath profiles = root.get().joinSegment("profiles");
            String key = profileKeys.get(hash);
            String remote = syncedProfiles == null || key == null ? null : syncedProfiles.read(key);
            List<RecoveryOption> options = new ArrayList<>();
            for (String suffix : new String[]{".json", ".json.bak"}) {
                try {
                    GrindstoneProfile loaded = repository.readProfile(profiles.joinSegment(hash + suffix), hash);
                    options.add(new RecoveryOption(suffix.equals(".json") ? "Local save" : "Previous local save", repository.toJson(loaded)));
                } catch (IOException | RuntimeException unavailable) {
                    // Invalid source files are preserved and cannot be selected for recovery.
                }
            }
            if (remote != null) {
                try {
                    options.add(new RecoveryOption("Synced save", repository.toJson(repository.readProfile(remote, hash))));
                } catch (IOException | RuntimeException unavailable) {
                    // A rejected cloud value is preserved when another source is explicitly chosen.
                }
            }
            return new RecoveryState(remote, List.copyOf(options));
        });
    }

    public CompletableFuture<GrindstoneProfile> recover(long hash, JsonObject snapshot, String expected) {
        Directory root = getDirectory();
        JsonObject captured = snapshot.deepCopy();
        return enqueue(() -> {
            GrindstoneProfile restored = repository.readProfile(captured.toString(), hash);
            String key = profileKeys.get(hash);
            String remote = syncedProfiles == null || key == null ? null : syncedProfiles.read(key);
            if (!Objects.equals(expected, remote)) {
                throw new SyncConflictException();
            }
            if (syncedProfiles != null) {
                requireProfileKey(key);
            }
            Filepath profiles = root.get().joinSegment("profiles");
            profiles.createDirectories();
            long timestamp = System.currentTimeMillis();
            Filepath local = profiles.joinSegment(hash + ".json");
            if (local.exists()) {
                profiles.joinSegment(hash + ".recovery-local-" + timestamp + ".json").write(repository.readText(local, GrindstoneRepository.MAX_PROFILE_BYTES));
            }
            if (remote != null) {
                profiles.joinSegment(hash + ".recovery-sync-" + timestamp + ".json").write(remote);
            }
            String text = repository.save(profiles, captured);
            if (syncedProfiles != null) {
                expectedRemote.put(hash, remote);
                if (text.length() <= MAX_SYNC_CHARACTERS) {
                    publish(hash, key, text);
                } else {
                    JsonObject marker = new JsonObject();
                    marker.add("base", remote == null ? JsonNull.INSTANCE : new JsonPrimitive(remote));
                    profiles.joinSegment(hash + ".sync-pending").write(marker.toString());
                    return restored;
                }
            }
            profiles.joinSegment(hash + ".sync-pending").deleteIfExists();
            return restored;
        });
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 11/04/2026
     */
    @Value
    public static class RecoveryState {
        String remote;
        List<RecoveryOption> options;
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 11/04/2026
     */
    @Value
    public static class RecoveryOption {
        String title;
        JsonObject snapshot;

        @Override
        public String toString() {
            return title + " · " + snapshot.getAsJsonArray("goals").size() + " milestones";
        }
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 11/04/2026
     */
    public static final class SyncConflictException extends IOException {
        public SyncConflictException() {
            super("The synced profile changed on another device. Your local save has been kept; choose which profile to use.");
        }
    }

    private static void requireProfileKey(String profileKey) throws IOException {
        if (profileKey == null) {
            throw new IOException("RuneLite's character profile is not ready. The local save has been kept.");
        }
    }

    public CompletableFuture<Void> flush() {
        return enqueue(() -> null);
    }

    public CompletableFuture<Void> sync() {
        return enqueue(() -> {
            if (syncedProfiles != null) {
                syncedProfiles.flush();
            }
            return null;
        });
    }

    public synchronized <T> CompletableFuture<T> enqueue(Callable<T> operation) {
        CompletableFuture<T> queued = tail.handle((ignored, failure) -> null).thenApplyAsync(ignored -> {
            if (SwingUtilities.isEventDispatchThread()) {
                throw new IllegalStateException("Profile disk work must not run on the UI thread.");
            }
            try {
                return operation.call();
            } catch (Exception failure) {
                throw new CompletionException(failure);
            }
        }, executor);
        tail = queued.handle((ignored, failure) -> null);
        return queued.thenApply(value -> value);
    }

    private synchronized Directory getDirectory() {
        if (directory == null) {
            throw new IllegalStateException("Profile storage is not configured.");
        }
        return directory;
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 10/19/2026
     */
    @FunctionalInterface
    public interface Directory {
        Filepath get() throws IOException;
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 10/21/2026
     */
    interface SyncedProfiles {
        String prepareProfile();

        String read(String profileKey);

        void write(String profileKey, String text);

        default void flush() {
        }
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 10/21/2026
     */
    public static final class SyncLimitException extends IOException {
        private SyncLimitException() {
            super("Profile exceeds RuneLite's 262,000-character sync limit. A local recovery copy was saved.");
        }
    }
}
