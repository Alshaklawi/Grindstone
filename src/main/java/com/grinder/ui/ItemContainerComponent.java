
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
    private static final int MAX_ROWS = 5;

    private static final int ROW_HEIGHT = ITEM_SIZE + ITEM_GAP;
    private static final int MAX_VISIBLE_HEIGHT = MAX_ROWS * ROW_HEIGHT;

    private final JPanel grid = new JPanel(new GridBagLayout());
    private final JScrollPane scroll;
    private final List<ItemComponent> items = new ArrayList<>();

    public ItemContainerComponent() {
        setLayout(new BorderLayout());
        setOpaque(false);

        grid.setOpaque(false);
        grid.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        scroll = new JScrollPane(grid, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setViewportBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getViewport().setBackground(ColorScheme.DARKER_GRAY_COLOR);
        scroll.getViewport().setScrollMode(JViewport.BLIT_SCROLL_MODE);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        JScrollBar vertical = scroll.getVerticalScrollBar();
        vertical.setUnitIncrement(ITEM_SIZE + ITEM_GAP);
        vertical.setPreferredSize(new Dimension(8, 0));
        vertical.setBorder(BorderFactory.createEmptyBorder());
        vertical.setBackground(ColorScheme.DARK_GRAY_COLOR);
        vertical.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = ColorScheme.MEDIUM_GRAY_COLOR;
                trackColor = ColorScheme.DARK_GRAY_COLOR;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                button.setFocusable(false);
                button.setBorder(BorderFactory.createEmptyBorder());
                button.setOpaque(false);
                return button;
            }
        });

        add(scroll, BorderLayout.CENTER);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, MAX_VISIBLE_HEIGHT));
    }

    public void set(List<ItemComponent> newItems) {
        items.clear();

        if (newItems != null) {
            items.addAll(newItems);
        }

        refresh();
    }

    /** Adds an item to the displayed collection. */
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

    /** Rebuilds the four-column grid and caps the visible area at five rows. */
    public void refresh() {
        grid.removeAll();

        for (int i = 0; i < items.size(); i++) {
            ItemComponent item = items.get(i);
            Dimension itemSize = new Dimension(ITEM_SIZE, ITEM_SIZE);

            item.setPreferredSize(itemSize);
            item.setMinimumSize(itemSize);
            item.setMaximumSize(itemSize);

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
        int gridHeight = rows == 0 ? 0 : rows * ROW_HEIGHT;
        int gridWidth = ITEMS_PER_ROW * ROW_HEIGHT;

        grid.setPreferredSize(new Dimension(gridWidth, gridHeight));
        grid.setMinimumSize(new Dimension(gridWidth, gridHeight));
        grid.setMaximumSize(new Dimension(gridWidth, gridHeight));

        scroll.setPreferredSize(new Dimension(gridWidth + 8, Math.min(gridHeight, MAX_VISIBLE_HEIGHT)));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, MAX_VISIBLE_HEIGHT));
        scroll.setVerticalScrollBarPolicy(rows > MAX_ROWS
                ? ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
                : ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        int rows = (items.size() + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW;
        int height = Math.min(rows * ROW_HEIGHT, MAX_VISIBLE_HEIGHT);
        int width = ITEMS_PER_ROW * ROW_HEIGHT + 8;

        return new Dimension(width, height);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, MAX_VISIBLE_HEIGHT);
    }
}
