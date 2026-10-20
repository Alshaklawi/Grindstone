package com.grindstone.repository;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.CompletionException;
import net.runelite.client.util.Filepath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class ProfilePersistenceTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void migratesLocalProfileAndPrefersSyncedDataOnAnotherComputer() throws Exception {
        Filepath first = Filepath.Unchecked.getRooted(temporary.newFolder().toPath());
        Filepath second = Filepath.Unchecked.getRooted(temporary.newFolder().toPath());
        TestProfiles.REPOSITORY.save(first.joinSegment("profiles"), TestProfiles.REPOSITORY.toJson(TestProfiles.populated(42)));
        MemoryProfiles settings = new MemoryProfiles();
        ProfilePersistence source = new ProfilePersistence(TestProfiles.REPOSITORY, Runnable::run, settings);
        source.configure(() -> first);
        GrindstoneProfile migrated = source.load(42).join();
        assertEquals(1, migrated.getGoals().size());
        assertNotNull(settings.values.get("first"));
        assertTrue(first.joinSegment("profiles").joinSegment("42.json").exists());
        migrated.getMetadata(migrated.getGoals().get(0)).setPinned(true);
        source.save(TestProfiles.REPOSITORY.toJson(migrated)).join();
        TestProfiles.REPOSITORY.save(second.joinSegment("profiles"), TestProfiles.REPOSITORY.toJson(new GrindstoneProfile(42)));
        ProfilePersistence destination = new ProfilePersistence(TestProfiles.REPOSITORY, Runnable::run, settings);
        destination.configure(() -> second);
        GrindstoneProfile restored = destination.load(42).join();
        assertEquals(1, restored.getGoals().size());
        assertTrue(restored.getMetadata(restored.getGoals().get(0)).isPinned());
        settings.values.put("first", TestProfiles.REPOSITORY.exportBackup(TestProfiles.populated(99)));
        try {
            destination.load(42).join();
            fail("A different account's synced data must be rejected");
        } catch (CompletionException expected) {
            assertTrue(expected.getCause() instanceof IOException);
        }
    }

    @Test
    public void queuedSavesKeepTheirOriginalCharacterProfile() throws Exception {
        Queue<Runnable> scheduled = new ArrayDeque<>();
        MemoryProfiles settings = new MemoryProfiles();
        ProfilePersistence persistence = new ProfilePersistence(TestProfiles.REPOSITORY, scheduled::add, settings);
        Filepath root = Filepath.Unchecked.getRooted(temporary.newFolder().toPath());
        persistence.configure(() -> root);
        persistence.load(42);
        settings.current = "second";
        persistence.load(99);
        persistence.save(TestProfiles.REPOSITORY.toJson(TestProfiles.populated(42)));
        while (!scheduled.isEmpty()) {
            scheduled.remove().run();
        }
        assertEquals(1, TestProfiles.REPOSITORY.readProfile(settings.values.get("first"), 42).getGoals().size());
        assertTrue(TestProfiles.REPOSITORY.readProfile(settings.values.get("second"), 99).getGoals().isEmpty());
    }

    @Test
    public void syncLimitKeepsLastSyncedValueAndSavesLocalRecovery() throws Exception {
        MemoryProfiles settings = new MemoryProfiles();
        ProfilePersistence persistence = new ProfilePersistence(TestProfiles.REPOSITORY, Runnable::run, settings);
        Filepath root = Filepath.Unchecked.getRooted(temporary.newFolder().toPath());
        persistence.configure(() -> root);
        persistence.load(42).join();
        JsonObject snapshot = TestProfiles.REPOSITORY.toJson(TestProfiles.populated(42));
        snapshot.addProperty("padding", "");
        char[] padding = new char[ProfilePersistence.MAX_SYNC_CHARACTERS - snapshot.toString().length()];
        Arrays.fill(padding, 'x');
        snapshot.addProperty("padding", new String(padding));
        persistence.save(snapshot).join();
        String previous = settings.values.get("first");
        assertEquals(ProfilePersistence.MAX_SYNC_CHARACTERS, previous.length());
        snapshot.addProperty("padding", new String(padding) + "x");
        snapshot.getAsJsonArray("goals").get(0).getAsJsonObject().getAsJsonObject("metadata").addProperty("pinned", true);
        try {
            persistence.save(snapshot).join();
            fail("An oversized profile must not replace synced data");
        } catch (CompletionException expected) {
            assertTrue(expected.getCause() instanceof ProfilePersistence.SyncLimitException);
        }
        assertEquals(previous, settings.values.get("first"));
        assertEquals(1, TestProfiles.REPOSITORY.load(root.joinSegment("profiles"), 42).getGoals().size());
        ProfilePersistence restarted = new ProfilePersistence(TestProfiles.REPOSITORY, Runnable::run, settings);
        restarted.configure(() -> root);
        GrindstoneProfile recovered = restarted.load(42).join();
        assertTrue(recovered.getMetadata(recovered.getGoals().get(0)).isPinned());
        restarted.save(TestProfiles.REPOSITORY.toJson(recovered)).join();
        assertFalse(root.joinSegment("profiles").joinSegment("42.sync-pending").exists());
        GrindstoneProfile synced = TestProfiles.REPOSITORY.readProfile(settings.values.get("first"), 42);
        assertTrue(synced.getMetadata(synced.getGoals().get(0)).isPinned());
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 10/21/2026
     */
    private static final class MemoryProfiles implements ProfilePersistence.SyncedProfiles {
        private final Map<String, String> values = new HashMap<>();
        private String current = "first";

        @Override
        public String prepareProfile() {
            return current;
        }

        @Override
        public String read(String profileKey) {
            return values.get(profileKey);
        }

        @Override
        public void write(String profileKey, String text) {
            values.put(profileKey, text);
        }
    }

    @Test
    public void cancellingCallerDoesNotSkipQueuedWork() {
        Queue<Runnable> scheduled = new ArrayDeque<>();
        ProfilePersistence persistence = new ProfilePersistence(TestProfiles.REPOSITORY, (Executor) scheduled::add);
        List<Integer> order = new ArrayList<>();
        CompletableFuture<Integer> first = persistence.enqueue(() -> {
            order.add(1);
            return 1;
        });
        persistence.enqueue(() -> order.add(2));
        first.cancel(false);
        assertEquals(1, scheduled.size());
        scheduled.remove().run();
        assertEquals(Arrays.asList(1), order);
        scheduled.remove().run();
        assertEquals(Arrays.asList(1, 2), order);
    }

    @Test
    public void queuesWorkWithoutBlockingAndRecoversAfterFailure() {
        Queue<Runnable> scheduled = new ArrayDeque<>();
        ProfilePersistence persistence = new ProfilePersistence(TestProfiles.REPOSITORY, (Executor) scheduled::add);
        List<Integer> order = new ArrayList<>();
        CompletableFuture<Integer> first = persistence.enqueue(() -> {
            order.add(1);
            throw new IOException("Simulated disk failure");
        });
        CompletableFuture<Integer> second = persistence.enqueue(() -> {
            order.add(2);
            return 2;
        });
        assertFalse(first.isDone());
        assertFalse(second.isDone());
        assertEquals(1, scheduled.size());
        scheduled.remove().run();
        assertTrue(first.isCompletedExceptionally());
        assertFalse(second.isDone());
        scheduled.remove().run();
        assertEquals(Integer.valueOf(2), second.getNow(null));
        assertEquals(Arrays.asList(1, 2), order);
    }

    @Test
    public void metadataCopyDoesNotShareMutablePeriodState() {
        MilestoneMetadata original = new MilestoneMetadata();
        original.getRecurring().configure(RepeatSchedule.DAILY, 5, 1000);
        MilestoneMetadata copy = original.copy();
        copy.setPinned(true);
        copy.getRecurring().configure(RepeatSchedule.WEEKLY, 10, 2000);
        assertEquals(original.getId(), copy.getId());
        assertFalse(original.isPinned());
        assertEquals(RepeatSchedule.DAILY, original.getRecurring().getSchedule());
        assertEquals(5, original.getRecurring().getTarget());
    }
}
