
package com.grindstone.content.goal.impl.collectionlog;

import com.grindstone.config.GrindstoneConfig;
import com.grindstone.content.collectionlog.CollectionLogHistory;
import com.grindstone.content.collectionlog.CollectionLogPages;
import com.grindstone.content.goal.Goal;
import com.grindstone.content.goal.GoalCategory;
import com.grindstone.content.goal.GoalStatus;
import com.grindstone.ui.Icon;
import com.grindstone.ui.components.GoalComponent;
import java.awt.image.BufferedImage;
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
public class CollectionLogItemGoal implements Goal {
    private static final int COLLECTION_LOG_CONTAINER_ID = InventoryID.COLLECTION_TRANSMIT;

    @Getter
    private final int item;
    @Getter
    private final int amount;
    private transient boolean obtained;
    private transient ItemManager itemManager;
    private transient CollectionLogHistory history;

    public void setItemManager(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    public void setHistory(CollectionLogHistory history) {
        this.history = history;
        obtained = history != null && history.contains(item);
    }

    public CollectionLogItemGoal(int item, int amount) {
        if (item <= 0) {
            throw new IllegalArgumentException("Item ID must be positive");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        this.item = item;
        this.amount = amount;
    }



    public boolean isObtained(Client client) {
        ItemContainer container = client.getItemContainer(COLLECTION_LOG_CONTAINER_ID);
        return isObtained(container);
    }

    private boolean isObtained(ItemContainer container) {
        return history != null && history.contains(item) || container != null && container.contains(CollectionLogPages.canonicalItemId(item));
    }

    @Override
    public String getTitle(Client client) {
        if (client == null) {
            return "Item " + item;
        }

        ItemComposition definition = client.getItemDefinition(item);
        return definition == null ? "Item " + item : definition.getName();
    }

    @Override
    public BufferedImage getIcon(Client client) {
        return itemManager == null ? Icon.COLLECTION_LOG.getImage() : itemManager.getImage(item);
    }

    @Override
    public String getDescription() {
        return history == null || !history.contains(item) ? "" : history.firstSeenText(item);
    }

    @Override
    public GoalCategory getCategory() {
        return GoalCategory.COLLECTION_LOG;
    }

    @Override
    public GoalStatus getStatus() {
        return hasObservedItem() ? GoalStatus.COMPLETE : GoalStatus.NOT_STARTED;
    }

    @Override
    public boolean hasProgress() {
        return true;
    }

    @Override
    public long getProgressCurrent() {
        return hasObservedItem() ? 1 : 0;
    }

    @Override
    public long getProgressMaximum() {
        return 1;
    }

    @Override
    public String getProgressText() {
        return hasObservedItem() ? "Obtained" : "Not obtained";
    }

    private boolean hasObservedItem() {
        return obtained || history != null && history.contains(item);
    }

    @Override
    public GoalComponent build(Client client) {
        obtained = isObtained(client);
        return Goal.super.build(client);
    }

    @Override
    public GoalComponent build(Client client, GrindstoneConfig config) {
        obtained = isObtained(client);
        return Goal.super.build(client, config);
    }

    @Override
    public boolean isAffectedBy(ItemContainer container) {
        return container != null && container.getId() == COLLECTION_LOG_CONTAINER_ID;
    }

    @Override
    public void onItemContainerUpdated(Client client, ItemContainer container) {
        if (isAffectedBy(container)) {
            obtained = isObtained(container);
        }
    }
}
