package com.grindstone.repository;

import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.impl.CustomMilestone;
import com.google.gson.JsonObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class ProductionProfileTest {
    @Test
    public void newProfilesAndEmptySavedProfilesHaveNoGeneratedGoals() {
        GrindstoneProfile profile = new GrindstoneProfile(42);
        assertTrue(profile.getGoals().isEmpty());
        assertTrue(TestProfiles.REPOSITORY.fromJson(TestProfiles.REPOSITORY.toJson(profile)).getGoals().isEmpty());
        assertTrue(new GrindstoneProfile(43, null).getGoals().isEmpty());
    }

    @Test
    public void savedPlayerGoalsAndMetadataRemainIntact() {
        GrindstoneProfile profile = new GrindstoneProfile(42);
        CustomMilestone goal = new CustomMilestone("My routine", "My notes", MilestoneCategory.OTHER, 3, 10);
        profile.add(goal);
        profile.getMetadata(goal).setPinned(true);
        JsonObject saved = TestProfiles.REPOSITORY.toJson(profile);
        GrindstoneProfile restored = TestProfiles.REPOSITORY.fromJson(saved);
        assertEquals(1, restored.getGoals().size());
        assertEquals("My routine", restored.getGoals().get(0).getTitle(null));
        assertEquals(3, restored.getGoals().get(0).getProgressCurrent());
        assertEquals(10, restored.getGoals().get(0).getProgressMaximum());
        assertTrue(restored.getMetadata(restored.getGoals().get(0)).isPinned());
    }
}
