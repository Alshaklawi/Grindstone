
package com.grinder.content.goal.impl.collectionlog;

import com.grinder.config.GrinderConfig;
import com.grinder.content.collectionlog.CollectionLogEntry;
import com.grinder.content.collectionlog.CollectionLogHistory;
import com.grinder.content.collectionlog.CollectionLogPages;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.GoalStatus;
import com.grinder.ui.Icon;
import com.grinder.ui.components.GoalComponent;
import com.grinder.ui.components.ItemComponent;
import com.grinder.ui.components.ItemContainerComponent;
import com.grinder.util.HtmlUtils;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.game.ItemManager;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class CollectionLogEntryGoal implements Goal {
    private static final int COLLECTION_LOG_CONTAINER_ID = InventoryID.COLLECTION_TRANSMIT;

    @Getter
    private final CollectionLogEntry entry;
    @Getter
    private final int pageStruct;
    @Getter
    private final String pageName;

    private transient ItemManager itemManager;
    private transient int iconItem;
    private transient CollectionLogHistory history;

    public void setHistory(CollectionLogHistory history) {
        this.history = history;
    }

    private boolean isKnownObtained(ItemContainer container, ItemComposition item) {
        return history != null && history.contains(item.getId()) || container != null && container.contains(item.getId());
    }
    private transient Map<Integer, ItemComponent> itemComponents = new HashMap<>();
    private transient GoalComponent current;
    private transient ItemContainerComponent main;
    private transient long obtained;
    private transient long total;
    @Getter
    private transient String progressText = "0 / 0 obtained (0%)";

    public CollectionLogEntryGoal(ItemManager itemManager, CollectionLogEntry entry) {
        this.itemManager = itemManager;
        this.entry = entry;
        this.pageStruct = Objects.requireNonNull(entry).getStruct();
        this.pageName = entry.getTitle(null);
        initializeTransientState();
    }
    public CollectionLogEntryGoal(ItemManager itemManager, int pageStruct, String pageName) {
        if (pageStruct <= 0 || pageName == null || pageName.trim().isEmpty() || pageName.length() > 200) {
            throw new IllegalArgumentException("Invalid collection-log page");
        }
        this.itemManager = itemManager;
        this.pageStruct = pageStruct;
        this.pageName = pageName;
        this.entry = Arrays.stream(CollectionLogEntry.values()).filter(value -> value.getStruct() == pageStruct).findFirst().orElse(null);
        initializeTransientState();
    }

    private void initializeTransientState() {
        if (itemComponents == null) {
            itemComponents = new HashMap<>();
        }
    }

    public void setItemManager(ItemManager itemManager) {
        this.itemManager = itemManager;
        initializeTransientState();
        itemComponents.clear();
    }

    @Override
    public GoalComponent build(Client client) {
        return build(client, null);
    }

    @Override
    public GoalComponent build(Client client, GrinderConfig config) {
        initializeTransientState();

        if (current == null) {
            current = new GoalComponent();
            main = new ItemContainerComponent();
            current.getContent().add(main);
        }

        refresh(client, config);

        current.setIconVisible(config == null || config.showGoalIcons());
        current.setProgressBarVisible(config == null || config.showProgressBar());
        return current;
    }

    private void refresh(Client client, GrinderConfig config) {
        ItemContainer container = client.getItemContainer(COLLECTION_LOG_CONTAINER_ID);
        ItemComposition[] items = CollectionLogPages.items(client, pageStruct);
        List<ItemComponent> components = new ArrayList<>(items.length);

        long nextObtained = 0;
        long nextTotal = 0;

        current.setTitle(getTitle(client));
        current.setIcon(config != null && !config.showActivityIcons() ? Icon.COLLECTION_LOG.getImage() : getIcon(client));

        main.configure(config == null ? 4 : config.itemsPerRow(), config == null ? 5 : config.maxItemRows());

        for (ItemComposition item : items) {
            if (item == null) {
                continue;
            }

            nextTotal++;
            boolean isObtained = isKnownObtained(container, item);

            if (isObtained) {
                nextObtained++;
            }

            if (config != null && !config.showObtainedItems() && isObtained) {
                continue;
            }

            ItemComponent component = itemComponents.get(item.getId());

            if (component == null) {
                component = new ItemComponent(itemManager, item);
                itemComponents.put(item.getId(), component);
            } else {
                component.setItem(item);
            }

            String observed = history == null ? "Date not recorded" : history.firstSeenText(item.getId());
            String tooltip = "<html>" + HtmlUtils.escape(item.getName()) + "<br>"
                    + (isObtained ? "Obtained" : "Not obtained") + "<br>" + HtmlUtils.escape(observed) + "</html>";
            component.setTooltip(config == null || config.showItemTooltips() ? tooltip : null);
            component.setObtained(isObtained);
            components.add(component);
        }

        obtained = nextObtained;
        total = nextTotal;
        main.setEmptyMessage(total == 0 ? "Open the collection log to load items" : obtained >= total ? "All items obtained" : "No items to display");
        main.set(components);

        long percentage = total == 0 ? 0 : obtained * 100 / total;
        progressText = obtained + " / " + total + " obtained (" + percentage + "%)";
        current.setProgress(obtained, total, progressText);
    }

    @Override
    public String getTitle(Client client) {
        return CollectionLogPages.title(client, pageStruct, pageName);
    }

    @Override
    public BufferedImage getIcon(Client client) {
        if (itemManager != null && client != null) {
            if (iconItem <= 0) {
                int[] items = CollectionLogPages.itemIds(client, pageStruct);
                if (items.length > 0) {
                    iconItem = items[0];
                }
            }
            if (iconItem > 0) {
                return itemManager.getImage(iconItem);
            }
        }
        return Icon.COLLECTION_LOG.getImage();
    }

    @Override
    public GoalCategory getCategory() {
        return GoalCategory.COLLECTION_LOG;
    }

    @Override
    public GoalStatus getStatus() {
        if (total <= 0 || obtained <= 0) {
            return GoalStatus.NOT_STARTED;
        }

        return obtained >= total ? GoalStatus.COMPLETE : GoalStatus.IN_PROGRESS;
    }

    @Override
    public boolean hasProgress() {
        return total > 0;
    }

    @Override
    public long getProgressCurrent() {
        return obtained;
    }

    @Override
    public long getProgressMaximum() {
        return total;
    }


    @Override
    public boolean isAffectedBy(ItemContainer container) {
        return container != null && container.getId() == COLLECTION_LOG_CONTAINER_ID;
    }

    @Override
    public void onItemContainerUpdated(Client client, ItemContainer container) {
        if (container == null || container.getId() != COLLECTION_LOG_CONTAINER_ID) {
            return;
        }

        ItemComposition[] items = CollectionLogPages.items(client, pageStruct);
        long nextObtained = 0;
        long nextTotal = 0;

        for (ItemComposition item : items) {
            if (item == null) {
                continue;
            }

            nextTotal++;

            if (isKnownObtained(container, item)) {
                nextObtained++;
            }
        }

        obtained = nextObtained;
        total = nextTotal;

        long percentage = total == 0 ? 0 : obtained * 100 / total;
        progressText = obtained + " / " + total + " obtained (" + percentage + "%)";
    }
}
