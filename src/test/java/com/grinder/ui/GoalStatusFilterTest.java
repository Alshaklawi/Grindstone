package com.grinder.ui;

import com.grinder.content.goal.GoalStatus;
import com.grinder.content.goal.impl.collectionlog.CollectionLogItemGoal;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class GoalStatusFilterTest {
    @Test
    public void activeFilterIncludesOnlyActiveGoals() {
        assertTrue(GrinderPanel.matchesStatus(GoalStatus.IN_PROGRESS, "In progress", false));
        assertFalse(GrinderPanel.matchesStatus(GoalStatus.COMPLETE, "In progress", false));
        assertFalse(GrinderPanel.matchesStatus(GoalStatus.NOT_STARTED, "In progress", false));
    }

    @Test
    public void explicitCompleteFilterOverridesHideCompletedPreference() {
        assertTrue(GrinderPanel.matchesStatus(GoalStatus.COMPLETE, "Complete", true));
        assertFalse(GrinderPanel.matchesStatus(GoalStatus.COMPLETE, "All statuses", true));
        assertTrue(GrinderPanel.matchesStatus(GoalStatus.COMPLETE, "All statuses", false));
    }

    @Test
    public void unfinishedIncludesUnstartedAndActive() {
        assertTrue(GrinderPanel.matchesStatus(GoalStatus.NOT_STARTED, "Unfinished", false));
        assertTrue(GrinderPanel.matchesStatus(GoalStatus.IN_PROGRESS, "Unfinished", false));
        assertFalse(GrinderPanel.matchesStatus(GoalStatus.COMPLETE, "Unfinished", false));
    }

    @Test
    public void unobtainedItemIsNotIncorrectlyMarkedInProgress() {
        assertEquals(GoalStatus.NOT_STARTED, new CollectionLogItemGoal(ItemID.DRAGON_PICKAXE, 1).getStatus());
    }
}
