package com.grindstone.repository;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
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
    private static final String STORAGE_GROUP = "grindstoneData";
    private final GrindstoneRepository repository;
    private final Executor executor;
    private final SyncedProfiles syncedProfiles;
    private final Map<Long, String> profileKeys = new ConcurrentHashMap<>();
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
            if (syncedProfiles != null) {
                requireProfileKey(profileKey);
                String saved = syncedProfiles.read(profileKey);
                if (saved != null) {
                    if (saved.length() > MAX_SYNC_CHARACTERS) {
                        throw new IOException("The synced profile exceeds RuneLite's storage limit.");
                    }
                    return repository.readProfile(saved, hash);
                }
            }
            GrindstoneProfile loaded = repository.load(root.get().joinSegment("profiles"), hash);
            if (syncedProfiles != null) {
                String text = repository.toJson(loaded).toString();
                if (text.length() <= MAX_SYNC_CHARACTERS) {
                    syncedProfiles.write(profileKey, text);
                }
            }
            return loaded;
        });
    }

    public CompletableFuture<Void> save(JsonObject snapshot) {
        Directory root = getDirectory();
        String profileKey = profileKeys.get(snapshot.get("hash").getAsLong());
        return enqueue(() -> {
            repository.save(root.get().joinSegment("profiles"), snapshot);
            if (syncedProfiles != null) {
                requireProfileKey(profileKey);
                String text = snapshot.toString();
                if (text.length() > MAX_SYNC_CHARACTERS) {
                    throw new SyncLimitException();
                }
                syncedProfiles.write(profileKey, text);
            }
            return null;
        });
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
