package com.grinder.repository;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalPreset;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class GoalPresetsTest {
    @Test public void eachPackIsNonemptyFreshAndSafeToApplyTwice() {
        for (GoalPreset preset : GoalPreset.values()) {
            GrinderProfile profile = new GrinderProfile(42);
            profile.replace(Collections.emptyList());
            for (Goal goal : preset.createGoals()) { assertTrue(profile.add(goal)); }
            int count = profile.getGoals().size();
            assertTrue(count > 0);
            for (Goal goal : preset.createGoals()) { assertFalse(profile.add(goal)); }
            assertEquals(count, profile.getGoals().size());
            assertEquals(count, GrinderRepository.fromJson(GrinderRepository.toJson(profile)).getGoals().size());
        }
    }
}
