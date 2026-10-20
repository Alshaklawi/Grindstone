package com.grindstone.content.milestone.impl.collectionlog;

import com.grindstone.config.GrindstoneConfig;
import com.grindstone.content.milestone.ClientCompletionMilestone;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.ui.components.MilestoneComponent;
import java.util.Locale;
import java.util.function.Supplier;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/20/2026
 */
public final class CollectionLogTotalMilestone extends ClientCompletionMilestone {
    private long target = 100;
    @Getter
    private transient int collectionMaximum;
    @Getter
    private transient boolean loaded;

    @Override
    public String selectionKey() {
        return "collection-total";
    }

    @Override
    public boolean supportsTarget() {
        return true;
    }

    @Override
    public void setTarget(long target) {
        if (target < 1 || target > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Choose a positive collection-log unlock target.");
        }
        this.target = target;
        if (loaded) {
            update(getProgressCurrent(), target, getProgressCurrent() > 0);
        }
    }

    @Override
    public String getTitle(Client client) {
        return "Total collection-log unlocks";
    }

    @Override
    public String getDescription() {
        return "Reach a total number of collection-log unlocks across all pages. Uses the game's total counter, not item quantities or a sum of page totals. Open your collection log if the total has not loaded.";
    }

    @Override
    public MilestoneCategory getCategory() {
        return MilestoneCategory.COLLECTION_LOG;
    }

    @Override
    public long getProgressMaximum() {
        return target;
    }

    @Override
    public String getProgressText() {
        return loaded ? String.format(Locale.ROOT, "%,d / %,d unlocks", getProgressCurrent(), target) : "Open your collection log to load the total";
    }

    @Override
    public boolean refresh(Client client) {
        int maximum = client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX);
        int current = client.getVarpValue(VarPlayerID.COLLECTION_COUNT);
        boolean nextLoaded = maximum > 0 && current >= 0 && current <= maximum;
        boolean changed = loaded != nextLoaded || collectionMaximum != maximum;
        loaded = nextLoaded;
        collectionMaximum = Math.max(0, maximum);
        return update(loaded ? current : 0, loaded ? target : 0, loaded && current > 0) || changed;
    }

    @Override
    public Supplier<MilestoneComponent> prepare(Client client, GrindstoneConfig config) {
        refresh(client);
        return super.prepare(client, config);
    }
}
