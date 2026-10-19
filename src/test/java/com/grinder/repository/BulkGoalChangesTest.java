package com.grinder.repository;

import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.impl.CustomGoal;
import com.grinder.content.goal.impl.level.BaseLevelGoal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class BulkGoalChangesTest {
    @Test
    public void clearingAllIncludesHiddenGoalsAndUndoRestoresPriorArchives() {
        GrinderProfile profile = TestProfiles.populated(42);
        CustomGoal archived = new CustomGoal("Archived", "", GoalCategory.OTHER, 1, 1);
        profile.add(archived);
        profile.metadata(archived).observe(archived, "Archived", System.currentTimeMillis());
        profile.remove(archived);
        List<Goal> before = new ArrayList<>(profile.getGoals());
        List<GrinderProfile.ArchivedHistory> history = new ArrayList<>(profile.archivedHistory());
        BulkGoalChanges.Change change = apply(profile, before, BulkGoalAction.DELETE);
        assertTrue(profile.getGoals().isEmpty());
        GrinderProfile saved = TestProfiles.REPOSITORY.fromJson(TestProfiles.REPOSITORY.toJson(profile));
        assertTrue(saved.getGoals().isEmpty());
        change.undo.run();
        assertEquals(before, profile.getGoals());
        assertEquals(history, new ArrayList<>(profile.archivedHistory()));
    }
    @Test
    public void pinAndCategoryChangesCanBeUndoneAsOneOperation() {
        GrinderProfile profile = TestProfiles.populated(42);
        List<Goal> selected = profile.getGoals().subList(0, 3);
        BulkGoalChanges.Change change = apply(profile, selected, BulkGoalAction.PIN);
        assertTrue(selected.stream().allMatch(goal -> profile.metadata(goal).isPinned()));
        change.undo.run();
        assertTrue(selected.stream().noneMatch(goal -> profile.metadata(goal).isPinned()));
        change = apply(profile, selected, BulkGoalAction.CATEGORY);
        assertTrue(selected.stream().allMatch(goal -> profile.metadata(goal).getDisplayCategory() == GoalCategory.OTHER));
        change.undo.run();
        assertTrue(selected.stream().allMatch(goal -> profile.metadata(goal).getDisplayCategory() == null));
    }

    @Test
    public void rejectsUnsupportedScheduleBeforeChangingAnyGoal() {
        GrinderProfile profile = TestProfiles.populated(42);
        CustomGoal custom = new CustomGoal("One", "", GoalCategory.OTHER, 2, 3);
        profile.add(custom);
        Goal unsupported = profile.findMatchingGoal(new BaseLevelGoal());
        try {
            apply(profile, Arrays.asList(custom, unsupported), BulkGoalAction.SCHEDULE);
            fail("Unsupported schedule should reject the whole batch");
        } catch (IllegalArgumentException expected) {
            assertEquals(2, custom.getProgressCurrent());
            assertEquals(RepeatSchedule.NONE, profile.metadata(custom).recurring().getSchedule());
        }
    }

    @Test
    public void deletesAndRestoresOriginalOrderAndMetadata() {
        GrinderProfile profile = TestProfiles.populated(42);
        List<Goal> before = new ArrayList<>(profile.getGoals());
        List<Goal> selected = new ArrayList<>(before.subList(0, 3));
        profile.metadata(selected.get(0)).setPinned(true);
        String id = profile.metadata(selected.get(0)).getId();
        BulkGoalChanges.Change change = apply(profile, selected, BulkGoalAction.DELETE);
        assertEquals(before.size() - 3, profile.getGoals().size());
        change.undo.run();
        assertEquals(before, profile.getGoals());
        assertEquals(id, profile.metadata(selected.get(0)).getId());
        assertTrue(profile.metadata(selected.get(0)).isPinned());
    }

    @Test
    public void scheduleChangeResetsManualProgressAndUndoRestoresIt() {
        GrinderProfile profile = TestProfiles.populated(42);
        CustomGoal goal = new CustomGoal("Repeat", "", GoalCategory.OTHER, 2, 3);
        profile.add(goal);
        BulkGoalChanges.Change change = apply(profile, Collections.singletonList(goal), BulkGoalAction.SCHEDULE);
        assertEquals(0, goal.getProgressCurrent());
        assertEquals(RepeatSchedule.DAILY, profile.metadata(goal).recurring().getSchedule());
        change.undo.run();
        assertEquals(2, goal.getProgressCurrent());
        assertEquals(3, goal.getProgressMaximum());
        assertEquals(RepeatSchedule.NONE, profile.metadata(goal).recurring().getSchedule());
    }

    private static BulkGoalChanges.Change apply(GrinderProfile profile, List<Goal> selected, BulkGoalAction action) {
        return BulkGoalChanges.apply(profile, new ArrayList<>(selected), action, GoalCategory.OTHER, RepeatSchedule.DAILY, 5, 0, false, System.currentTimeMillis());
    }
}
