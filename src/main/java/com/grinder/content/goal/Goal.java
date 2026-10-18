
package com.grinder.content.goal;

import com.grinder.content.utils.Icon;
import com.grinder.ui.GoalComponent;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;

import javax.swing.JLabel;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public interface Goal
{
    default String getTitle(Client client)
    {
        return "Unknown goal name.";
    }

    default BufferedImage getIcon(Client client)
    {
        return Icon.MAIN.getImage();
    }

    default LocalDateTime getStart()
    {
        return LocalDateTime.now();
    }

    default LocalDateTime getEnd()
    {
        return LocalDateTime.now();
    }

    default GoalStatus getStatus()
    {
        return GoalStatus.NOT_STARTED;
    }

    /**
     * Whether this goal has measurable progress and should display a progress bar.
     */
    default boolean hasProgress(Client client)
    {
        return false;
    }

    /**
     * The current amount of progress for this goal.
     */
    default long getProgressCurrent(Client client)
    {
        return 0L;
    }

    /**
     * The target amount of progress for this goal.
     */
    default long getProgressMaximum(Client client)
    {
        return 0L;
    }

    /**
     * Optional text displayed in the goal's always-visible progress summary.
     */
    default String getProgressText(Client client)
    {
        GoalStatus status = getStatus();
        return status == null ? "" : status.toString().replace('_', ' ');
    }

    default GoalComponent build(Client client)
    {
        GoalComponent component = new GoalComponent();
        component.setTitle(getTitle(client));
        component.setIcon(getIcon(client));
        component.setStartDate(getStart());
        component.setEndDate(getEnd());

        if (hasProgress(client))
        {
            component.setProgress(getProgressCurrent(client), getProgressMaximum(client), getProgressText(client));
        }
        else
        {
            component.setProgressStatus(getProgressText(client));
        }

        component.getContent().add(new JLabel(getTitle(client) + " | " + getClass().getSimpleName()));
        return component;
    }

    default void onItemContainerUpdated(Client client, ItemContainer container)
    {
    }
}
