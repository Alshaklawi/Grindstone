
package com.grinder.repository;

import com.grinder.content.CollectionLogEntry;
import com.grinder.content.TrackerEntry;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.impl.TrackerGoal;
import com.grinder.content.goal.impl.colletion_log.CollectionLogEntryGoal;
import lombok.Getter;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.client.game.ItemManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
@Getter
public class GrinderProfile {
    private final long hash;
    private final List<Goal> goals = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();

    public GrinderProfile(long hash) {
        this(hash, null);
    }

    public GrinderProfile(long hash, ItemManager itemManager) {
        this.hash = hash;
        initializeGoals(itemManager);
    }

    private void initializeGoals(ItemManager itemManager) {
        for (final CollectionLogEntry entry : CollectionLogEntry.values()) {
            goals.add(new CollectionLogEntryGoal(itemManager, entry));
        }

        for (final TrackerEntry entry : TrackerEntry.values()) {
            goals.add(new TrackerGoal(entry, 5000));
        }
    }

    public void setItemManager(ItemManager itemManager) {
        for (Goal goal : goals) {
            if (goal instanceof CollectionLogEntryGoal) {
                ((CollectionLogEntryGoal) goal).setItemManager(itemManager);
            }
        }
    }

    public boolean add(Goal goal) {
        return goals.add(Objects.requireNonNull(goal, "goal"));
    }

    public boolean remove(Goal goal) {
        return goals.remove(goal);
    }

    public List<Goal> getGoals() {
        return Collections.unmodifiableList(goals);
    }

    public void replace(List<Goal> goals) {
        this.goals.clear();
        this.goals.addAll(goals);
    }

    public void onContainerOpen(ItemContainer container) {
        items.clear();

        if (container == null) {
            return;
        }

        for (Item item : container.getItems()) {
            if (item != null && item.getId() > 0 && item.getQuantity() > 0) {
                items.add(item);
            }
        }
    }
}
