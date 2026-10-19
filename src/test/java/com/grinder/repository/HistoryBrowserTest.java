package com.grinder.repository;

import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.impl.CustomGoal;
import java.util.Collections;
import java.util.function.Consumer;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class HistoryBrowserTest {
    @Test public void changingSubjectsArchivesHistoryAndUndoRestoresItsOriginalIdentity() {
        GrinderProfile profile = new GrinderProfile(42);
        CustomGoal original = new CustomGoal("Original", "", GoalCategory.OTHER, 1, 1);
        profile.replace(Collections.singletonList(original));
        profile.metadata(original).observe(original, "Original", 1000);
        Consumer<Goal> restore = profile.snapshotEdit(original);
        CustomGoal replacement = new CustomGoal("Replacement", "", GoalCategory.OTHER, 0, 1);
        profile.replaceGoal(original, replacement);
        assertEquals(1, profile.archivedHistory().size());
        assertTrue(profile.metadata(replacement).getCompletions().isEmpty());
        restore.accept(replacement);
        assertTrue(profile.archivedHistory().isEmpty());
        assertEquals(1, profile.metadata(original).getCompletions().size());
        assertSame(original, profile.getGoals().get(0));
    }
    @Test public void filtersBySearchScheduleAndOverlappingDateRange() {
        HistoryRecord record = new HistoryRecord("id", "Woodcutting XP", RepeatSchedule.DAILY, 100, 200, 50L, 100, "Incomplete", 110, 190, 0, false);
        assertTrue(record.matches("wood", RepeatSchedule.DAILY, 150, 170));
        assertFalse(record.matches("fishing", null, 0, 300));
        assertFalse(record.matches("", RepeatSchedule.WEEKLY, 0, 300));
        assertFalse(record.matches("", null, 200, 300));
    }
    @Test public void deletingPreservesObservedHistoryWithoutCreatingALiveGoalLink() {
        GrinderProfile profile = new GrinderProfile(42);
        CustomGoal goal = new CustomGoal("Routine", "", GoalCategory.OTHER, 1, 1);
        profile.replace(Collections.singletonList(goal));
        profile.metadata(goal).observe(goal, "Routine", 1000);
        profile.remove(goal);
        GrinderProfile restored = TestProfiles.REPOSITORY.fromJson(TestProfiles.REPOSITORY.toJson(profile));
        assertTrue(restored.getGoals().isEmpty());
        assertEquals(1, restored.archivedHistory().size());
        assertEquals(1, HistoryRecord.snapshot(restored, value -> value.getTitle(null)).size());
        assertEquals("", HistoryRecord.snapshot(restored, value -> value.getTitle(null)).get(0).goalId);
        assertTrue(TestProfiles.REPOSITORY.importBackup(TestProfiles.REPOSITORY.exportBackup(profile), 99).archivedHistory().isEmpty());
        assertTrue(profile.add(goal, profile.metadata(goal)));
        assertTrue(profile.archivedHistory().isEmpty());
    }
}
