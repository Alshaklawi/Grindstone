package com.grindstone.ui.editor;

import com.grindstone.ui.util.UiTypography;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class MilestoneTypePicker extends JPanel {
    private final JComboBox<MilestoneType> model;
    private final JComboBox<MilestoneType> selection = new JComboBox<>();
    private final JComboBox<MilestoneFocus> focus = new JComboBox<>();
    private final JPanel typeField = new JPanel(new BorderLayout(0, 5));
    private final JLabel description = new JLabel();
    private boolean updating;
    private final Predicate<MilestoneType> available;

    public MilestoneTypePicker(JComboBox<MilestoneType> model, Predicate<MilestoneType> available) {
        super(new BorderLayout(0, 10));
        this.model = model;
        this.available = available;
        setOpaque(false);
        Map<MilestoneFocus, ImageIcon> icons = new EnumMap<>(MilestoneFocus.class);
        for (MilestoneFocus group : MilestoneFocus.values()) {
            for (MilestoneType kind : MilestoneType.values()) {
                if (kind.getFocus() == group && available.test(kind)) {
                    focus.addItem(group);
                    break;
                }
            }
            icons.put(group, new ImageIcon(ImageUtil.resizeImage(group.getCategory().getIcon().getImage(), 16, 16)));
        }
        focus.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused) {
                JLabel rendered = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof MilestoneFocus) {
                    rendered.setIcon(icons.get((MilestoneFocus) value));
                    rendered.setIconTextGap(8);
                }
                rendered.setFont(UiTypography.BODY);
                rendered.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
                return rendered;
            }
        });
        focus.setMaximumRowCount(MilestoneFocus.values().length);
        focus.setToolTipText("Choose what you want to track.");
        focus.addActionListener(event -> {
            if (!updating && focus.getSelectedItem() != null) {
                selectGroup((MilestoneFocus) focus.getSelectedItem());
            }
        });
        JPanel top = new JPanel(new BorderLayout(0, 7));
        top.setOpaque(false);
        JLabel label = new JLabel("Category");
        label.setFont(UiTypography.TITLE);
        label.setLabelFor(focus);
        top.add(label, BorderLayout.NORTH);
        top.add(focus, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        JPanel details = new JPanel(new BorderLayout(0, 7));
        details.setOpaque(false);
        description.setFont(UiTypography.BODY);
        description.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        description.setBorder(BorderFactory.createEmptyBorder(2, 0, 3, 0));
        details.add(description, BorderLayout.NORTH);
        selection.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                JLabel rendered = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                rendered.setText(value instanceof MilestoneType ? value.toString() : "Choose a milestone type");
                rendered.setFont(UiTypography.BODY);
                return rendered;
            }
        });
        selection.setToolTipText("Choose a milestone type within this focus. Your target fields appear below.");
        JLabel typeLabel = new JLabel("Type");
        typeLabel.setLabelFor(selection);
        typeLabel.setFont(UiTypography.BODY);
        typeField.setOpaque(false);
        typeField.add(typeLabel, BorderLayout.NORTH);
        typeField.add(selection, BorderLayout.CENTER);
        details.add(typeField, BorderLayout.CENTER);
        add(details, BorderLayout.CENTER);
        selection.addActionListener(event -> {
            if (!updating && selection.getSelectedItem() != null) {
                model.setSelectedItem(selection.getSelectedItem());
            }
        });
        model.addActionListener(event -> syncSelection());
        if (!available.test((MilestoneType) model.getSelectedItem())) {
            for (MilestoneType kind : MilestoneType.values()) {
                if (available.test(kind)) {
                    model.setSelectedItem(kind);
                    break;
                }
            }
        }
        syncSelection();
    }

    private void selectGroup(MilestoneFocus group) {
        MilestoneType current = (MilestoneType) model.getSelectedItem();
        if (current != null && current.getFocus() == group) {
            syncSelection();
            return;
        }
        for (MilestoneType kind : MilestoneType.values()) {
            if (kind.getFocus() == group && available.test(kind)) {
                model.setSelectedItem(kind);
                return;
            }
        }
    }

    private void syncSelection() {
        MilestoneType current = (MilestoneType) model.getSelectedItem();
        if (current == null) {
            return;
        }
        updating = true;
        try {
            MilestoneFocus group = current.getFocus();
            focus.setSelectedItem(group);
            description.setText("<html>" + group.getDescription() + "</html>");
            selection.removeAllItems();
            for (MilestoneType option : MilestoneType.values()) {
                if (option.getFocus() == group && available.test(option)) {
                    selection.addItem(option);
                }
            }
            selection.setSelectedItem(current);
            typeField.setVisible(selection.getItemCount() > 1);
        } finally {
            updating = false;
        }
    }
}
