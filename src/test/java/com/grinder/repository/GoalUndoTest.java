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
public class GoalUndoTest {
    @Test
    public void restoresManualDetailsAndMetadataWithoutTouchingOtherGoals() {
        GrinderProfile profile = TestProfiles.populated(42);
        CustomGoal goal = new CustomGoal("Routine", "Notes", GoalCategory.OTHER, 0, 5);
        profile.replace(Collections.singletonList(goal));
        Consumer<Goal> snapshot = profile.snapshotEdit(goal);
        goal.update("Changed", "", GoalCategory.OTHER, 5, 10);
        profile.touch();
        GoalUndo undo = new GoalUndo();
        undo.record(profile, "edit", () -> snapshot.accept(goal), () -> true);
        assertTrue(undo.undo(profile));
        assertEquals("Routine", goal.getTitle(null));
        assertEquals(0, goal.getProgressCurrent());
        assertEquals(5, goal.getProgressMaximum());
        assertFalse(undo.undo(profile));
    }
    @Test
    public void rejectsLaterEditsOtherAccountsAndExpiredPeriods() {
        GrinderProfile profile = TestProfiles.populated(42);
        GoalUndo undo = new GoalUndo();
        undo.record(profile, "edit", () -> fail("Must not run"), () -> true);
        profile.touch();
        assertFalse(undo.undo(profile));
        undo.record(profile, "edit", () -> fail("Must not run"), () -> true);
        assertFalse(undo.undo(TestProfiles.populated(42)));
        undo.record(profile, "edit", () -> fail("Must not run"), () -> false);
        assertFalse(undo.undo(profile));
    }
}
