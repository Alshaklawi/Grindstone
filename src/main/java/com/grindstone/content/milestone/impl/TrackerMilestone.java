package com.grindstone.content.milestone.impl;


import com.grindstone.config.GrindstoneConfig;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.content.tracker.TrackerEntry;
import com.grindstone.ui.components.MilestoneComponent;
import java.util.function.Supplier;
import java.awt.image.BufferedImage;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class TrackerMilestone implements Milestone {
    @Getter
    private final TrackerEntry tracker;

    @Getter
    private int count;

    private transient long current;
    private transient ItemManager itemManager;
    private transient boolean activityIcons = true;
    public void setItemManager(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    public TrackerMilestone(final TrackerEntry entry, final int count) {
        if (entry == null) {
            throw new IllegalArgumentException("Tracker entry is required");
        }

        if (count <= 0) {
            throw new IllegalArgumentException("Target count must be positive");
        }

        this.tracker = entry;
        this.count = count;
    }

    public void setCount(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Target count must be positive");
        }

        this.count = count;
    }

    @Override
    public boolean supportsTarget() {
        return true;
    }

    @Override
    public void setTarget(long target) {
        if (target < 1 || target > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Choose a positive target count");
        }
        setCount((int) target);
    }

    @Override
    public String getTitle(Client client) {
        return tracker.getDisplayName();
    }

    @Override
    public String getDescription() {
        return tracker.getDescription();
    }

    @Override
    public String getCardDescription() {
        return "";
    }

    @Override
    public BufferedImage getIcon(Client client) {
        if (activityIcons && itemManager != null) {
            int item = tracker.iconItem(client);
            if (item > 0) {
                return itemManager.getImage(item);
            }
        }
        return tracker.getIcon().getImage();
    }

    @Override
    public MilestoneCategory getCategory() {
        return tracker.getCategory();
    }

    @Override
    public MilestoneStatus getStatus() {
        long current = getProgressCurrent();
        return current >= count ? MilestoneStatus.COMPLETE : current > 0 ? MilestoneStatus.IN_PROGRESS : MilestoneStatus.NOT_STARTED;
    }

    @Override
    public boolean hasProgress() {
        return true;
    }

    @Override
    public long getProgressCurrent() {
        return current;
    }

    @Override
    public long getProgressMaximum() {
        return count;
    }

    @Override
    public String getProgressText() {
        return current + " / " + count;
    }

    @Override
    public Supplier<MilestoneComponent> prepare(Client client, GrindstoneConfig config) {
        activityIcons = config == null || config.showActivityIcons();
        refresh(client);
        return Milestone.super.prepare(client, config);
    }

    @Override
    public boolean refresh(Client client) {
        long next = Math.max(0, tracker.getCount(client));
        if (next == current) {
            return false;
        }

        current = next;
        return true;
    }
}
