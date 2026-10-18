
package com.grinder.content.goal.impl.colletion_log;

import com.grinder.config.GrinderConfig;
import com.grinder.content.CollectionLogEntry;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.GoalStatus;
import com.grinder.ui.GoalComponent;
import com.grinder.ui.Icon;
import com.grinder.ui.ItemComponent;
import com.grinder.ui.ItemContainerComponent;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.game.ItemManager;

public class CollectionLogEntryGoal implements Goal
{
    private static final int COLLECTION_LOG_CONTAINER_ID = InventoryID.COLLECTION_TRANSMIT;
    private static final Color OBTAINED_BORDER_COLOR = new Color(70, 180, 100);

    @Getter
    private final CollectionLogEntry entry;

    private transient ItemManager itemManager;
    private transient Map<Integer, ItemComponent> itemComponents = new HashMap<>();
    private transient GoalComponent current;
    private transient ItemContainerComponent main;
    private transient GrinderConfig activeConfig;
    private transient long obtained;
    private transient long total;
    private transient String progressText = "0 / 0 obtained (0%)";

    public CollectionLogEntryGoal(ItemManager itemManager, CollectionLogEntry entry)
    {
        this.itemManager = itemManager;
        this.entry = entry;
        initializeTransientState();
    }

    private void initializeTransientState()
    {
        if (itemComponents == null)
        {
            itemComponents = new HashMap<>();
        }
    }

    public void setItemManager(ItemManager itemManager)
    {
        this.itemManager = itemManager;
        initializeTransientState();
        itemComponents.clear();
    }

    @Override
    public GoalComponent build(Client client)
    {
        return build(client, null);
    }

    @Override
    public GoalComponent build(Client client, GrinderConfig config)
    {
        initializeTransientState();
        activeConfig = config;

        if (current == null)
        {
            current = new GoalComponent();
            main = new ItemContainerComponent();
            current.getContent().add(main);
        }

        refresh(client, config);

        current.setIconVisible(config == null || config.showGoalIcons());
        current.setProgressBarVisible(config == null || config.showProgressBar());
        return current;
    }

    private void refresh(Client client, GrinderConfig config)
    {
        ItemContainer container = client.getItemContainer(COLLECTION_LOG_CONTAINER_ID);
        ItemComposition[] items = entry.getItems(client);
        List<ItemComponent> components = new ArrayList<>(items.length);

        long nextObtained = 0;
        long nextTotal = 0;

        current.setTitle(getTitle(client));
        current.setIcon(getIcon(client));
        current.setStartDate(getStart());
        current.setEndDate(getEnd());

        main.configure(config == null ? 4 : config.itemsPerRow(), config == null ? 5 : config.maxItemRows());

        for (ItemComposition item : items)
        {
            if (item == null)
            {
                continue;
            }

            nextTotal++;
            boolean isObtained = entry.isObtained(container, item);

            if (isObtained)
            {
                nextObtained++;
            }

            if (config != null && !config.showObtainedItems() && isObtained)
            {
                continue;
            }

            ItemComponent component = itemComponents.get(item.getId());

            if (component == null)
            {
                component = new ItemComponent(itemManager, item);
                itemComponents.put(item.getId(), component);
            }
            else
            {
                component.setItem(item);
            }

            component.setTooltip(config == null || config.showItemTooltips() ? item.getName() : null);
            component.setBorderColor(isObtained ? OBTAINED_BORDER_COLOR : null);
            components.add(component);
        }

        obtained = nextObtained;
        total = nextTotal;
        main.set(components);

        long percentage = total == 0 ? 0 : obtained * 100 / total;
        progressText = obtained + " / " + total + " obtained (" + percentage + "%)";
        current.setProgress(obtained, total, progressText);
    }

    @Override
    public String getTitle(Client client)
    {
        return entry.getTitle(client);
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
        if (total <= 0 || obtained <= 0)
        {
            return GoalStatus.NOT_STARTED;
        }

        return obtained >= total ? GoalStatus.COMPLETE : GoalStatus.IN_PROGRESS;
    }

    @Override
    public boolean hasProgress()
    {
        return total > 0;
    }

    @Override
    public long getProgressCurrent()
    {
        return obtained;
    }

    @Override
    public long getProgressMaximum()
    {
        return total;
    }

    @Override
    public String getProgressText()
    {
        return progressText;
    }

    @Override
    public boolean isAffectedBy(ItemContainer container)
    {
        return container != null && container.getId() == COLLECTION_LOG_CONTAINER_ID;
    }

    @Override
    public void onItemContainerUpdated(Client client, ItemContainer container)
    {
        if (container == null || container.getId() != COLLECTION_LOG_CONTAINER_ID)
        {
            return;
        }

        // The panel should schedule the visual refresh on the Swing EDT.
        // This event updates the cached goal values only.
        ItemComposition[] items = entry.getItems(client);
        long nextObtained = 0;
        long nextTotal = 0;

        for (ItemComposition item : items)
        {
            if (item == null)
            {
                continue;
            }

            nextTotal++;

            if (entry.isObtained(container, item))
            {
                nextObtained++;
            }
        }

        obtained = nextObtained;
        total = nextTotal;

        long percentage = total == 0 ? 0 : obtained * 100 / total;
        progressText = obtained + " / " + total + " obtained (" + percentage + "%)";
    }
}
