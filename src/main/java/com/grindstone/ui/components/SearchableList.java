package com.grindstone.ui.components;

import com.grindstone.ui.util.UiTypography;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.IconTextField;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public abstract class SearchableList<T> extends JPanel {
    private static final String ALL = "All categories";
    private final IconTextField search = new IconTextField();
    private final JComboBox<String> category = new JComboBox<>();
    private final DefaultListModel<T> model = new DefaultListModel<>();
    private final JList<T> list = new JList<>(model);
    private final JLabel count = new JLabel();
    private final Map<T, String> labels = new LinkedHashMap<>();
    private final Map<T, String> categories = new LinkedHashMap<>();
    private final Consumer<T> onSelection;
    private boolean updating;

    protected SearchableList(Consumer<T> onSelection) {
        super(new BorderLayout(0, 6));
        this.onSelection = onSelection;
        setOpaque(false);
        JPanel controls = new JPanel(new BorderLayout(0, 6));
        controls.setOpaque(false);
        search.setIcon(IconTextField.Icon.SEARCH);
        search.setFont(UiTypography.BODY);
        search.setPreferredSize(new Dimension(300, 30));
        search.setToolTipText("Search names and categories. Every search word must match.");
        category.setFont(UiTypography.BODY);
        category.setToolTipText("Narrow the list to one category.");
        controls.add(search, BorderLayout.NORTH);
        controls.add(category, BorderLayout.SOUTH);
        add(controls, BorderLayout.NORTH);
        list.setFont(UiTypography.BODY);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> source, Object value, int index, boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(source, value, index, selected, focus);
                T entry = index >= 0 && index < model.size() ? model.get(index) : null;
                label.putClientProperty("html.disable", Boolean.TRUE);
                label.setText(labels.getOrDefault(entry, ""));
                label.setToolTipText(labels.getOrDefault(entry, "") + " · " + categories.getOrDefault(entry, ""));
                label.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                String group = categories.getOrDefault(entry, "");
                if (index > 0 && group.equals(categories.get(model.get(index - 1))) || group.isEmpty()) {
                    return label;
                }
                JPanel row = new JPanel(new BorderLayout(0, 3));
                row.setBackground(source.getBackground());
                JLabel heading = new JLabel(group);
                heading.putClientProperty("html.disable", Boolean.TRUE);
                heading.setFont(UiTypography.META);
                heading.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                heading.setBorder(BorderFactory.createEmptyBorder(7, 8, 2, 8));
                row.add(heading, BorderLayout.NORTH);
                row.add(label, BorderLayout.CENTER);
                return row;
            }
        });
        list.addListSelectionListener(event -> {
            if (!updating && !event.getValueIsAdjusting()) {
                onSelection.accept(list.getSelectedValue());
            }
        });
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(300, 180));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        add(scroll, BorderLayout.CENTER);
        count.setFont(UiTypography.META);
        count.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        add(count, BorderLayout.SOUTH);
        category.addActionListener(event -> refresh());
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                refresh();
            }
            @Override
            public void removeUpdate(DocumentEvent event) {
                refresh();
            }
            @Override
            public void changedUpdate(DocumentEvent event) {
                refresh();
            }
        });
    }

    protected abstract String getText(T value);

    protected String getCategory(T value) {
        return "";
    }

    public void setItems(Collection<T> values) {
        updating = true;
        try {
            labels.clear();
            categories.clear();
            TreeSet<String> groups = new TreeSet<>();
            for (T value : values) {
                labels.put(value, getText(value));
                String group = getCategory(value);
                categories.put(value, group);
                if (!group.isEmpty()) {
                    groups.add(group);
                }
            }
            search.setText("");
            List<String> options = new ArrayList<>();
            options.add(ALL);
            options.addAll(groups);
            category.setModel(new DefaultComboBoxModel<>(options.toArray(new String[0])));
            category.setVisible(groups.size() > 1);
        } finally {
            updating = false;
        }
        refresh(values.isEmpty() ? null : values.iterator().next());
    }

    public T getSelectedValue() {
        return list.getSelectedValue();
    }

    public void setSelectedValue(T value) {
        updating = true;
        try {
            search.setText("");
            category.setSelectedItem(ALL);
        } finally {
            updating = false;
        }
        refresh();
        list.setSelectedValue(value, true);
    }

    private void refresh() {
        refresh(list.getSelectedValue());
    }

    private void refresh(T previous) {
        if (updating) {
            return;
        }
        String query = search.getText().trim().toLowerCase(Locale.ROOT);
        String selectedCategory = (String) category.getSelectedItem();
        List<T> visible = new ArrayList<>();
        for (T value : labels.keySet()) {
            if (selectedCategory != null && !ALL.equals(selectedCategory) && !selectedCategory.equals(categories.get(value))) {
                continue;
            }
            String text = (labels.get(value) + " " + categories.get(value)).toLowerCase(Locale.ROOT);
            boolean matches = true;
            for (String word : query.split("\\s+")) {
                matches &= text.contains(word);
            }
            if (matches) {
                visible.add(value);
            }
        }
        visible.sort(Comparator.comparing((T value) -> categories.get(value)).thenComparing(labels::get, String.CASE_INSENSITIVE_ORDER));
        updating = true;
        try {
            model.clear();
            visible.forEach(model::addElement);
            if (visible.contains(previous)) {
                list.setSelectedValue(previous, true);
            } else if (!visible.isEmpty()) {
                list.setSelectedIndex(0);
            }
        } finally {
            updating = false;
        }
        count.setText(visible.isEmpty() ? "No matches. Change the search or category." : visible.size() + " of " + labels.size() + " choices");
        onSelection.accept(list.getSelectedValue());
    }
}
