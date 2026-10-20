package com.grindstone.repository;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class ProfilePersistenceTest {
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
