package com.grinder.repository;

import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.GoalStatus;
import com.grinder.content.goal.impl.CustomGoal;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class CustomGoalTest {
    @Test
    public void customDetailsAndIdentitySurviveReload() {
        GrinderProfile profile = new GrinderProfile(42);
        profile.replace(Collections.emptyList());
        CustomGoal goal = new CustomGoal("  Bank supplies  ", "First line\n<plain text> & notes", GoalCategory.SKILLING, 3, 10);
        assertTrue(profile.add(goal));
        CustomGoal restored = (CustomGoal) TestProfiles.REPOSITORY.fromJson( TestProfiles.REPOSITORY.toJson(profile)).getGoals().get(0);
        assertEquals(goal.getId(), restored.getId());
        assertEquals("Bank supplies", restored.getTitle(null));
        assertEquals(goal.getDescription(), restored.getDescription());
        assertEquals(GoalCategory.SKILLING, restored.getCategory());
        assertEquals(3, restored.getProgressCurrent());
        assertEquals(10, restored.getProgressMaximum());
        assertEquals(GoalStatus.IN_PROGRESS, restored.getStatus());
    }

    @Test
    public void separateCustomGoalsCanShareATitle() {
        GrinderProfile profile = new GrinderProfile(42);
        profile.replace(Collections.emptyList());
        CustomGoal first = new CustomGoal("Prepare", "Food", GoalCategory.OTHER, 0, 1);
        CustomGoal second = new CustomGoal("Prepare", "Potions", GoalCategory.OTHER, 0, 1);
        assertTrue(profile.add(first));
        assertTrue(profile.add(second));
        assertFalse(profile.add(first));
        assertEquals(2, profile.getGoals().size());
    }

    @Test
    public void editingAndCompletingPreservesIdentity() {
        CustomGoal goal = new CustomGoal("Prepare", "", GoalCategory.OTHER, 0, 1);
        String id = goal.getId();
        assertEquals(GoalStatus.NOT_STARTED, goal.getStatus());
        goal.update("Ready", "All supplies collected", GoalCategory.BOSSING, 10, 10);
        assertEquals(id, goal.getId());
        assertEquals(GoalStatus.COMPLETE, goal.getStatus());
        assertEquals(GoalCategory.BOSSING, goal.getCategory());
    }

    @Test
    public void invalidEditDoesNotPartiallyChangeGoal() {
        CustomGoal goal = new CustomGoal("Original", "Notes", GoalCategory.OTHER, 2, 10);
        try {
            goal.update("Changed", "Other notes", GoalCategory.BOSSING, 11, 10);
            fail("Progress above the target must be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals("Original", goal.getTitle(null));
            assertEquals("Notes", goal.getDescription());
            assertEquals(2, goal.getProgressCurrent());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void blankTitleIsRejected() {
        new CustomGoal("   ", "", GoalCategory.OTHER, 0, 1);
    }
}
