package com.grinder.repository;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.impl.CustomGoal;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class GoalHistoryTest {
    @Test
    public void observesEachTargetOnceAndKeepsHistoryAfterProgressRegresses() {
        CustomGoal goal = new CustomGoal("Routine", "", GoalCategory.OTHER, 0, 10);
        GoalMetadata metadata = new GoalMetadata();
        assertFalse(metadata.observe(goal, "Routine", 1000));
        goal.update("Routine", "", GoalCategory.OTHER, 10, 10);
        assertTrue(metadata.observe(goal, "Routine", 1000));
        assertFalse(metadata.observe(goal, "Routine", 2000));
        goal.update("Routine", "", GoalCategory.OTHER, 0, 20);
        assertEquals(1, metadata.getCompletions().size());
        goal.update("Routine", "", GoalCategory.OTHER, 20, 20);
        assertTrue(metadata.observe(goal, "Routine", 3000));
        assertEquals(2, metadata.getCompletions().size());
    }
    @Test
    public void metadataIdentityAndObservedDatesSurviveProfileReload() {
        GrinderProfile profile = TestProfiles.populated(42);
        CustomGoal goal = new CustomGoal("Routine", "", GoalCategory.OTHER, 10, 10);
        profile.replace(Collections.singletonList(goal));
        profile.metadata(goal).observe(goal, "Routine", 1000);
        profile.metadata(goal).setPinned(true);
        profile.metadata(goal).setCollapsed(true);
        String id = profile.metadata(goal).getId();
        GrinderProfile restored = TestProfiles.REPOSITORY.fromJson(TestProfiles.REPOSITORY.toJson(profile));
        GoalMetadata metadata = restored.metadata(restored.getGoals().get(0));
        assertEquals(id, metadata.getId());
        assertTrue(metadata.isPinned());
        assertTrue(metadata.isCollapsed(false));
        assertEquals(1000, metadata.getCompletions().get(0).getObservedAt());
        assertTrue(metadata.historyText().contains("not the historical"));
    }
}
