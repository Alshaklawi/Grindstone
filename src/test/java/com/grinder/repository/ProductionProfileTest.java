package com.grinder.repository;

import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.impl.CustomGoal;
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
        GrinderProfile profile = new GrinderProfile(42);
        assertTrue(profile.getGoals().isEmpty());
        assertTrue(TestProfiles.REPOSITORY.fromJson(TestProfiles.REPOSITORY.toJson(profile)).getGoals().isEmpty());
        assertTrue(new GrinderProfile(43, null).getGoals().isEmpty());
    }

    @Test
    public void savedPlayerGoalsAndLegacyExampleMetadataRemainIntact() {
        GrinderProfile profile = new GrinderProfile(42);
        CustomGoal goal = new CustomGoal("My routine", "My notes", GoalCategory.OTHER, 3, 10);
        profile.add(goal);
        profile.metadata(goal).setPinned(true);
        JsonObject saved = TestProfiles.REPOSITORY.toJson(profile);
        saved.getAsJsonArray("goals").get(0).getAsJsonObject().getAsJsonObject("metadata").addProperty("sample", true);
        GrinderProfile restored = TestProfiles.REPOSITORY.fromJson(saved);
        assertEquals(1, restored.getGoals().size());
        assertEquals("My routine", restored.getGoals().get(0).getTitle(null));
        assertEquals(3, restored.getGoals().get(0).getProgressCurrent());
        assertEquals(10, restored.getGoals().get(0).getProgressMaximum());
        assertTrue(restored.metadata(restored.getGoals().get(0)).isPinned());
    }
}
