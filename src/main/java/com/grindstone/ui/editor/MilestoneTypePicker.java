package com.grindstone.ui.editor;

import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.ui.util.UiTypography;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.function.IntPredicate;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class MilestoneTypePicker extends JPanel {
    private static final String[] GROUPS = {"Skills", "Bossing", "Raids", "Slayer", "Clue Scrolls", "Minigames", "Collection Log", "Items", "Quests", "Achievement Diary", "Combat Achievements", "Personal"};
    private static final String[] DESCRIPTIONS = {"Build levels, earn experience or track skilling activities.", "Track boss kills, chests and combat challenge completions.", "Track raid completions and their difficulty modes.", "Track Slayer points and normal or Wilderness task streaks.", "Track completed clue scrolls.", "Track minigame games, wins, points and activity counts.", "Track collection-log totals, pages and individual unlocks.", "Track owned item quantities across known personal storage.", "Finish quests, follow a quest series or track quest speedruns.", "Complete achievement diary regions and tiers.", "Work through Combat Achievement tasks and reward tiers.", "Create your own milestone, checklist or repeating routine."};
    private static final MilestoneCategory[] CATEGORIES = {MilestoneCategory.SKILLING, MilestoneCategory.BOSSING, MilestoneCategory.RAIDS, MilestoneCategory.SLAYER, MilestoneCategory.CLUES, MilestoneCategory.MINIGAMES, MilestoneCategory.COLLECTION_LOG, MilestoneCategory.ITEMS, MilestoneCategory.QUEST, MilestoneCategory.ACHIEVEMENT_DIARY, MilestoneCategory.COMBAT_ACHIEVEMENT, MilestoneCategory.OTHER};
    private static final int[][] TYPES = {{0, 1, 2, 3, 4, 18, 19, 20, 27}, {5}, {23}, {24}, {25}, {26}, {22, 6, 7}, {16}, {9, 10, 28}, {11, 12}, {13, 14, 15, 17}, {8, 21}};
    private final JComboBox<String> model;
    private final JComboBox<Integer> selection = new JComboBox<>();
    private final JToggleButton[] groups = new JToggleButton[GROUPS.length];
    private final JLabel description = new JLabel();
    private boolean updating;
    private final IntPredicate available;

    public MilestoneTypePicker(JComboBox<String> model, IntPredicate available) {
        super(new BorderLayout(0, 10));
        this.model = model;
        this.available = available;
        setOpaque(false);
        JPanel focuses = new JPanel(new GridLayout(0, 2, 6, 6));
        focuses.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < GROUPS.length; i++) {
            final int index = i;
            JToggleButton button = new JToggleButton(GROUPS[i], new ImageIcon(ImageUtil.resizeImage(CATEGORIES[i].getIcon().getImage(), 16, 16)));
            button.setFont(UiTypography.BODY);
            button.setMargin(new Insets(9, 8, 9, 8));
            button.setToolTipText(DESCRIPTIONS[i]);
            button.addActionListener(event -> selectGroup(index));
            group.add(button);
            groups[i] = button;
            boolean visible = false;
            for (int kind : TYPES[i]) {
                visible |= available.test(kind);
            }
            if (visible) {
                focuses.add(button);
            }
        }
        JPanel top = new JPanel(new BorderLayout(0, 7));
        top.setOpaque(false);
        JLabel label = new JLabel("Choose your focus");
        label.setFont(UiTypography.TITLE);
        top.add(label, BorderLayout.NORTH);
        top.add(focuses, BorderLayout.CENTER);
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
        JPanel field = new JPanel(new BorderLayout(0, 5));
        field.setOpaque(false);
        field.add(typeLabel, BorderLayout.NORTH);
        field.add(selection, BorderLayout.CENTER);
        details.add(field, BorderLayout.CENTER);
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
                    groups[i].setSelected(true);
                    description.setText("<html>" + DESCRIPTIONS[i] + "</html>");
                    selection.removeAllItems();
                    for (int option : TYPES[i]) {
                        if (available.test(option)) {
                            selection.addItem(option);
                        }
                    }
                    selection.setSelectedItem(current);
                } finally {
                    updating = false;
                }
                return;
            }
        }
    }
}
