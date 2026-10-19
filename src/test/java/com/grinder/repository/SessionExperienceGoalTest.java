package com.grinder.repository;

import com.grinder.content.goal.impl.experience.SessionExperienceGoal;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class SessionExperienceGoalTest {
    @Test
    public void capturesBaselineCountsGainsAndResetsExplicitly() {
        SessionExperienceGoal goal = new SessionExperienceGoal(Skill.ATTACK);
        goal.observeExperience(1_000_000);
        assertEquals(0, goal.getProgressCurrent());
        goal.observeExperience(1_000_100);
        assertEquals(100, goal.getProgressCurrent());
        goal.observeExperience(1_000_100);
        assertEquals(100, goal.getProgressCurrent());
        goal.restart();
        goal.observeExperience(1_000_200);
        assertEquals(0, goal.getProgressCurrent());
    }

    @Test
    public void savedConfigurationDoesNotPersistSessionReadings() {
        GrinderProfile profile = new GrinderProfile(42);
        SessionExperienceGoal goal = (SessionExperienceGoal) profile.findMatchingGoal(new SessionExperienceGoal(Skill.WOODCUTTING));
        goal.setTarget(20_000);
        goal.observeExperience(1_000_000);
        goal.observeExperience(1_005_000);
        GrinderProfile restored = TestProfiles.REPOSITORY.fromJson(TestProfiles.REPOSITORY.toJson(profile));
        assertEquals(20_000, restored.findMatchingGoal(goal).getProgressMaximum());
        assertEquals(0, restored.findMatchingGoal(goal).getProgressCurrent());
        assertFalse(RecurringGoal.supports(goal));
    }

    @Test
    public void editingTheSameSessionTargetKeepsObservedGains() {
        GrinderProfile profile = new GrinderProfile(42);
        SessionExperienceGoal source = (SessionExperienceGoal) profile.findMatchingGoal(new SessionExperienceGoal(Skill.WOODCUTTING));
        source.observeExperience(1_000_000);
        source.observeExperience(1_001_000);
        SessionExperienceGoal replacement = new SessionExperienceGoal(Skill.WOODCUTTING);
        replacement.setTarget(5_000);
        assertTrue(profile.replaceGoal(source, replacement));
        assertEquals(1_000, replacement.getProgressCurrent());
        replacement.observeExperience(1_001_500);
        assertEquals(1_500, replacement.getProgressCurrent());
    }
}
