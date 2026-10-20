package com.grindstone.content.milestone.impl.level;

import com.grindstone.config.GrindstoneConfig;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.ui.Icon;
import com.grindstone.ui.components.MilestoneComponent;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Skill;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class SkillLevelMilestone implements Milestone {
    private static final int MAX_LEVEL = 99;
    private int target;

    @Override
    public boolean supportsTarget() {
        return true;
    }

    @Override
    public void setTarget(long target) {
        if (target < 1 || target > MAX_LEVEL) {
            throw new IllegalArgumentException("Choose a level from 1 to 99");
        }
        this.target = (int) target;
    }

    @Getter
    private final Skill skill;

    private transient int current;

    public SkillLevelMilestone(Skill skill) {
        if (skill == null) {
            throw new IllegalArgumentException("Skill is required");
        }

        this.skill = skill;
    }

    @Override
    public String getTitle(Client client) {
        return skill.getName() + " level";
    }

    @Override
    public BufferedImage getIcon(Client client) {
        return Icon.SKILLING.getImage();
    }

    @Override
    public MilestoneCategory getCategory() {
        return MilestoneCategory.SKILLING;
    }

    @Override
    public MilestoneStatus getStatus() {
        return current >= getProgressMaximum() ? MilestoneStatus.COMPLETE
                : current > 1 ? MilestoneStatus.IN_PROGRESS : MilestoneStatus.NOT_STARTED;
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
        return target > 0 ? target : MAX_LEVEL;
    }

    @Override
    public String getProgressText() {
        return current + " / " + getProgressMaximum();
    }

    @Override
    public boolean refresh(Client client) {
        int next = Math.max(1, client.getRealSkillLevel(skill));
        if (next == current) {
            return false;
        }

        current = next;
        return true;
    }

    @Override
    public Supplier<MilestoneComponent> prepare(Client client, GrindstoneConfig config) {
        refresh(client);
        return Milestone.super.prepare(client, config);
    }
}
