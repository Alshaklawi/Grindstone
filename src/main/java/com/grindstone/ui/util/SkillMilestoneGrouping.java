package com.grindstone.ui.util;

import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.impl.experience.SessionExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.SkillExperienceMilestone;
import com.grindstone.content.milestone.impl.level.SkillLevelMilestone;
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
public final class SkillMilestoneGrouping {
    private SkillMilestoneGrouping() {
    }

    public static Skill skill(Milestone goal) {
        if (goal instanceof SessionExperienceMilestone) {
            SessionExperienceMilestone session = (SessionExperienceMilestone) goal;
            return session.isTotal() ? null : session.getSkill();
        }
        return goal instanceof SkillLevelMilestone ? ((SkillLevelMilestone) goal).getSkill() : goal instanceof SkillExperienceMilestone ? ((SkillExperienceMilestone) goal).getSkill() : null;
    }

    public static <T> List<List<T>> group(List<T> rows, Function<T, Milestone> goal) {
        Map<Object, List<T>> groups = new LinkedHashMap<>();
        for (T row : rows) {
            Skill skill = skill(goal.apply(row));
            groups.computeIfAbsent(skill == null ? new Object() : skill, key -> new ArrayList<>()).add(row);
        }
        return new ArrayList<>(groups.values());
    }
}
