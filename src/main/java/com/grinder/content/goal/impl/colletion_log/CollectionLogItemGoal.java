
package com.grinder.content.goal.impl.colletion_log;

import com.grinder.config.GrinderConfig;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalStatus;
import com.grinder.ui.GoalComponent;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemComposition;

public class CollectionLogItemGoal implements Goal
{
    private static final int COLLECTION_LOG_CONTAINER_ID = 620;

    private final int item;
    private final int amount;
    private transient boolean obtained;

    public CollectionLogItemGoal(int item, int amount)
    {
        if (item <= 0 || amount <= 0)
        {
            throw new IllegalArgumentException("Item ID and amount must be positive.");
        }

        this.item = item;
        this.amount = amount;
    }

    public CollectionLogItemGoal(int item)
    {
        this(item, 1);
    }

    public int getItem()
    {
        return item;
    }

    public int getAmount()
    {
        return amount;
    }

    private boolean isObtained(Client client)
    {
        ItemContainer container = client.getItemContainer(COLLECTION_LOG_CONTAINER_ID);

        if (container == null)
        {
            return false;
        }

        for (Item loggedItem : container.getItems())
        {
            if (loggedItem != null && loggedItem.getId() == item)
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getTitle(Client client)
    {
        ItemComposition composition = client.getItemDefinition(item);
        return composition == null ? "Item " + item : composition.getName();
    }

    @Override
    public BufferedImage getIcon(Client client)
    {
        return null;
    }

    @Override
    public GoalComponent build(Client client)
    {
        obtained = isObtained(client);
        return Goal.super.build(client);
    }

    @Override
    public GoalComponent build(Client client, GrinderConfig config)
    {
        obtained = isObtained(client);
        return Goal.super.build(client, config);
    }

    @Override
    public boolean hasProgress()
    {
        return true;
    }

    @Override
    public long getProgressCurrent()
    {
        return obtained ? 1 : 0;
    }

    @Override
    public long getProgressMaximum()
    {
        return 1;
    }

    @Override
    public String getProgressText()
    {
        return obtained ? "Obtained" : "Not obtained";
    }

    @Override
    public GoalStatus getStatus()
    {
        return obtained ? GoalStatus.COMPLETE : GoalStatus.IN_PROGRESS;
    }

    @Override
    public void onItemContainerUpdated(Client client, ItemContainer container)
    {
        if (container != null && container.getId() == COLLECTION_LOG_CONTAINER_ID)
        {
            obtained = isObtained(client);
        }
    }
}
