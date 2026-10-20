package com.grindstone.content.milestone;

import com.grindstone.config.GrindstoneConfig;
import com.grindstone.ui.Icon;
import com.grindstone.ui.components.MilestoneComponent;
import com.grindstone.util.NumberFormats;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import net.runelite.api.Client;
import net.runelite.api.Skill;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public abstract class BaseSkillMilestone implements Milestone {
    private long target;
    private transient long current;
    private transient int reached;
    private transient Skill lowest;

    protected abstract boolean tracksExperience();

    @Override
    public final boolean supportsTarget() {
        return true;
    }

    @Override
    public final void setTarget(long target) {
        long maximum = tracksExperience() ? 200_000_000L : 99;
        if (target < 1 || target > maximum) {
            throw new IllegalArgumentException("Base target must be between 1 and " + maximum);
        }
        this.target = target;
    }

    @Override
    public final long getProgressCurrent() {
        return current;
    }

    @Override
    public final long getProgressMaximum() {
        return target > 0 ? target : tracksExperience() ? 1_000_000 : 70;
    }

    @Override
    public final boolean hasProgress() {
        return true;
    }

    @Override
    public final MilestoneStatus getStatus() {
        return current >= getProgressMaximum() ? MilestoneStatus.COMPLETE : current > 0 ? MilestoneStatus.IN_PROGRESS : MilestoneStatus.NOT_STARTED;
    }

    @Override
    public final MilestoneCategory getCategory() {
        return MilestoneCategory.SKILLING;
    }

    @Override
    public final BufferedImage getIcon(Client client) {
        return Icon.SKILLING.getImage();
    }

    @Override
    public final String getDescription() {
        return "Every skill must reach the target. Progress is the lowest " + (tracksExperience() ? "skill XP" : "real skill level (boosts excluded)") + ". " + reached + " / " + Skill.values().length + " skills meet the target." + (lowest == null ? " Waiting for client readings." : " Lowest: " + lowest.getName() + ".");
    }

    @Override
    public final String getCardDescription() {
        return lowest == null ? "" : "Lowest: " + lowest.getName();
    }

    @Override
    public final String getProgressText() {
        return (tracksExperience() ? NumberFormats.experience(current) + " / " + NumberFormats.experience(getProgressMaximum()) + " base XP" : current + " / " + getProgressMaximum() + " base level") + " · " + reached + "/" + Skill.values().length + " skills";
    }

    @Override
    public final boolean refresh(Client client) {
        return updateReadings(tracksExperience() ? client::getSkillExperience : client::getRealSkillLevel);
    }

    @Override
    public final Supplier<MilestoneComponent> prepare(Client client, GrindstoneConfig config) {
        refresh(client);
        return Milestone.super.prepare(client, config);
    }

    public final boolean updateReadings(ToIntFunction<Skill> readings) {
        long minimum = Long.MAX_VALUE;
        int nextReached = 0;
        Skill nextLowest = null;
        for (Skill skill : Skill.values()) {
            int value = Math.max(0, readings.applyAsInt(skill));
            if (value < minimum) {
                minimum = value;
                nextLowest = skill;
            }
            if (value >= getProgressMaximum()) {
                nextReached++;
            }
        }
        boolean changed = current != minimum || reached != nextReached || lowest != nextLowest;
        current = minimum;
        reached = nextReached;
        lowest = nextLowest;
        return changed;
    }
}
