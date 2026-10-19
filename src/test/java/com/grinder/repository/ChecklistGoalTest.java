package com.grinder.repository;

import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalStatus;
import com.grinder.content.goal.impl.ChecklistGoal;
import com.grinder.ui.editor.GoalEditorPanel;
import java.text.ParseException;
import java.time.Instant;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class ChecklistGoalTest {
    @Test
    public void checksHaveIndependentStateAndUndoRestoresThem() {
        GrinderProfile profile = TestProfiles.populated(42);
        ChecklistGoal goal = new ChecklistGoal("Supplies", "Prepare", Arrays.asList("Food", "Potions"));
        profile.add(goal);
        Consumer<Goal> undo = profile.snapshotEdit(goal);
        goal.setChecked(0, true);
        assertEquals(1, goal.getProgressCurrent());
        assertEquals(GoalStatus.IN_PROGRESS, goal.getStatus());
        boolean[] copy = goal.getChecks();
        copy[1] = true;
        assertEquals(1, goal.getProgressCurrent());
        goal.setChecked(1, true);
        assertEquals(GoalStatus.COMPLETE, goal.getStatus());
        undo.accept(goal);
        assertEquals(0, goal.getProgressCurrent());
    }

    @Test
    public void dailyChecklistArchivesAndResetsAllSteps() {
        ChecklistGoal goal = new ChecklistGoal("Routine", "", Arrays.asList("One", "Two"));
        RecurringGoal recurring = new RecurringGoal();
        long now = Instant.parse("2026-10-19T10:00:00Z").toEpochMilli();
        recurring.configure(RepeatSchedule.DAILY, 2, now);
        recurring.observe(goal, now);
        goal.setChecked(0, true);
        goal.setChecked(1, true);
        recurring.observe(goal, now + 1000);
        assertEquals(GoalStatus.COMPLETE, recurring.getStatus());
        recurring.observe(goal, now + 86_400_000);
        assertEquals(0, goal.getProgressCurrent());
        assertEquals(0, recurring.getProgress());
        assertEquals(2, recurring.getHistory().get(0).getProgress());
        assertTrue(recurring.getHistory().get(0).getCompletedAt() > 0);
    }

    @Test
    public void backupKeepsSameAccountChecksAndClearsOtherAccountProgress() {
        GrinderProfile profile = TestProfiles.populated(42);
        ChecklistGoal goal = (ChecklistGoal) profile.getGoals().stream().filter(item -> item instanceof ChecklistGoal).findFirst().get();
        goal.setChecked(0, true);
        String backup = TestProfiles.REPOSITORY.exportBackup(profile);
        assertEquals(1, TestProfiles.REPOSITORY.importBackup(backup, 42).findMatchingGoal(goal).getProgressCurrent());
        assertEquals(0, TestProfiles.REPOSITORY.importBackup(backup, 43).findMatchingGoal(goal).getProgressCurrent());
    }

    @Test
    public void editorPreservesChecksAndUsesEveryStepAsThePeriodTarget() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ChecklistGoal goal = new ChecklistGoal("Weekly", "Notes", Arrays.asList("One", "Two", "Three"));
            goal.setChecked(1, true);
            GoalEditorPanel form = new GoalEditorPanel(null, (query, callback) -> () -> {
            });
            try {
                form.loadGoal(goal);
                form.loadRepeat(RepeatSchedule.WEEKLY, 3);
                assertEquals(3, form.getRepeatTarget());
                ChecklistGoal result = (ChecklistGoal) form.createGoal();
                assertEquals(goal.getId(), result.getId());
                assertEquals(1, result.getProgressCurrent());
            } catch (ParseException failure) {
                throw new AssertionError(failure);
            } finally {
                form.stopSearch();
            }
        });
    }
}
