package com.grindstone.ui;

import com.grindstone.content.milestone.BaseSkillMilestone;
import com.grindstone.content.milestone.ManualMilestone;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.content.milestone.impl.LinkedMilestone;
import com.grindstone.content.milestone.impl.experience.SessionExperienceMilestone;
import com.grindstone.repository.RepeatSchedule;
import com.grindstone.repository.SidebarViewState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
final class MilestoneFilters {
    static final String ALL_CATEGORIES = "All";
    static final String ALL_STATUSES = "All statuses";
    static final String NOT_STARTED = "New";
    static final String IN_PROGRESS = "Active";
    static final String COMPLETE = "Done";
    static final String UNFINISHED = "Unfinished";
    static final String WAITING = "Waiting";
    static final String ALL_SCHEDULES = "All";
    static final String ONE_OFF = "One-off milestones";
    static final String ALL_TYPES = "All";
    static final String AUTOMATIC = "Automatic";
    static final String CUSTOM = "Custom";
    static final String LINKED = "Linked";
    static final String BASE = "Base stats";
    static final String SESSION = "Session XP";
    static final String DEADLINES = "With deadline";
    static final String PAST_DUE = "Past due";
    static final String SORT_NAME = "Name (A-Z)";
    static final String SORT_PROGRESS_DESC = "Progress (highest)";
    static final String SORT_PROGRESS_ASC = "Progress (lowest)";
    static final String SORT_STATUS = "Status";
    static final String SORT_NAME_DESC = "Name (Z-A)";
    static final String SORT_CLOSEST = "Closest unfinished";

    private MilestoneFilters() {
    }

    static List<MilestoneView> filter(List<MilestoneView> goals, SidebarViewState state, boolean hideCompleted) {
        return filter(goals, state, hideCompleted, state.getSearch());
    }

    static List<MilestoneView> filter(List<MilestoneView> goals, SidebarViewState state, boolean hideCompleted, String search) {
        List<MilestoneView> filtered = new ArrayList<>();
        String query = search.trim().toLowerCase(Locale.ROOT);
        String[] words = query.isEmpty() ? new String[0] : query.split("\\s+");
        String selectedCategory = state.getFilters().get("category");
        String selectedStatus = state.getFilters().get("status");
        String selectedType = state.getFilters().get("type");
        String selectedSchedule = state.getFilters().get("schedule");

        for (MilestoneView view : goals) {
            RepeatSchedule schedule = (RepeatSchedule) view.getComponent().getClientProperty("grindstone.repeat");
            if (isComplete(view.getStatus()) && schedule == RepeatSchedule.NONE && !COMPLETE.equals(selectedStatus)) {
                continue;
            }
            if (!matchesSearch(words, view.getSearchText())) {
                continue;
            }
            if (selectedCategory != null && !ALL_CATEGORIES.equals(selectedCategory) && !selectedCategory.equals(view.getCategory().getDisplayName())) {
                continue;
            }
            if (!matchesStatus(view.getStatus(), selectedStatus, hideCompleted, Boolean.TRUE.equals(view.getComponent().getClientProperty("grindstone.ready")))) {
                continue;
            }
            if (!matchesSchedule(schedule, selectedSchedule) || state.isPinnedOnly() && !Boolean.TRUE.equals(view.getComponent().getClientProperty("grindstone.pinned"))) {
                continue;
            }
            Object deadline = view.getComponent().getClientProperty("grindstone.deadline");
            if (!matchesType(view.getGoal(), view.getStatus(), selectedType, deadline instanceof Long ? (Long) deadline : 0, System.currentTimeMillis())) {
                continue;
            }
            filtered.add(view);
        }
        return filtered;
    }

