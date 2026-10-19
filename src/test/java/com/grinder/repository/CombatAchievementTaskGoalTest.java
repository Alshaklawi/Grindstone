package com.grinder.repository;
import com.grinder.content.goal.GoalStatus;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementTaskGoal;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class CombatAchievementTaskGoalTest {
    @Test public void individualTasksUseBinaryCompletionAndIndependentIdentity() {
        CombatAchievementTaskGoal task = new CombatAchievementTaskGoal(0, "Test task", "Test requirements");
        task.observeCompletion(false);
        assertEquals(GoalStatus.NOT_STARTED, task.getStatus());
        assertEquals(1, task.getProgressMaximum());
        task.observeCompletion(true);
        assertEquals(GoalStatus.COMPLETE, task.getStatus());
        assertEquals(1, task.getProgressCurrent());
        GrinderProfile profile = new GrinderProfile(42);
        profile.replace(Collections.emptyList());
        assertTrue(profile.add(task));
        assertFalse(profile.add(new CombatAchievementTaskGoal(0, "Renamed task", "")));
        assertTrue(profile.add(new CombatAchievementTaskGoal(1, "Other task", "")));
    }
    @Test public void taskSelectionsAndRequirementsPersistWithoutClassNames() {
        GrinderProfile profile = new GrinderProfile(42);
        profile.replace(Collections.singletonList(new CombatAchievementTaskGoal(31, "Test task", "Keep these requirements")));
        String backup = TestProfiles.REPOSITORY.exportBackup(profile);
        assertTrue(backup.contains("combat-task"));
        assertFalse(backup.contains("com.grinder."));
        CombatAchievementTaskGoal restored = (CombatAchievementTaskGoal) TestProfiles.REPOSITORY.importBackup(backup, 42).getGoals().get(0);
        assertEquals(31, restored.getTask());
        assertEquals("Keep these requirements", restored.getDescription());
    }
    @Test(expected = IllegalArgumentException.class) public void invalidNativeTaskIndexIsRejected() {
        new CombatAchievementTaskGoal(-1, "Bad", "");
    }
}
