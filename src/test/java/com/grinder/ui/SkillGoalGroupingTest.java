package com.grinder.ui;

import com.grinder.content.goal.Goal;
import com.grinder.content.goal.impl.level.SkillLevelGoal;
import com.grinder.content.goal.impl.experience.SkillExperienceGoal;
import com.grinder.content.goal.impl.level.BaseLevelGoal;
import com.grinder.ui.util.SkillGoalGrouping;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class SkillGoalGroupingTest {
    @Test
    public void groupsOnlyMatchingSkillsWithoutDroppingTargetsOrMergingOtherGoals() {
        Goal level = new SkillLevelGoal(Skill.ATTACK);
        Goal experience = new SkillExperienceGoal(Skill.ATTACK);
        Goal defence = new SkillLevelGoal(Skill.DEFENCE);
        Goal base = new BaseLevelGoal();
        Goal session = new com.grinder.content.goal.impl.experience.SessionExperienceGoal(Skill.ATTACK);
        List<List<Goal>> groups = SkillGoalGrouping.group(Arrays.asList(level, defence, base, experience, new BaseLevelGoal(), session), goal -> goal);
        assertEquals(4, groups.size());
        assertEquals(Arrays.asList(level, experience, session), groups.get(0));
        assertEquals(Arrays.asList(defence), groups.get(1));
        assertEquals(Arrays.asList(base), groups.get(2));
    }
}
