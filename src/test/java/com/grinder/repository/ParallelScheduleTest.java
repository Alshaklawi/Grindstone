package com.grinder.repository;

import com.grinder.content.goal.Goal;
import com.grinder.content.goal.impl.experience.SkillExperienceGoal;
import java.util.Collections;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class ParallelScheduleTest {
    private GrinderProfile profile() {
        GrinderProfile profile = TestProfiles.populated(42);
        profile.replace(Collections.emptyList());
        return profile;
    }
    private Goal add(GrinderProfile profile, RepeatSchedule schedule) {
        Goal goal = new SkillExperienceGoal(Skill.WOODCUTTING);
        GoalMetadata metadata = new GoalMetadata();
        metadata.recurring().configure(schedule, 100, 1_000);
        assertTrue(profile.add(goal, metadata));
        return goal;
    }
    @Test
    public void sameSubjectCanHaveLifetimeDailyWeeklyAndMonthlyTargets() {
        GrinderProfile profile = profile();
        for (RepeatSchedule schedule : RepeatSchedule.values()) {
            add(profile, schedule);
        }
        assertEquals(4, profile.getGoals().size());
        for (RepeatSchedule schedule : RepeatSchedule.values()) {
            assertNotNull(profile.findMatchingGoal(new SkillExperienceGoal(Skill.WOODCUTTING), schedule));
        }
        GoalMetadata duplicate = new GoalMetadata();
        duplicate.recurring().configure(RepeatSchedule.DAILY, 200, 1_000);
        assertFalse(profile.add(new SkillExperienceGoal(Skill.WOODCUTTING), duplicate));
        GrinderProfile restored = TestProfiles.REPOSITORY.fromJson(TestProfiles.REPOSITORY.toJson(profile));
        assertEquals(4, restored.getGoals().size());
        assertEquals(4, profile().mergeBackup(restored));
    }
    @Test
    public void editingCannotCollideWithAnotherSchedulesIdentity() {
        GrinderProfile profile = profile();
        Goal daily = add(profile, RepeatSchedule.DAILY);
        add(profile, RepeatSchedule.WEEKLY);
        try {
            profile.replaceGoal(daily, new SkillExperienceGoal(Skill.WOODCUTTING), RepeatSchedule.WEEKLY);
            fail("Duplicate schedule");
        } catch (IllegalArgumentException expected) {
            assertEquals(2, profile.getGoals().size());
        }
    }
}
