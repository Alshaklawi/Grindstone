package com.grindstone.content.milestone.impl.collectionlog;

import com.grindstone.content.collectionlog.CollectionLogHistory;
import com.grindstone.content.collectionlog.CollectionLogPages;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.ui.Icon;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/20/2026
 */
public final class CollectionLogTabMilestone implements Milestone {
    @Getter
    private final int tabStruct;
    @Getter
    private final String tabName;
    @Setter
    private transient CollectionLogHistory history;
    private transient long obtained;
    private transient long total;

    public CollectionLogTabMilestone(int tabStruct, String tabName) {
        if (tabStruct <= 0 || tabName == null || tabName.trim().isEmpty() || tabName.length() > 200) {
            throw new IllegalArgumentException("Invalid collection-log tab");
        }
        this.tabStruct = tabStruct;
        this.tabName = tabName;
    }

    @Override
    public String getTitle(Client client) {
        return "Collection log: " + tabName;
    }

    @Override
    public MilestoneCategory getCategory() {
        return MilestoneCategory.COLLECTION_LOG;
    }

    @Override
    public BufferedImage getIcon(Client client) {
        switch (tabName) {
            case "Bosses":
                return Icon.BOSSING.getImage();
            case "Raids":
                return Icon.RAIDS.getImage();
            case "Clues":
                return Icon.CLUES.getImage();
            case "Minigames":
                return Icon.MINIGAMES.getImage();
            default:
                return Icon.COLLECTION_LOG.getImage();
        }
    }

    @Override
    public MilestoneStatus getStatus() {
        return total > 0 && obtained >= total ? MilestoneStatus.COMPLETE : obtained > 0 ? MilestoneStatus.IN_PROGRESS : MilestoneStatus.NOT_STARTED;
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
    public String getProgressText() {
        return obtained + " / " + total + " unlocks";
    }

    @Override
    public boolean isAffectedBy(ItemContainer container) {
        return container != null && container.getId() == InventoryID.COLLECTION_TRANSMIT;
    }

    @Override
    public void onItemContainerUpdated(Client client, ItemContainer container) {
        refresh(client);
    }

    @Override
    public boolean refresh(Client client) {
        int[] items = CollectionLogPages.getTabItemIds(client, tabStruct);
        if (items.length == 0 || history == null) {
            return false;
        }
        long next = Arrays.stream(items).filter(history::contains).count();
        boolean changed = next != obtained || total != items.length;
        obtained = next;
        total = items.length;
        return changed;
    }
}
