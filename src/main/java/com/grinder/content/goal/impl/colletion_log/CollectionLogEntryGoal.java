
package com.grinder.content.goal.impl.colletion_log;

import com.grinder.content.CollectionLogEntry;
import com.grinder.content.goal.Goal;
import com.grinder.content.utils.Icon;
import com.grinder.ui.GoalComponent;
import com.grinder.ui.ItemComponent;
import com.grinder.ui.ItemContainerComponent;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.client.game.ItemManager;

import javax.swing.BoxLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class CollectionLogEntryGoal implements Goal {
    private static final Color OBTAINED_COLOR = new Color(65, 170, 85);

    @Getter
    private final CollectionLogEntry entry;

    private transient ItemManager itemManager;
    private transient Map<Integer, ItemComponent> itemComponents = new HashMap<>();
    private transient GoalComponent current;
    private transient ItemContainerComponent main;

    public CollectionLogEntryGoal(final ItemManager itemManager, final CollectionLogEntry entry) {
        this.itemManager = itemManager;
        this.entry = entry;
    }

    public void setItemManager(ItemManager itemManager) {
        this.itemManager = itemManager;
        itemComponents.clear();
    }

    @Override
    public GoalComponent build(final Client client) {
        if (current == null) {
            current = new GoalComponent();
            main = new ItemContainerComponent();
            main.setAlignmentX(Component.LEFT_ALIGNMENT);
            current.getContent().setLayout(new BoxLayout(current.getContent(), BoxLayout.Y_AXIS));
            current.getContent().add(main);
        }

        refresh(client);
        return current;
    }

    private void refresh(final Client client) {
        if (current == null || main == null) {
            return;
        }

        current.setTitle(getTitle(client));
        current.setIcon(getIcon(client));
        current.setStartDate(getStart());
        current.setEndDate(getEnd());

        final ItemComposition[] items = entry.getItems(client);
        final List<ItemComponent> components = new ArrayList<>(items.length);
        final Set<Integer> activeItemIds = new HashSet<>();
        int obtained = 0;

        for (final ItemComposition item : items) {
            if (item == null) {
                continue;
            }

            final int itemId = item.getId();
            final boolean isObtained = entry.isObtained(client, item);

            if (isObtained) {
                obtained++;
            }

            activeItemIds.add(itemId);

            ItemComponent component = itemComponents.get(itemId);

            if (component == null) {
                component = itemManager == null
                        ? new ItemComponent(item, null)
                        : new ItemComponent(itemManager, item);
                itemComponents.put(itemId, component);
            } else {
                component.setItem(item);
            }

            component.setTooltip(item.getName());
            component.setBorderColor(isObtained ? OBTAINED_COLOR : null);
            components.add(component);
        }

        itemComponents.keySet().retainAll(activeItemIds);
        main.set(components);

        final int total = components.size();
        final int percentage = total == 0 ? 0 : Math.round(obtained * 100.0f / total);

        current.setProgress(obtained, total, String.format("%d / %d obtained (%d%%)", obtained, total, percentage));
        current.revalidate();
        current.repaint();
    }

    @Override
    public boolean hasProgress(final Client client) {
        return getProgressMaximum(client) > 0;
    }

    @Override
    public long getProgressCurrent(final Client client) {
        int obtained = 0;

        for (final ItemComposition item : entry.getItems(client)) {
            if (item != null && entry.isObtained(client, item)) {
                obtained++;
            }
        }

        return obtained;
    }

    @Override
    public long getProgressMaximum(final Client client) {
        int total = 0;

        for (final ItemComposition item : entry.getItems(client)) {
            if (item != null) {
                total++;
            }
        }

        return total;
    }

    @Override
    public String getProgressText(final Client client) {
        final long obtained = getProgressCurrent(client);
        final long total = getProgressMaximum(client);
        final int percentage = total == 0 ? 0 : (int) Math.round(obtained * 100.0 / total);
        return String.format("%,d / %,d obtained (%d%%)", obtained, total, percentage);
    }

    @Override
    public void onItemContainerUpdated(final Client client, final ItemContainer container) {
        if (container != null && container.getId() == 620 && current != null) {
            refresh(client);
        }
    }

    @Override
    public String getTitle(final Client client) {
        return entry.getTitle(client);
    }

    @Override
    public BufferedImage getIcon(final Client client) {
        return Icon.COLLECTION_LOG.getImage();
    }
}
