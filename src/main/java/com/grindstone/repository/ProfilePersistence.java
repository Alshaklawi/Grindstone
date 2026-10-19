package com.grindstone.repository;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.client.util.Filepath;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
@Singleton
public final class ProfilePersistence {
    private final GrindstoneRepository repository;
    private final Executor executor;
    private CompletableFuture<Void> tail = CompletableFuture.completedFuture(null);
    private Directory directory;

    @Inject
    public ProfilePersistence(GrindstoneRepository repository, ScheduledExecutorService executor) {
        this(repository, (Executor) executor);
    }

    ProfilePersistence(GrindstoneRepository repository, Executor executor) {
        this.repository = repository;
        this.executor = executor;
    }

    public synchronized void configure(Directory directory) {
        this.directory = directory;
    }

    public CompletableFuture<GrindstoneProfile> load(long hash) {
        Directory root = directory();
        return enqueue(() -> repository.load(root.get().joinSegment("profiles"), hash));
    }

    public CompletableFuture<Void> save(JsonObject snapshot) {
        Directory root = directory();
        return enqueue(() -> {
            repository.save(root.get().joinSegment("profiles"), snapshot);
            return null;
        });
    }

    public CompletableFuture<Void> flush() {
        return enqueue(() -> null);
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

    private synchronized Directory directory() {
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
}
