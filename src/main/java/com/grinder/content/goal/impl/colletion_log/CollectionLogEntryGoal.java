
package com.grinder.content.goal.impl.colletion_log;

import com.grinder.config.GrinderConfig;
import com.grinder.content.CollectionLogEntry;
import com.grinder.content.goal.Goal;
import com.grinder.ui.GoalComponent;
import com.grinder.ui.ItemComponent;
import com.grinder.ui.ItemContainerComponent;
import com.grinder.ui.Icon;
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
import net.runelite.client.game.ItemManager;

public class CollectionLogEntryGoal implements Goal
{
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

        refresh(client, activeConfig);
        current.setIconVisible(activeConfig == null || activeConfig.showGoalIcons());
        current.setProgressBarVisible(activeConfig == null || activeConfig.showProgressBar());
        return current;
    }

    private void refresh(Client client, GrinderConfig config)
    {
        current.setTitle(getTitle(client));
        BufferedImage icon = getIcon(client);
        current.setIcon(icon);
        current.setStartDate(getStart());
        current.setEndDate(getEnd());

        List<ItemComponent> components = new ArrayList<>();
        ItemComposition[] items = entry.getItems(client);
        obtained = 0;
        total = items.length;

        main.configure(config == null ? 4 : config.itemsPerRow(), config == null ? 5 : config.maxItemRows());

        for (ItemComposition item : items)
        {
            if (item == null)
            {
                continue;
            }

            boolean isObtained = entry.isObtained(client, item);

            if (isObtained)
            {
                obtained++;
            }

            if (config != null && !config.showObtainedItems() && isObtained)
            {
                continue;
            }

            ItemComponent component = itemComponents.computeIfAbsent(item.getId(), id -> new ItemComponent(itemManager, item));
            component.setItem(item);
            component.setTooltip(config == null || config.showItemTooltips() ? item.getName() : null);
            component.setBorderColor(isObtained ? new Color(70, 180, 100) : null);
            components.add(component);
        }

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
    public void onItemContainerUpdated(Client client, ItemContainer container)
    {
        if (container != null && container.getId() == 620 && current != null)
        {
            refresh(client, activeConfig);
            current.setIconVisible(activeConfig == null || activeConfig.showGoalIcons());
            current.setProgressBarVisible(activeConfig == null || activeConfig.showProgressBar());
        }
    }
}
