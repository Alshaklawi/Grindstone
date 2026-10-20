package com.grindstone.content.milestone.impl.level;

import com.grindstone.config.GrindstoneConfig;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.ui.Icon;
import com.grindstone.ui.components.MilestoneComponent;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.Skill;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class TotalLevelMilestone implements Milestone {
    private static final int MAX_LEVEL_PER_SKILL = 99;
    private transient int current;
    private int target;

    @Override
    public boolean supportsTarget() {
        return true;
    }

    @Override
    public void setTarget(long target) {
        if (target < 1 || target > Skill.values().length * MAX_LEVEL_PER_SKILL) {
            throw new IllegalArgumentException("Target exceeds the total level cap");
        }
        this.target = (int) target;
    }

    @Override
    public String getTitle(Client client) {
        return "Total level";
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
        return current >= getMaximumTotalLevel() ? MilestoneStatus.COMPLETE
                : current > Skill.values().length ? MilestoneStatus.IN_PROGRESS : MilestoneStatus.NOT_STARTED;
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
        return getMaximumTotalLevel();
    }

    @Override
    public String getProgressText() {
        return current + " / " + getMaximumTotalLevel();
    }

    @Override
    public boolean refresh(Client client) {
        int next = Math.max(0, client.getTotalLevel());
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

    private int getMaximumTotalLevel() {
        return target > 0 ? target : Skill.values().length * MAX_LEVEL_PER_SKILL;
    }
}
