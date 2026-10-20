
package com.grindstone.content.milestone.impl.collectionlog;

import com.grindstone.config.GrindstoneConfig;
import com.grindstone.content.collectionlog.CollectionLogEntry;
import com.grindstone.content.collectionlog.CollectionLogHistory;
import com.grindstone.content.collectionlog.CollectionLogPages;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.ui.Icon;
import com.grindstone.ui.components.MilestoneComponent;
import com.grindstone.ui.components.ItemComponent;
import com.grindstone.ui.components.ItemContainerComponent;
import com.grindstone.util.HtmlUtils;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
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
public class CollectionLogEntryMilestone implements Milestone {
    private static final int COLLECTION_LOG_CONTAINER_ID = InventoryID.COLLECTION_TRANSMIT;

    @Getter
    private final CollectionLogEntry entry;
    @Getter
    private final int pageStruct;
    @Getter
    private final String pageName;

    private transient ItemManager itemManager;
    private transient CollectionLogHistory history;

    public void setHistory(CollectionLogHistory history) {
        this.history = history;
    }

    private boolean isKnownObtained(ItemContainer container, ItemComposition item) {
        return history != null ? history.contains(item.getId()) : container != null && container.count(item.getId()) > 0;
    }
    private transient long obtained;
    private transient long total;
    @Getter
    private transient String progressText = "0 / 0 obtained (0%)";

    public CollectionLogEntryMilestone(ItemManager itemManager, CollectionLogEntry entry) {
        this.itemManager = itemManager;
        this.entry = entry;
        this.pageStruct = Objects.requireNonNull(entry).getStruct();
        this.pageName = entry.getTitle(null);
    }
    public CollectionLogEntryMilestone(ItemManager itemManager, int pageStruct, String pageName) {
        if (pageStruct <= 0 || pageName == null || pageName.trim().isEmpty() || pageName.length() > 200) {
            throw new IllegalArgumentException("Invalid collection-log page");
        }
        this.itemManager = itemManager;
        this.pageStruct = pageStruct;
        this.pageName = pageName;
        this.entry = Arrays.stream(CollectionLogEntry.values()).filter(value -> value.getStruct() == pageStruct).findFirst().orElse(null);
    }

    public void setItemManager(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    @Override
    public Supplier<MilestoneComponent> prepare(Client client, GrindstoneConfig config) {
        ItemContainer container = client.getItemContainer(COLLECTION_LOG_CONTAINER_ID);
        ItemComposition[] items = CollectionLogPages.getItems(client, pageStruct);
        List<Supplier<ItemComponent>> preparedItems = new ArrayList<>(items.length);
        long nextObtained = 0;
        long nextTotal = 0;
        String title = getTitle(client);
        BufferedImage icon = config != null && !config.showActivityIcons() ? Icon.COLLECTION_LOG.getImage() : getIcon(client);
        int columns = config == null ? 4 : config.itemsPerRow();
        int rows = config == null ? 5 : config.maxItemRows();
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
            int id = item.getId();
            String name = item.getName();
            BufferedImage image = itemManager == null ? null : itemManager.getImage(id);
            String observed = history == null ? "Date not recorded" : history.getFirstSeenText(id);
            String tooltip = config == null || config.showItemTooltips() ? "<html>" + HtmlUtils.escape(name) + "<br>" + (isObtained ? "Obtained" : "Not obtained") + "<br>" + HtmlUtils.escape(observed) + "</html>" : null;
            preparedItems.add(() -> new ItemComponent(id, name, image, tooltip, isObtained));
        }
        obtained = nextObtained;
        total = nextTotal;
        long percentage = total == 0 ? 0 : obtained * 100 / total;
        progressText = obtained + " / " + total + " obtained (" + percentage + "%)";
        Supplier<MilestoneComponent> prepared = Milestone.super.prepare(client, config);
        long displayedObtained = obtained;
        long displayedTotal = total;
        String emptyMessage = total == 0 ? "Open the collection log to load items" : obtained >= total ? "All items obtained" : "No items to display";
        return () -> {
            MilestoneComponent component = prepared.get();
            component.setTitle(title);
            component.setIcon(icon);
            ItemContainerComponent grid = new ItemContainerComponent();
            grid.configure(columns, rows);
            grid.setCollectionSummary(displayedObtained, displayedTotal);
            grid.setEmptyMessage(emptyMessage);
            List<ItemComponent> components = new ArrayList<>(preparedItems.size());
            for (Supplier<ItemComponent> item : preparedItems) {
                components.add(item.get());
            }
            grid.set(components);
            component.getContent().add(grid);
            return component;
        };
    }

    @Override
    public String getTitle(Client client) {
        return CollectionLogPages.getTitle(client, pageStruct, pageName);
    }

    @Override
    public BufferedImage getIcon(Client client) {
        return Icon.COLLECTION_LOG.getImage();
    }

    @Override
    public MilestoneCategory getCategory() {
        return MilestoneCategory.COLLECTION_LOG;
    }

    @Override
    public MilestoneStatus getStatus() {
        if (total <= 0 || obtained <= 0) {
            return MilestoneStatus.NOT_STARTED;
        }

        return obtained >= total ? MilestoneStatus.COMPLETE : MilestoneStatus.IN_PROGRESS;
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
    public boolean refresh(Client client) {
        long previousObtained = obtained;
        long previousTotal = total;
        onItemContainerUpdated(client, client.getItemContainer(COLLECTION_LOG_CONTAINER_ID));
        return obtained != previousObtained || total != previousTotal;
    }

    @Override
    public void onItemContainerUpdated(Client client, ItemContainer container) {
        if (container == null && history == null || container != null && container.getId() != COLLECTION_LOG_CONTAINER_ID) {
            return;
        }

        ItemComposition[] items = CollectionLogPages.getItems(client, pageStruct);
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
