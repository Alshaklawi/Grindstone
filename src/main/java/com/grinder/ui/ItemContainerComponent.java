
package com.grinder.ui;

import java.awt.BorderLayout;
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
    private static final int SCROLLBAR_WIDTH = 10;

    private final JPanel grid = new JPanel();
    private final JScrollPane scroll = new JScrollPane(grid);
    private final List<ItemComponent> items = new ArrayList<>();

    private int itemsPerRow = 4;
    private int maxRows = 5;
    private List<ItemComponent> renderedItems = Collections.emptyList();
    private Dimension renderedGridSize;
    private Dimension renderedPreferredSize;

    public ItemContainerComponent()
    {
        super(new BorderLayout());
        setOpaque(false);
        setAlignmentX(LEFT_ALIGNMENT);

        grid.setOpaque(false);
        grid.setLayout(new GridLayout(0, itemsPerRow, ITEM_GAP, ITEM_GAP));

        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(ITEM_SIZE + ITEM_GAP);

        add(scroll, BorderLayout.CENTER);
        refresh();
    }

    public void configure(int itemsPerRow, int maxRows)
    {
        int nextItemsPerRow = Math.max(1, Math.min(8, itemsPerRow));
        int nextMaxRows = Math.max(1, Math.min(10, maxRows));

        if (this.itemsPerRow == nextItemsPerRow && this.maxRows == nextMaxRows)
        {
            return;
        }

        this.itemsPerRow = nextItemsPerRow;
        this.maxRows = nextMaxRows;
        grid.setLayout(new GridLayout(0, nextItemsPerRow, ITEM_GAP, ITEM_GAP));
        renderedGridSize = null;
        renderedPreferredSize = null;
        refresh();
    }

    public void set(List<ItemComponent> components)
    {
        List<ItemComponent> nextItems = components == null
                ? Collections.emptyList()
                : new ArrayList<>(components);

        if (items.equals(nextItems))
        {
            return;
        }

        items.clear();
        items.addAll(nextItems);
        refresh();
    }

    public void add(ItemComponent component)
    {
        if (component == null || items.contains(component))
        {
            return;
        }

        items.add(component);
        refresh();
    }

    public void clear()
    {
        if (items.isEmpty())
        {
            return;
        }

        items.clear();
        refresh();
    }

    public List<ItemComponent> getItems()
    {
        return Collections.unmodifiableList(items);
    }

    private void refresh()
    {
        int scrollPosition = scroll.getVerticalScrollBar().getValue();
        boolean childrenChanged = !renderedItems.equals(items);
        boolean sizeChanged;

        int rows = items.isEmpty() ? 0 : (items.size() + itemsPerRow - 1) / itemsPerRow;
        int visibleRows = Math.max(1, Math.min(rows, maxRows));
        int gridWidth = itemsPerRow * ITEM_SIZE + (itemsPerRow - 1) * ITEM_GAP;
        int gridHeight = Math.max(1, rows) * ITEM_SIZE + Math.max(0, rows - 1) * ITEM_GAP;
        int visibleHeight = visibleRows * ITEM_SIZE + (visibleRows - 1) * ITEM_GAP;

        Dimension nextGridSize = new Dimension(gridWidth, gridHeight);
        Dimension nextPreferredSize = new Dimension(
                gridWidth + SCROLLBAR_WIDTH,
                visibleHeight + 4);

        sizeChanged = !nextGridSize.equals(renderedGridSize)
                || !nextPreferredSize.equals(renderedPreferredSize);

        if (childrenChanged)
        {
            grid.removeAll();

            for (ItemComponent item : items)
            {
                grid.add(item);
            }

            renderedItems = new ArrayList<>(items);
        }

        if (!nextGridSize.equals(renderedGridSize))
        {
            grid.setPreferredSize(nextGridSize);
            renderedGridSize = nextGridSize;
        }

        if (!nextPreferredSize.equals(renderedPreferredSize))
        {
            setPreferredSize(nextPreferredSize);
            setMinimumSize(new Dimension(gridWidth, 1));
            setMaximumSize(new Dimension(gridWidth + SCROLLBAR_WIDTH, Integer.MAX_VALUE));
            renderedPreferredSize = nextPreferredSize;
        }

        scroll.setVerticalScrollBarPolicy(rows > maxRows
                ? ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
                : ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        if (childrenChanged || sizeChanged)
        {
            grid.revalidate();
            revalidate();
        }

        if (childrenChanged)
        {
            grid.repaint();
            repaint();
        }

        scroll.getVerticalScrollBar().setValue(Math.min(
                scrollPosition,
                scroll.getVerticalScrollBar().getMaximum()));
    }
}
