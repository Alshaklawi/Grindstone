package com.grinder.content.goal.impl.experience;

import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.GoalStatus;
import com.grinder.util.NumberFormats;
import java.util.Objects;
import net.runelite.api.Client;
import net.runelite.api.Skill;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class SessionExperienceGoal implements Goal {
    private Skill skill;
    private boolean total;
    private long target = 100_000;
    private transient boolean initialized;
    private transient long lastValue;
    private transient long progress;

    public SessionExperienceGoal() {
        total = true;
    }

    public SessionExperienceGoal(Skill skill) {
        this.skill = Objects.requireNonNull(skill);
    }

    public Skill getSkill() {
        return skill;
    }

    public boolean isTotal() {
        return total;
    }

    public void validate() {
        if (!total && skill == null) {
            throw new IllegalArgumentException("Choose a skill for the session goal.");
        }
        setTarget(target);
    }

    public void restart() {
        initialized = false;
        lastValue = 0;
        progress = 0;
    }

    public void copyReadingsFrom(SessionExperienceGoal source) {
        if (total != source.total || skill != source.skill) {
            throw new IllegalArgumentException("Session sources do not match.");
        }
        initialized = source.initialized;
        lastValue = source.lastValue;
        progress = source.progress;
    }

    @Override
    public com.grinder.ui.components.GoalComponent build(Client client, com.grinder.config.GrinderConfig config) {
        refresh(client);
        return Goal.super.build(client, config);
    }

    public boolean observeExperience(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Experience cannot be negative.");
        }
        if (!initialized) {
            lastValue = value;
            initialized = true;
            return true;
        }
        long previous = progress;
        if (value >= lastValue) {
            long gained = value - lastValue;
            progress += Math.min(gained, Long.MAX_VALUE - progress);
        }
        lastValue = value;
        return progress != previous;
    }

    @Override
    public boolean refresh(Client client) {
        if (!total) {
            return observeExperience(Math.max(0, client.getSkillExperience(skill)));
        }
        long experience = 0;
        for (Skill next : Skill.values()) {
            experience += Math.max(0, client.getSkillExperience(next));
        }
        return observeExperience(experience);
    }

    @Override
    public String getTitle(Client client) {
        return (total ? "Total" : skill.getName()) + " session XP";
    }

    @Override
    public String getDescription() {
        return "Only XP gains observed since this goal's first reading in the current login are counted. Existing XP is the baseline, not progress. Relogging, reconnecting or restarting this goal resets session progress. Session readings are not saved or imported.";
    }

    @Override
    public GoalCategory getCategory() {
        return GoalCategory.SKILLING;
    }

    @Override
    public java.awt.image.BufferedImage getIcon(Client client) {
        return com.grinder.ui.Icon.SKILLING.getImage();
    }

    @Override
    public boolean supportsTarget() {
        return true;
    }

    @Override
    public void setTarget(long target) {
        long cap = total ? Skill.values().length * 200_000_000L : 200_000_000L;
        if (target < 1 || target > cap) {
            throw new IllegalArgumentException("Session target must be between 1 and " + cap);
        }
        this.target = target;
    }

    @Override
    public boolean hasProgress() {
        return true;
    }

    @Override
    public long getProgressCurrent() {
        return progress;
    }

    @Override
    public long getProgressMaximum() {
        return target;
    }

    @Override
    public String getProgressText() {
        return NumberFormats.experience(progress) + " / " + NumberFormats.experience(target) + " this login";
    }

    @Override
    public GoalStatus getStatus() {
        return progress >= target ? GoalStatus.COMPLETE : progress > 0 ? GoalStatus.IN_PROGRESS : GoalStatus.NOT_STARTED;
    }
}
