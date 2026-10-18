
package com.grinder.content.goal;

import com.grinder.config.GrinderConfig;
import com.grinder.ui.GoalComponent;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;

public interface Goal
{
    default String getTitle(Client client)
    {
        return getClass().getSimpleName();
    }

    default BufferedImage getIcon(Client client)
    {
        return com.grinder.ui.Icon.MAIN.getImage();
    }

    default LocalDateTime getStart()
    {
        return null;
    }

    default LocalDateTime getEnd()
    {
        return null;
    }

    default GoalStatus getStatus()
    {
        return GoalStatus.NOT_STARTED;
    }

    default GoalCategory getCategory()
    {
        return GoalCategory.fromName(getClass().getSimpleName());
    }

    default boolean hasProgress()
    {
        return false;
    }

    default long getProgressCurrent()
    {
        return 0;
    }

    default long getProgressMaximum()
    {
        return 0;
    }

    default String getProgressText()
    {
        return hasProgress() ? getProgressCurrent() + " / " + getProgressMaximum() : "";
    }

    default GoalComponent build(Client client)
    {
        GoalComponent component = new GoalComponent();
        component.setTitle(getTitle(client));
        component.setIcon(getIcon(client));
        component.setStartDate(getStart());
        component.setEndDate(getEnd());

        if (hasProgress())
        {
            component.setProgress(getProgressCurrent(), getProgressMaximum(), getProgressText());
        }
        else
        {
            component.setProgressStatus(getStatus().name());
        }

        return component;
    }

    default GoalComponent build(Client client, GrinderConfig config)
    {
        GoalComponent component = build(client);
        component.setIconVisible(config == null || config.showGoalIcons());
        component.setProgressBarVisible(config == null || config.showProgressBar());
        return component;
    }

    /**
     * Called when an item container changes. Implementations should update their
     * internal state only; Swing updates must be scheduled on the EDT.
     */
    default void onItemContainerUpdated(Client client, ItemContainer container)
    {
    }

    /**
     * Returns whether this goal may be affected by a change to the given container.
     * Goals that depend on other containers should override this method.
     */
    default boolean isAffectedBy(ItemContainer container)
    {
        return container != null;
    }
}
