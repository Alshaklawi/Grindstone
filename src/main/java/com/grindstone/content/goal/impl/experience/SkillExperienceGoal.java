package com.grindstone.content.goal.impl.experience;

import com.grindstone.config.GrindstoneConfig;
import com.grindstone.content.goal.Goal;
import com.grindstone.content.goal.GoalCategory;
import com.grindstone.content.goal.GoalStatus;
import com.grindstone.ui.Icon;
import com.grindstone.ui.components.GoalComponent;
import com.grindstone.util.NumberFormats;
import java.awt.image.BufferedImage;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Skill;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class SkillExperienceGoal implements Goal {
    public static final long LEVEL_99_EXPERIENCE = 13_034_431L;
    private long target;

    @Override
    public boolean supportsTarget() {
        return true;
    }

    @Override
    public void setTarget(long target) {
        if (target < 1 || target > 200_000_000L) {
            throw new IllegalArgumentException("Choose XP from 1 to 200,000,000");
        }
        this.target = target;
    }

    @Getter
    private final Skill skill;

    private transient long current;

    public SkillExperienceGoal(Skill skill) {
        if (skill == null) {
            throw new IllegalArgumentException("Skill is required");
        }

        this.skill = skill;
    }

    @Override
    public String getTitle(Client client) {
        return skill.getName() + " experience";
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
        return current >= getProgressMaximum() ? GoalStatus.COMPLETE
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
        return target > 0 ? target : LEVEL_99_EXPERIENCE;
    }

    @Override
    public String getProgressText() {
        return NumberFormats.experience(current) + " / " + NumberFormats.experience(getProgressMaximum());
    }

    @Override
    public boolean refresh(Client client) {
        long next = Math.max(0, client.getSkillExperience(skill));
        if (next == current) {
            return false;
        }

        current = next;
        return true;
    }

    @Override
    public GoalComponent build(Client client, GrindstoneConfig config) {
        refresh(client);
        return Goal.super.build(client, config);
    }
}
