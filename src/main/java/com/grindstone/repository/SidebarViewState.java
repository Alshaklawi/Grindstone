package com.grindstone.repository;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import lombok.Value;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
@Value
public class SidebarViewState {
    String search;
    Map<String, String> filters;
    boolean pinnedOnly;
    boolean routines;
    boolean filtersExpanded;
    boolean dashboardExpanded;
    Set<String> collapsedCategories;
    Set<String> collapsedRepeats;

    public SidebarViewState(String search, Map<String, String> filters, boolean pinnedOnly, boolean routines, boolean filtersExpanded, boolean dashboardExpanded, Set<String> collapsedCategories, Set<String> collapsedRepeats) {
        if (search == null || search.length() > 300 || filters == null || filters.size() > 6 || collapsedCategories == null || collapsedCategories.size() > 50 || collapsedRepeats == null || collapsedRepeats.size() > 4) {
            throw new IllegalArgumentException("Invalid sidebar view.");
        }
        filters.forEach((key, value) -> {
            if (key == null || key.length() > 30 || value == null || value.length() > 100) {
                throw new IllegalArgumentException("Invalid sidebar filter.");
            }
        });
        for (String key : collapsedCategories) {
            if (key == null || key.length() > 100) {
                throw new IllegalArgumentException("Invalid collapsed category.");
            }
        }
        for (String key : collapsedRepeats) {
            if (key == null || key.length() > 30) {
                throw new IllegalArgumentException("Invalid collapsed schedule.");
            }
        }
        this.search = search;
        this.filters = Collections.unmodifiableMap(new LinkedHashMap<>(filters));
        this.pinnedOnly = pinnedOnly;
        this.routines = routines;
        this.filtersExpanded = filtersExpanded;
        this.dashboardExpanded = dashboardExpanded;
        this.collapsedCategories = Collections.unmodifiableSet(new LinkedHashSet<>(collapsedCategories));
        this.collapsedRepeats = Collections.unmodifiableSet(new LinkedHashSet<>(collapsedRepeats));
    }

    public static SidebarViewState createDefault() {
        return new SidebarViewState("", Map.of(), false, false, false, false, Set.of(), Set.of());
    }

    public static SidebarViewState validated(SidebarViewState state) {
        return new SidebarViewState(state.search, state.filters, state.pinnedOnly, state.routines, state.filtersExpanded, state.dashboardExpanded, state.collapsedCategories, state.collapsedRepeats);
    }
}
