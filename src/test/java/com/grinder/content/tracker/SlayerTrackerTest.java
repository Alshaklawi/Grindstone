package com.grinder.content.tracker;

import com.grinder.content.goal.GoalCategory;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class SlayerTrackerTest {
    @Test
    public void slayerCountersReadSeparateNamedVarbitsInsteadOfVarps() {
        assertEquals(250, TrackerEntry.SLAYER_POINTS.readCount(id -> failVarp(), id -> id == VarbitID.SLAYER_POINTS ? 250 : 0));
        assertEquals(42, TrackerEntry.SLAYER_STREAK.readCount(id -> failVarp(), id -> id == VarbitID.SLAYER_TASKS_COMPLETED ? 42 : 0));
        assertEquals(12, TrackerEntry.SLAYER_WILDERNESS_STREAK.readCount(id -> failVarp(), id -> id == VarbitID.SLAYER_WILDERNESS_TASKS_COMPLETED ? 12 : 0));
        assertEquals(0, TrackerEntry.SLAYER_STREAK.readCount(id -> failVarp(), id -> -1));
        assertEquals(0, TrackerEntry.SLAYER_WILDERNESS_STREAK.readCount(id -> failVarp(), id -> 0));
        assertEquals(50, TrackerEntry.SLAYER_POINTS.readCount(id -> failVarp(), id -> 50));
    }

    @Test
    public void existingActivityCountersStillUseVarps() {
        assertEquals(18, TrackerEntry.WINTERTODT.readCount(id -> id == TrackerEntry.WINTERTODT.getId() ? 18 : 0, id -> failVarp()));
        assertEquals(5000, TrackerEntry.WINTERTODT.getDefaultTarget());
    }

    @Test
    public void slayerDefaultsHaveAppropriateTargetsIconsAndSourceDetails() {
        for (TrackerEntry entry : new TrackerEntry[]{TrackerEntry.SLAYER_POINTS, TrackerEntry.SLAYER_STREAK, TrackerEntry.SLAYER_WILDERNESS_STREAK}) {
            assertTrue(entry.isVarbit());
            assertEquals(GoalCategory.SKILLING, entry.getCategory());
            assertEquals(ItemID.SLAYER_HELM, entry.iconItem(null));
            assertFalse(entry.getDescription().isEmpty());
        }
        assertEquals(1000, TrackerEntry.SLAYER_POINTS.getDefaultTarget());
        assertEquals(100, TrackerEntry.SLAYER_STREAK.getDefaultTarget());
        assertEquals(100, TrackerEntry.SLAYER_WILDERNESS_STREAK.getDefaultTarget());
    }

    private static int failVarp() {
        throw new AssertionError("Wrong client variable reader");
    }
}
