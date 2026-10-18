
package com.grinder.content.goal;

import com.grinder.config.GrinderConfig;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;

import com.grinder.ui.GoalComponent;
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
        component.setIconVisible(config.showGoalIcons());
        component.setProgressBarVisible(config.showProgressBar());
        return component;
    }

    default void onItemContainerUpdated(Client client, ItemContainer container)
    {
    }
}
