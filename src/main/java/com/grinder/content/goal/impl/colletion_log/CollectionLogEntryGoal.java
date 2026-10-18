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

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class CollectionLogEntryGoal implements Goal {

    private static final Color OBTAINED_COLOR = new Color(65, 170, 85);
    @Getter
    private final CollectionLogEntry entry;
    private GoalComponent current;
    private Client client;


    private ItemContainerComponent main;
    private JLabel progressLabel;

    public CollectionLogEntryGoal(final CollectionLogEntry entry) {
        this.entry = entry;
    }

    @Override
    public GoalComponent build(final Client client) {
        this.client = client;

        if (current == null) {
            current = new GoalComponent();

            // Create the goal-specific content once.
            main = new ItemContainerComponent();
            main.setAlignmentX(Component.LEFT_ALIGNMENT);

            progressLabel = new JLabel();
            progressLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            final JPanel content = current.getContent();
            content.add(main);
            content.add(Box.createVerticalStrut(6));
            content.add(progressLabel);
        }

        refresh(client);
        return current;
    }

    private void refresh(final Client client) {
        if (current == null || main == null || progressLabel == null) {
            return;
        }

        this.client = client;

        final ItemComposition[] items = entry.getItems(client);

        // Header.
        current.setTitle(getTitle(client));
        current.setIcon(getIcon(client));
        current.setStartDate(getStart());
        current.setEndDate(getEnd());

        final int obtained = (int) Arrays.stream(items).filter(item -> entry.isObtained(client, item)).count();
        // Update existing item components and create any new ones.
        final List<ItemComponent> components = new ArrayList<>();

        for (final ItemComposition item : items) {
            if (item == null) {
                continue;
            }


            final ItemComponent component = find(components, item.getId());

            if (component == null) {
                final ItemComponent created = new ItemComponent(item, Icon.MAIN.getImage());
                created.setTooltip(item.getName());
                components.add(created);
            } else {
                if (entry.isObtained(client, item)) {
                    component.setBorderColor(OBTAINED_COLOR);
                } else {
                    component.setBorderColor(null);
                }

            }
        }


        main.set(components);

        final int total = components.size();

        final int percentage = total == 0 ? 0 : (int) Math.round(obtained * 100.0 / total);
        progressLabel.setText(obtained + " / " + total + " (" + percentage + "%)");

        current.revalidate();
        current.repaint();
    }

    private ItemComponent find(final List<ItemComponent> components, final int item) {
        for (final ItemComponent component : components) {
            if (component.getItemId() == item) {
                return component;
            }
        }

        return null;
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
    public BufferedImage getIcon(Client client) {
        return Icon.COLLECTION_LOG.getImage();
    }
}
