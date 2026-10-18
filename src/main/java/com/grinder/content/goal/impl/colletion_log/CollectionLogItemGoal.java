
package com.grinder.content.goal.impl.colletion_log;

import com.grinder.config.GrinderConfig;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.GoalStatus;
import com.grinder.ui.GoalComponent;
import com.grinder.ui.Icon;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;

public class CollectionLogItemGoal implements Goal
{
    private static final int COLLECTION_LOG_CONTAINER_ID = InventoryID.COLLECTION_TRANSMIT;

    private final int item;
    private final int amount;
    private transient boolean obtained;

    public CollectionLogItemGoal(int item, int amount)
    {
        if (item <= 0)
        {
            throw new IllegalArgumentException("Item ID must be positive");
        }

        if (amount <= 0)
        {
            throw new IllegalArgumentException("Amount must be positive");
        }

        this.item = item;
        this.amount = amount;
    }

    public int getItem()
    {
        return item;
    }

    public int getAmount()
    {
        return amount;
    }

    public boolean isObtained(Client client)
    {
        ItemContainer container = client.getItemContainer(COLLECTION_LOG_CONTAINER_ID);
        return isObtained(container);
    }

    private boolean isObtained(ItemContainer container)
    {
        return container != null && container.contains(item);
    }

    @Override
    public String getTitle(Client client)
    {
        if (client == null)
        {
            return "Item " + item;
        }

        net.runelite.api.ItemComposition definition = client.getItemDefinition(item);
        return definition == null ? "Item " + item : definition.getName();
    }

    @Override
    public BufferedImage getIcon(Client client)
    {
        return Icon.COLLECTION_LOG.getImage();
    }

    @Override
    public GoalCategory getCategory()
    {
        return GoalCategory.COLLECTION_LOG;
    }

    @Override
    public GoalStatus getStatus()
    {
        return obtained ? GoalStatus.COMPLETE : GoalStatus.IN_PROGRESS;
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
    public boolean isAffectedBy(ItemContainer container)
    {
        return container != null && container.getId() == COLLECTION_LOG_CONTAINER_ID;
    }

    @Override
    public void onItemContainerUpdated(Client client, ItemContainer container)
    {
        if (isAffectedBy(container))
        {
            obtained = isObtained(container);
        }
    }
}
