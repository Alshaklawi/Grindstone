package com.grinder.content.goal.impl.level;

import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.GoalStatus;
import com.grinder.ui.Icon;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.Skill;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class TotalLevelGoal implements Goal
{
    private static final int MAX_LEVEL_PER_SKILL = 99;
    private transient int current;
    private int target;

    @Override
    public boolean supportsTarget() { return true; }

    @Override
    public void setTarget(long target)
    {
        if (target < 1 || target > Skill.values().length * MAX_LEVEL_PER_SKILL) { throw new IllegalArgumentException("Target exceeds the total level cap"); }
        this.target = (int) target;
    }

    @Override
    public String getTitle(Client client)
    {
        return "Total level";
    }

    @Override
    public BufferedImage getIcon(Client client)
    {
        return Icon.SKILLING.getImage();
    }

    @Override
    public GoalCategory getCategory()
    {
        return GoalCategory.SKILLING;
    }

    @Override
    public GoalStatus getStatus()
    {
        return current >= getMaximumTotalLevel() ? GoalStatus.COMPLETE
                : current > Skill.values().length ? GoalStatus.IN_PROGRESS : GoalStatus.NOT_STARTED;
    }

    @Override
    public boolean hasProgress()
    {
        return true;
    }

    @Override
    public long getProgressCurrent()
    {
        return current;
    }

    @Override
    public long getProgressMaximum()
    {
        return getMaximumTotalLevel();
    }

    @Override
    public String getProgressText()
    {
        return current + " / " + getMaximumTotalLevel();
    }

    @Override
    public boolean refresh(Client client)
    {
        int next = Math.max(0, client.getTotalLevel());
        if (next == current)
        {
            return false;
        }

        current = next;
        return true;
    }

    @Override
    public com.grinder.ui.GoalComponent build(Client client, com.grinder.config.GrinderConfig config)
    {
        refresh(client);
        return Goal.super.build(client, config);
    }

    private int getMaximumTotalLevel()
    {
        return target > 0 ? target : Skill.values().length * MAX_LEVEL_PER_SKILL;
    }
}
