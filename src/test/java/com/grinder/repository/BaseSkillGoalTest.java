package com.grinder.repository;

import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalStatus;
import com.grinder.content.goal.impl.experience.BaseExperienceGoal;
import com.grinder.content.goal.impl.level.BaseLevelGoal;
import com.grinder.ui.editor.GoalEditorPanel;
import com.grinder.ui.editor.GoalTargetModels;
import java.text.ParseException;
import javax.swing.SwingUtilities;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class BaseSkillGoalTest {
    @Test
    public void baseLevelRequiresEverySkillRatherThanAverage() {
        BaseLevelGoal goal = new BaseLevelGoal();
        goal.setTarget(70);
        assertTrue(goal.updateReadings(skill -> skill == Skill.ATTACK ? 69 : 99));
        assertEquals(69, goal.getProgressCurrent());
        assertEquals(GoalStatus.IN_PROGRESS, goal.getStatus());
        assertTrue(goal.getDescription().contains("Attack"));
        assertFalse(goal.updateReadings(skill -> skill == Skill.ATTACK ? 69 : 99));
        assertTrue(goal.updateReadings(skill -> 70));
        assertEquals(GoalStatus.COMPLETE, goal.getStatus());
        assertTrue(goal.getProgressText().contains(Skill.values().length + "/" + Skill.values().length));
    }

    @Test
    public void baseExperienceHandlesZeroAndCaps() {
        BaseExperienceGoal goal = new BaseExperienceGoal();
        goal.setTarget(200_000_000);
        goal.updateReadings(skill -> skill == Skill.ATTACK ? 0 : 200_000_000);
        assertEquals(0, goal.getProgressCurrent());
        assertNotEquals(GoalStatus.COMPLETE, goal.getStatus());
        goal.updateReadings(skill -> 200_000_000);
        assertEquals(GoalStatus.COMPLETE, goal.getStatus());
        rejects(() -> goal.setTarget(200_000_001));
        rejects(() -> new BaseLevelGoal().setTarget(100));
        rejects(() -> goal.setTarget(0));
    }

    @Test
    public void savesTargetsWithoutCarryingClientReadingsAcrossAccounts() {
        GrinderProfile profile = TestProfiles.populated(42);
        BaseLevelGoal goal = (BaseLevelGoal) profile.findMatchingGoal(new BaseLevelGoal());
        goal.setTarget(80);
        goal.updateReadings(skill -> 99);
        GrinderProfile restored = TestProfiles.REPOSITORY.importBackup(TestProfiles.REPOSITORY.exportBackup(profile), 43);
        Goal copy = restored.findMatchingGoal(new BaseLevelGoal());
        assertEquals(80, copy.getProgressMaximum());
        assertEquals(0, copy.getProgressCurrent());
        assertFalse(restored.add(new BaseLevelGoal()));
        assertFalse(RecurringGoal.supports(copy));
    }

    @Test
    public void editorRoundTripsBothBaseTypesWithCorrectLimits() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GoalEditorPanel editor = new GoalEditorPanel(null, (query, callback) -> () -> {
            });
            try {
                for (Goal goal : new Goal[]{new BaseLevelGoal(), new BaseExperienceGoal()}) {
                    editor.loadGoal(goal);
                    assertTrue(editor.isReady());
                    try {
                        Goal created = editor.createGoal();
                        assertEquals(goal.getClass(), created.getClass());
                        assertEquals(goal.getProgressMaximum(), created.getProgressMaximum());
                    } catch (ParseException failure) {
                        throw new AssertionError(failure);
                    }
                }
                assertEquals(99L, GoalTargetModels.create(new BaseLevelGoal()).getMaximum());
                assertEquals(200_000_000L, GoalTargetModels.create(new BaseExperienceGoal()).getMaximum());
            } finally {
                editor.stopSearch();
            }
        });
    }

    private static void rejects(Runnable operation) {
        try {
            operation.run();
            fail("Expected invalid target to be rejected");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }
}
