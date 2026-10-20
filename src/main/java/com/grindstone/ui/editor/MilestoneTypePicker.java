package com.grindstone.ui.editor;

import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.ui.util.UiTypography;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.function.IntPredicate;
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
    private static final String[] GROUPS = {"Skills", "Bossing", "Raids", "Slayer", "Clue Scrolls", "Minigames", "Collection Log", "Items", "Quests", "Achievement Diary", "Combat Achievements", "Personal"};
    private static final String[] DESCRIPTIONS = {"Build levels, earn experience or track skilling activities.", "Track boss kills, chests and combat challenge completions.", "Track raid completions and their difficulty modes.", "Track Slayer points and normal or Wilderness task streaks.", "Track completed clue scrolls.", "Track minigame games, wins, points and activity counts.", "Track collection-log totals, tabs, pages and individual unlocks.", "Track owned item quantities across known personal storage.", "Finish quests or follow a quest series.", "Complete achievement diary regions and tiers.", "Work through Combat Achievement tasks and reward tiers.", "Create your own milestone, checklist or repeating routine."};
    private static final MilestoneCategory[] CATEGORIES = {MilestoneCategory.SKILLING, MilestoneCategory.BOSSING, MilestoneCategory.RAIDS, MilestoneCategory.SLAYER, MilestoneCategory.CLUES, MilestoneCategory.MINIGAMES, MilestoneCategory.COLLECTION_LOG, MilestoneCategory.ITEMS, MilestoneCategory.QUEST, MilestoneCategory.ACHIEVEMENT_DIARY, MilestoneCategory.COMBAT_ACHIEVEMENT, MilestoneCategory.OTHER};
    private static final int[][] TYPES = {{0, 1, 2, 3, 4, 18, 19, 20, 27}, {5}, {23}, {24}, {25}, {26}, {22, 29, 6, 7}, {16}, {9, 10, 28}, {11, 12}, {13, 14, 15, 17}, {8, 21}};
    private final JComboBox<String> model;
    private final JComboBox<Integer> selection = new JComboBox<>();
    private final JComboBox<Integer> focus = new JComboBox<>();
    private final JPanel typeField = new JPanel(new BorderLayout(0, 5));
    private final JLabel description = new JLabel();
    private boolean updating;
    private final IntPredicate available;

    public MilestoneTypePicker(JComboBox<String> model, IntPredicate available) {
        super(new BorderLayout(0, 10));
        this.model = model;
        this.available = available;
        setOpaque(false);
        for (int i = 0; i < GROUPS.length; i++) {
            boolean visible = false;
            for (int kind : TYPES[i]) {
                visible |= available.test(kind);
            }
            if (visible) {
                focus.addItem(i);
            }
        }
        ImageIcon[] icons = new ImageIcon[GROUPS.length];
        for (int i = 0; i < GROUPS.length; i++) {
            icons[i] = new ImageIcon(ImageUtil.resizeImage(CATEGORIES[i].getIcon().getImage(), 16, 16));
        }
        focus.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused) {
                JLabel rendered = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof Integer) {
                    int category = (Integer) value;
                    rendered.setText(GROUPS[category]);
                    rendered.setIcon(icons[category]);
                    rendered.setIconTextGap(8);
                }
                rendered.setFont(UiTypography.BODY);
                rendered.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
                return rendered;
            }
        });
        focus.setMaximumRowCount(GROUPS.length);
        focus.setToolTipText("Choose what you want to track.");
        focus.addActionListener(event -> {
            if (!updating && focus.getSelectedItem() != null) {
                selectGroup((Integer) focus.getSelectedItem());
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
                rendered.setText(value instanceof Integer ? model.getItemAt((Integer) value) : "Choose a milestone type");
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
                model.setSelectedIndex((Integer) selection.getSelectedItem());
            }
        });
        model.addActionListener(event -> syncSelection());
        if (!available.test(model.getSelectedIndex())) {
            for (int kind = 0; kind < model.getItemCount(); kind++) {
                if (available.test(kind)) {
                    model.setSelectedIndex(kind);
                    break;
                }
            }
        }
        syncSelection();
    }

    private void selectGroup(int index) {
        int current = model.getSelectedIndex();
        for (int kind : TYPES[index]) {
            if (kind == current) {
                syncSelection();
                return;
            }
        }
        for (int kind : TYPES[index]) {
            if (available.test(kind)) {
                model.setSelectedIndex(kind);
                return;
            }
        }
    }

    private void syncSelection() {
        int current = model.getSelectedIndex();
        for (int i = 0; i < TYPES.length; i++) {
            for (int kind : TYPES[i]) {
                if (kind != current) {
                    continue;
                }
                updating = true;
                try {
                    focus.setSelectedItem(i);
                    description.setText("<html>" + DESCRIPTIONS[i] + "</html>");
                    selection.removeAllItems();
                    for (int option : TYPES[i]) {
                        if (available.test(option)) {
                            selection.addItem(option);
                        }
                    }
                    selection.setSelectedItem(current);
                    typeField.setVisible(selection.getItemCount() > 1);
                } finally {
                    updating = false;
                }
                return;
            }
        }
    }
}
