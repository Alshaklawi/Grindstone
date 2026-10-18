
package com.grinder.ui;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

public class ItemContainerComponent extends JPanel
{
    private static final int ITEM_GAP = 4;
    private static final int ITEM_SIZE = 40;

    private final JPanel grid = new JPanel();
    private final JScrollPane scroll;
    private final List<ItemComponent> items = new ArrayList<>();

    private int itemsPerRow = 4;
    private int maxRows = 5;

    public ItemContainerComponent()
    {
        setLayout(new java.awt.BorderLayout());
        setOpaque(false);

        grid.setOpaque(false);
        grid.setLayout(new GridLayout(0, itemsPerRow, ITEM_GAP, ITEM_GAP));

        scroll = new JScrollPane(grid, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(ITEM_SIZE + ITEM_GAP);

        add(scroll, java.awt.BorderLayout.CENTER);
        refresh();
    }

    public void configure(int itemsPerRow, int maxRows)
    {
        this.itemsPerRow = Math.max(1, Math.min(8, itemsPerRow));
        this.maxRows = Math.max(1, Math.min(10, maxRows));
        grid.setLayout(new GridLayout(0, this.itemsPerRow, ITEM_GAP, ITEM_GAP));
        refresh();
    }

    public void set(List<ItemComponent> components)
    {
        items.clear();

        if (components != null)
        {
            items.addAll(components);
        }

        refresh();
    }

    public void add(ItemComponent component)
    {
        if (component == null)
        {
            return;
        }

        items.add(component);
        refresh();
    }

    public void clear()
    {
        items.clear();
        refresh();
    }

    public List<ItemComponent> getItems()
    {
        return Collections.unmodifiableList(items);
    }

    public void refresh()
    {
        grid.removeAll();

        for (ItemComponent item : items)
        {
            grid.add(item);
        }

        int rows = Math.max(1, (int) Math.ceil((double) items.size() / itemsPerRow));
        int visibleRows = Math.min(rows, maxRows);
        int width = itemsPerRow * ITEM_SIZE + Math.max(0, itemsPerRow - 1) * ITEM_GAP;
        int height = visibleRows * ITEM_SIZE + Math.max(0, visibleRows - 1) * ITEM_GAP;

        grid.setPreferredSize(new Dimension(width, Math.max(ITEM_SIZE, rows * ITEM_SIZE + Math.max(0, rows - 1) * ITEM_GAP)));
        scroll.setPreferredSize(new Dimension(width, height + 2));
        scroll.setVerticalScrollBarPolicy(rows > maxRows ? ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED : ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        revalidate();
        repaint();
    }
}
