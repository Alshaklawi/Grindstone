package com.grinder.ui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import com.grinder.ui.util.UiTypography;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class ItemContainerComponent extends JPanel {
    private static final int GAP = 4;
    private final JPanel grid = new JPanel();
    private final JButton showMore = new JButton();
    private final JLabel emptyLabel = new JLabel("No items to display");
    private final List<ItemComponent> items = new ArrayList<>();
    private List<ItemComponent> visibleItems = Collections.emptyList();
    private int itemsPerRow = 4;
    private int maxRows = 5;
    private int columns = 4;
    private boolean expanded;

    public ItemContainerComponent() {
        super(new BorderLayout(0, 6));
        setOpaque(false);
        setAlignmentX(LEFT_ALIGNMENT);
        grid.setOpaque(false);
        grid.setLayout(new GridLayout(0, columns, GAP, GAP));
        emptyLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        emptyLabel.setFont(UiTypography.BODY);
        emptyLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        showMore.setFont(UiTypography.BODY);
        showMore.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        showMore.addActionListener(event -> {
            expanded = !expanded;
            refresh();
        });
        add(grid, BorderLayout.CENTER);
        add(showMore, BorderLayout.SOUTH);
        refresh();
    }

    public void configure(int itemsPerRow, int maxRows) {
        int nextColumns = Math.max(1, Math.min(4, itemsPerRow));
        int nextRows = Math.max(1, Math.min(10, maxRows));
        if (this.itemsPerRow == nextColumns && this.maxRows == nextRows) { return; }
        this.itemsPerRow = nextColumns;
        this.maxRows = nextRows;
        refresh();
    }

    public void set(List<ItemComponent> components) {
        List<ItemComponent> next = new ArrayList<>();
        if (components != null) {
            for (ItemComponent component : components) {
                if (component != null && !next.contains(component)) { next.add(component); }
            }
        }
        if (items.equals(next)) { return; }
        items.clear();
        items.addAll(next);
        refresh();
    }

    public void add(ItemComponent component) {
        if (component == null || items.contains(component)) { return; }
        items.add(component);
        refresh();
    }

    public void clear() {
        if (items.isEmpty()) { return; }
        items.clear();
        expanded = false;
        refresh();
    }

    public List<ItemComponent> getItems() { return Collections.unmodifiableList(items); }

    public void setEmptyMessage(String message) {
        emptyLabel.setText(message);
        emptyLabel.setToolTipText(message);
    }

    private int columnsForWidth() {
        int width = getWidth();
        return width <= 0 ? itemsPerRow
                : Math.max(1, Math.min(itemsPerRow, (width + GAP) / (ItemComponent.CELL_SIZE + GAP)));
    }

    private void refresh() {
        int nextColumns = columnsForWidth();
        if (items.isEmpty()) {
            columns = nextColumns;
            grid.setLayout(new BorderLayout());
        }
        else if (columns != nextColumns || !(grid.getLayout() instanceof GridLayout)) {
            columns = nextColumns;
            grid.setLayout(new GridLayout(0, columns, GAP, GAP));
        }
        int count = expanded ? items.size() : Math.min(items.size(), columns * maxRows);
        List<ItemComponent> next = new ArrayList<>(items.subList(0, count));
        if (!visibleItems.equals(next) || items.isEmpty() && grid.getComponentCount() == 0) {
            grid.removeAll();
            if (next.isEmpty()) { grid.add(emptyLabel); }
            else { next.forEach(grid::add); }
            visibleItems = next;
        }
        boolean overflow = items.size() > columns * maxRows;
        showMore.setVisible(overflow);
        showMore.setText(expanded ? "Show fewer" : "Show all " + items.size() + " items");
        showMore.setToolTipText(expanded ? "Return to the configured preview height."
                : "Expand this grid. Use the main sidebar scrollbar to view the remaining items.");
        revalidate();
        repaint();
    }

    @Override
    public void doLayout() {
        if (columnsForWidth() != columns) { refresh(); }
        super.doLayout();
    }

    @Override
    public Dimension getPreferredSize() {
        int count = expanded ? items.size() : Math.min(items.size(), columnsForWidth() * maxRows);
        int cols = columnsForWidth();
        int rows = count == 0 ? 0 : (count + cols - 1) / cols;
        int height = rows == 0 ? emptyLabel.getPreferredSize().height
                : rows * ItemComponent.CELL_SIZE + Math.max(0, rows - 1) * GAP;
        if (items.size() > cols * maxRows) { height += 6 + showMore.getPreferredSize().height; }
        return new Dimension(cols * ItemComponent.CELL_SIZE + Math.max(0, cols - 1) * GAP, height);
    }

    @Override
    public Dimension getMinimumSize() { return new Dimension(ItemComponent.CELL_SIZE, getPreferredSize().height); }

    @Override
    public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
}
