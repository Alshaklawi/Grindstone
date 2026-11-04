package com.grindstone.ui;

import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.repository.RepeatSchedule;
import com.grindstone.repository.SidebarViewState;
import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import net.runelite.client.ui.components.IconTextField;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
public final class SidebarViewControls {
    private final IconTextField search;
    private final Map<String, JComboBox<String>> filters;
    private final JCheckBox pinned;
    private final Component filterOptions;
    private final Component dashboard;
    private final Map<MilestoneCategory, Boolean> categories;
    private final Map<RepeatSchedule, Boolean> repeats;

    public SidebarViewControls(IconTextField search, Map<String, JComboBox<String>> filters, JCheckBox pinned, Component filterOptions, Component dashboard, Map<MilestoneCategory, Boolean> categories, Map<RepeatSchedule, Boolean> repeats) {
        this.search = search;
        this.filters = filters;
        this.pinned = pinned;
        this.filterOptions = filterOptions;
        this.dashboard = dashboard;
        this.categories = categories;
        this.repeats = repeats;
    }

    public SidebarViewState capture(boolean routines) {
        Map<String, String> selected = new LinkedHashMap<>();
        filters.forEach((key, combo) -> selected.put(key, String.valueOf(combo.getSelectedItem())));
        Set<String> collapsedCategories = new LinkedHashSet<>();
        categories.forEach((key, value) -> {
            if (Boolean.TRUE.equals(value)) {
                collapsedCategories.add(key.name());
            }
        });
        Set<String> collapsedRepeats = new LinkedHashSet<>();
        repeats.forEach((key, value) -> {
            if (Boolean.TRUE.equals(value)) {
                collapsedRepeats.add(key.name());
            }
        });
        String query = search.getText();
        return new SidebarViewState(query.substring(0, Math.min(300, query.length())), selected, pinned.isSelected(), routines, filterOptions.isVisible(), dashboard.isVisible(), collapsedCategories, collapsedRepeats);
    }

    public void apply(SidebarViewState state) {
        search.setText(state.getSearch());
        filters.forEach((key, combo) -> {
            String value = state.getFilters().get(key);
            combo.setSelectedIndex(0);
            if ("category".equals(key)) {
                for (MilestoneCategory category : MilestoneCategory.values()) {
                    if (category.getDisplayName().equals(value)) {
                        boolean present = false;
                        for (int i = 0; i < combo.getItemCount(); i++) {
                            present |= value.equals(combo.getItemAt(i));
                        }
                        if (!present) {
                            combo.addItem(value);
                        }
                        break;
                    }
                }
            }
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (combo.getItemAt(i).equals(value)) {
                    combo.setSelectedIndex(i);
                    break;
                }
            }
        });
        pinned.setSelected(state.isPinnedOnly());
        filterOptions.setVisible(state.isFiltersExpanded());
        dashboard.setVisible(state.isDashboardExpanded());
        categories.clear();
        for (MilestoneCategory category : MilestoneCategory.values()) {
            categories.put(category, state.getCollapsedCategories().contains(category.name()));
        }
        repeats.clear();
        for (RepeatSchedule schedule : RepeatSchedule.values()) {
            repeats.put(schedule, state.getCollapsedRepeats().contains(schedule.name()));
        }
    }
}
