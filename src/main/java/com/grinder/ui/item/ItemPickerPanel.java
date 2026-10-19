package com.grinder.ui.item;

import com.grinder.ui.util.UiTooltips;
import com.grinder.util.HtmlUtils;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.IconTextField;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class ItemPickerPanel extends JPanel {
    private final ItemLookup service;
    private final IconTextField search = new IconTextField();
    private final DefaultListModel<ItemSearchService.Result> model = new DefaultListModel<>();
    private final JList<ItemSearchService.Result> results = new JList<>(model);
    private final JLabel status = new JLabel();
    private final JLabel preview = new JLabel("No item selected");
    private final Timer debounce = new Timer(220, event -> search());
    private final Consumer<ItemSearchService.Result> onSelection;
    private ItemSearchService.Result selected;
    private Runnable cancelSearch = () -> {
    };
    private boolean updating;
    private int queryVersion;

    public ItemPickerPanel(ItemLookup service, Consumer<ItemSearchService.Result> onSelection) {
        super(new BorderLayout(0, 8));
        this.service = service;
        this.onSelection = onSelection;
        setOpaque(false);
        debounce.setRepeats(false);
        search.setIcon(IconTextField.Icon.SEARCH);
        search.setPreferredSize(new Dimension(350, 32));
        search.addActionListener(event -> search());
        UiTooltips.set(search, "Search normal item names (at least 2 letters), or enter an ID with optional #. Noted and placeholder IDs resolve to the normal item; placeholders never count as owned items.");
        add(search, BorderLayout.NORTH);
        results.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        results.setFixedCellHeight(42);
        results.setVisibleRowCount(4);
        results.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof ItemSearchService.Result) {
                    ItemSearchService.Result item = (ItemSearchService.Result) value;
                    label.setText("<html>" + HtmlUtils.escape(item.name) + "<br><small>Item ID: " + item.id + "</small></html>");
                    label.setIcon(item.image == null ? null : new ImageIcon(item.image));
                    label.setToolTipText(item.name + " (ID " + item.id + ")");
                    label.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
                }
                return label;
            }
        });
        results.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !updating) {
                select(results.getSelectedValue());
            }
        });
        JScrollPane list = new JScrollPane(results);
        list.setPreferredSize(new Dimension(350, 174));
        list.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(list, BorderLayout.CENTER);
        JPanel info = new JPanel(new BorderLayout(0, 8));
        info.setOpaque(false);
        status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        status.setFont(FontManager.getRunescapeSmallFont());
        setStatus("Search by item name or numeric ID.");
        preview.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        preview.setOpaque(true);
        preview.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        preview.setPreferredSize(new Dimension(350, 60));
        preview.setToolTipText("Confirm the item name, ID and icon before adding the goal.");
        info.add(status, BorderLayout.NORTH);
        info.add(preview, BorderLayout.CENTER);
        add(info, BorderLayout.SOUTH);
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                queryChanged();
            }
            @Override
            public void removeUpdate(DocumentEvent event) {
                queryChanged();
            }
            @Override
            public void changedUpdate(DocumentEvent event) {
                queryChanged();
            }
        });
    }

    private void queryChanged() {
        queryVersion++;
        cancelSearch.run();
        model.clear();
        select(null);
        setStatus("Searching...");
        debounce.restart();
    }

    void search() {
        debounce.stop();
        cancelSearch.run();
        int version = queryVersion;
        String text = search.getText().trim();
        if (text.isEmpty()) {
            setStatus("Enter an item name or numeric ID.");
            return;
        }
        cancelSearch = service.search(text, response -> {
            if (version != queryVersion) {
                return;
            }
            int selectedId = selected == null ? -1 : selected.id;
            updating = true;
            try {
                model.clear();
                for (ItemSearchService.Result item : response.results) {
                    model.addElement(item);
                    if (item.id == selectedId) {
                        results.setSelectedValue(item, true);
                    }
                    if (item.image != null) {
                        item.image.onLoaded(results::repaint);
                    }
                }
                if (response.complete && response.results.size() == 1) {
                    results.setSelectedIndex(0);
                }
            } finally {
                updating = false;
            }
            setStatus(response.message);
            select(results.getSelectedValue());
        });
    }

    private void select(ItemSearchService.Result item) {
        selected = item;
        preview.setIcon(null);
        preview.setText(item == null ? "No item selected" : "<html>" + HtmlUtils.escape(item.name) + "<br>Item ID: " + item.id + "</html>");
        if (item != null && item.image != null) {
            preview.setIcon(new ImageIcon(item.image));
            item.image.onLoaded(() -> SwingUtilities.invokeLater(() -> {
                if (selected == item) {
                    preview.repaint();
                }
            }));
        }
        onSelection.accept(item);
    }

    public int getSelectedItemId() {
        if (selected == null) {
            throw new IllegalArgumentException("Search for an item and select its name and icon first.");
        }
        return selected.id;
    }

    public boolean hasSelection() {
        return selected != null;
    }
    public String getSelectedItemName() {
        if (selected == null) {
            throw new IllegalArgumentException("Select an item first.");
        }
        return selected.name;
    }
    public void loadItem(int id) {
        search.setText(Integer.toString(id));
        search();
    }

    public void stopSearch() {
        queryVersion++;
        debounce.stop();
        cancelSearch.run();
    }

    public void resumeSearch() {
        if (!search.getText().trim().isEmpty() && selected == null) {
            debounce.restart();
        }
    }

    private void setStatus(String message) {
        status.setText("<html><div style='width: 330px'>" + HtmlUtils.escape(message) + "</div></html>");
        status.setToolTipText(message);
    }

    @Override
    public void removeNotify() {
        stopSearch();
        super.removeNotify();
    }
}