    static boolean matchesSearch(String query, String title, String description, String category) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        return matchesSearch(query.trim().toLowerCase(Locale.ROOT).split("\\s+"), getSearchText(title, description, category));
    }

    static String getSearchText(String title, String description, String category) {
        return ((title == null ? "" : title) + " " + (description == null ? "" : description) + " " + (category == null ? "" : category)).toLowerCase(Locale.ROOT);
    }

    static boolean matchesSearch(String[] words, String text) {
        for (String word : words) {
            if (!text.contains(word)) {
                return false;
            }
        }
        return true;
    }

    static boolean matchesSchedule(RepeatSchedule schedule, String selected) {
        if (selected == null || ALL_SCHEDULES.equals(selected)) {
            return true;
        }
        RepeatSchedule actual = schedule == null ? RepeatSchedule.NONE : schedule;
        return ONE_OFF.equals(selected) ? actual == RepeatSchedule.NONE : actual != RepeatSchedule.NONE && actual.name().equalsIgnoreCase(selected);
    }

    static boolean matchesStatus(MilestoneStatus status, String selected, boolean hideCompleted, boolean ready) {
        if (WAITING.equals(selected)) {
            return !ready;
        }
        if (!ready && (NOT_STARTED.equals(selected) || IN_PROGRESS.equals(selected) || COMPLETE.equals(selected))) {
            return false;
        }
        return matchesStatus(status, selected, hideCompleted);
    }

    static boolean matchesType(Milestone goal, MilestoneStatus status, String selectedType, long deadline, long now) {
        if (CUSTOM.equals(selectedType)) {
            return goal instanceof ManualMilestone;
        }
        if (AUTOMATIC.equals(selectedType)) {
            return !(goal instanceof ManualMilestone) && !(goal instanceof LinkedMilestone);
        }
        if (LINKED.equals(selectedType)) {
            return goal instanceof LinkedMilestone;
        }
        if (BASE.equals(selectedType)) {
            return goal instanceof BaseSkillMilestone;
        }
        if (SESSION.equals(selectedType)) {
            return goal instanceof SessionExperienceMilestone;
        }
        if (DEADLINES.equals(selectedType)) {
            return deadline > 0;
        }
        if (PAST_DUE.equals(selectedType)) {
            return deadline > 0 && now >= deadline && !isComplete(status);
        }
        return true;
    }

    static boolean matchesStatus(MilestoneStatus status, String selectedStatus, boolean hideCompleted) {
        if (selectedStatus == null || ALL_STATUSES.equals(selectedStatus)) {
            return !hideCompleted || status != MilestoneStatus.COMPLETE;
        }
        if (UNFINISHED.equals(selectedStatus)) {
            return status != MilestoneStatus.COMPLETE;
        }
        String value = status == null ? "" : status.name();
        if (COMPLETE.equals(selectedStatus)) {
            return "COMPLETE".equalsIgnoreCase(value) || "COMPLETED".equalsIgnoreCase(value);
        }
        if (IN_PROGRESS.equals(selectedStatus)) {
            return "IN_PROGRESS".equalsIgnoreCase(value);
        }
        if (NOT_STARTED.equals(selectedStatus)) {
            return "NOT_STARTED".equalsIgnoreCase(value);
        }
        return true;
    }

    static void sort(List<MilestoneView> goals, String selectedSort) {
        if (SORT_NAME_DESC.equals(selectedSort)) {
            goals.sort(Comparator.comparing(MilestoneView::getTitle, String.CASE_INSENSITIVE_ORDER).reversed());
        } else if (SORT_CLOSEST.equals(selectedSort)) {
            goals.sort(Comparator.comparing((MilestoneView view) -> isComplete(view.getStatus())).thenComparing(Comparator.comparingDouble(MilestoneFilters::getProgressRatio).reversed()).thenComparing(MilestoneView::getTitle, String.CASE_INSENSITIVE_ORDER));
        } else if (SORT_PROGRESS_DESC.equals(selectedSort)) {
            goals.sort(Comparator.comparingDouble(MilestoneFilters::getProgressRatio).reversed().thenComparing(MilestoneView::getTitle, String.CASE_INSENSITIVE_ORDER));
        } else if (SORT_PROGRESS_ASC.equals(selectedSort)) {
            goals.sort(Comparator.comparingDouble(MilestoneFilters::getProgressRatio).thenComparing(MilestoneView::getTitle, String.CASE_INSENSITIVE_ORDER));
        } else if (SORT_STATUS.equals(selectedSort)) {
            goals.sort(Comparator.comparingInt((MilestoneView view) -> getStatusOrder(view.getStatus())).thenComparing(MilestoneView::getTitle, String.CASE_INSENSITIVE_ORDER));
        } else {
            goals.sort(Comparator.comparing(MilestoneView::getTitle, String.CASE_INSENSITIVE_ORDER));
        }
    }

    static double getProgressRatio(MilestoneView view) {
        if (!view.hasProgress() || view.getProgressMaximum() <= 0) {
            return isComplete(view.getStatus()) ? 1.0 : 0.0;
        }
        long current = Math.max(0, Math.min(view.getProgressCurrent(), view.getProgressMaximum()));
        return current / (double) view.getProgressMaximum();
    }

    static boolean isComplete(MilestoneStatus status) {
        return status != null && ("COMPLETE".equalsIgnoreCase(status.name()) || "COMPLETED".equalsIgnoreCase(status.name()));
    }

    private static int getStatusOrder(MilestoneStatus status) {
        if (status == MilestoneStatus.IN_PROGRESS) {
            return 0;
        }
        if (status == MilestoneStatus.NOT_STARTED) {
            return 1;
        }
        return 2;
    }
}
