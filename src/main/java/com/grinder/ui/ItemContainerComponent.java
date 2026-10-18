package com.grinder.ui;

import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class ItemContainerComponent extends JPanel {
    private static final int ITEMS_PER_ROW = 4;
    private static final int ITEM_GAP = 4;
    private static final int ITEM_SIZE = 40;

    private final JPanel grid;
    private final List<ItemComponent> items = new ArrayList<>();

    public ItemContainerComponent() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);

        grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        add(grid);
    }

    public void set(List<ItemComponent> newItems) {
        items.clear();

        if (newItems != null) {
            items.addAll(newItems);
        }

        refresh();
    }

    /**
     * Adds an item to the displayed collection.
     */
    public void add(ItemComponent item) {
        if (item == null) {
            return;
        }

        items.add(item);
        refresh();
    }

    public void clear() {
        items.clear();
        refresh();
    }

    public List<ItemComponent> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Rebuilds the four-column grid and recalculates its required height.
     */
    public void refresh() {
        grid.removeAll();

        for (int i = 0; i < items.size(); i++) {
            ItemComponent item = items.get(i);
            item.setPreferredSize(new Dimension(ITEM_SIZE, ITEM_SIZE));
            item.setMinimumSize(new Dimension(ITEM_SIZE, ITEM_SIZE));
            item.setMaximumSize(new Dimension(ITEM_SIZE, ITEM_SIZE));
            item.setAlignmentX(Component.LEFT_ALIGNMENT);

            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = i % ITEMS_PER_ROW;
            constraints.gridy = i / ITEMS_PER_ROW;
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.insets = new Insets(0, 0, ITEM_GAP, ITEM_GAP);

            grid.add(item, constraints);
        }

        int rows = (items.size() + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW;
        int height = rows == 0 ? 0 : rows * ITEM_SIZE + rows * ITEM_GAP;

        grid.setPreferredSize(new Dimension(ITEMS_PER_ROW * (ITEM_SIZE + ITEM_GAP), height));
        grid.setMinimumSize(new Dimension(0, height));
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));

        grid.revalidate();
        grid.repaint();
        revalidate();
        repaint();

        if (getParent() != null) {
            getParent().revalidate();
            getParent().repaint();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension preferred = super.getPreferredSize();
        int rows = (items.size() + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW;
        int height = rows == 0 ? 0 : rows * ITEM_SIZE + rows * ITEM_GAP;
        return new Dimension(preferred.width, height);
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension preferred = getPreferredSize();
        return new Dimension(Integer.MAX_VALUE, preferred.height);
    }
}
