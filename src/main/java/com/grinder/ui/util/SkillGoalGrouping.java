package com.grinder.ui.util;

import com.grinder.content.goal.Goal;
import com.grinder.content.goal.impl.experience.SkillExperienceGoal;
import com.grinder.content.goal.impl.level.SkillLevelGoal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.runelite.api.Skill;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class SkillGoalGrouping {
    private SkillGoalGrouping() {
    }

    public static Skill skill(Goal goal) {
        return goal instanceof SkillLevelGoal ? ((SkillLevelGoal) goal).getSkill() : goal instanceof SkillExperienceGoal ? ((SkillExperienceGoal) goal).getSkill() : null;
    }

    public static <T> List<List<T>> group(List<T> rows, Function<T, Goal> goal) {
        Map<Object, List<T>> groups = new LinkedHashMap<>();
        for (T row : rows) {
            Skill skill = skill(goal.apply(row));
            groups.computeIfAbsent(skill == null ? new Object() : skill, key -> new ArrayList<>()).add(row);
        }
        return new ArrayList<>(groups.values());
    }
}
