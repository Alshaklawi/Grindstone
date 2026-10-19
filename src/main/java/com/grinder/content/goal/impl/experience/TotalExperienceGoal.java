package com.grinder.content.goal.impl.experience;

import com.grinder.config.GrinderConfig;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.GoalStatus;
import com.grinder.ui.Icon;
import com.grinder.ui.components.GoalComponent;
import com.grinder.util.NumberFormats;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.Skill;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class TotalExperienceGoal implements Goal {
    private transient long current;
    private long target;

    @Override
    public boolean supportsTarget() {
        return true;
    }

    @Override
    public void setTarget(long target) {
        if (target < 1 || target > Skill.values().length * 200_000_000L) {
            throw new IllegalArgumentException("Target exceeds the total XP cap");
        }
        this.target = target;
    }

    @Override
    public String getTitle(Client client) {
        return "Total experience";
    }

    @Override
    public BufferedImage getIcon(Client client) {
        return Icon.SKILLING.getImage();
    }

    @Override
    public GoalCategory getCategory() {
        return GoalCategory.SKILLING;
    }

    @Override
    public GoalStatus getStatus() {
        return current >= getMaximumExperience() ? GoalStatus.COMPLETE
                : current > 0 ? GoalStatus.IN_PROGRESS : GoalStatus.NOT_STARTED;
    }

    @Override
    public boolean hasProgress() {
        return true;
    }

    @Override
    public long getProgressCurrent() {
        return current;
    }

    @Override
    public long getProgressMaximum() {
        return getMaximumExperience();
    }

    @Override
    public String getProgressText() {
        return NumberFormats.experience(current) + " / "
                + NumberFormats.experience(getMaximumExperience());
    }

    @Override
    public boolean refresh(Client client) {
        long next = 0;
        for (Skill skill : Skill.values()) {
            next += Math.max(0, client.getSkillExperience(skill));
        }

        if (next == current) {
            return false;
        }

        current = next;
        return true;
    }

    @Override
    public GoalComponent build(Client client, GrinderConfig config) {
        refresh(client);
        return Goal.super.build(client, config);
    }

    private long getMaximumExperience() {
        return target > 0 ? target : Skill.values().length * SkillExperienceGoal.LEVEL_99_EXPERIENCE;
    }
}
