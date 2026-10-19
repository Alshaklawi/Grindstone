package com.grinder.repository;

import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.ManualGoal;
import com.grinder.content.goal.impl.ChecklistGoal;
import com.grinder.content.goal.impl.CustomGoal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class BulkGoalChanges {
    private BulkGoalChanges() {
    }

    public static Change apply(GrinderProfile profile, List<Goal> selected, BulkGoalAction action, GoalCategory category, RepeatSchedule schedule, long target, long deadlineAt, boolean reminder, long now) {
        if (selected.isEmpty() || selected.size() > 5000 || new HashSet<>(selected).size() != selected.size() || !profile.getGoals().containsAll(selected) || action == null || now <= 0) {
            throw new IllegalArgumentException("Select existing goals before applying a bulk action.");
        }
        if (action == BulkGoalAction.CATEGORY && category == null) {
            throw new IllegalArgumentException("Choose a category.");
        }
        if (action == BulkGoalAction.DEADLINE) {
            new GoalMetadata().setDeadline(deadlineAt, reminder);
        }
        if (action == BulkGoalAction.SCHEDULE) {
            if (schedule == null || target < 1 || target > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Choose a repeat schedule and positive period target.");
            }
            for (int i = 0; i < selected.size(); i++) {
                Goal goal = selected.get(i);
                if (!RecurringGoal.supports(goal)) {
                    throw new IllegalArgumentException(goal.getTitle(null) + " does not support repeat schedules. No goals were changed.");
                }
                Goal matching = profile.findMatchingGoal(goal, schedule);
                if (matching != null && matching != goal) {
                    throw new IllegalArgumentException("A matching tracker already has that schedule. No goals were changed.");
                }
                for (int j = 0; j < i; j++) {
                    if (GrinderProfile.sameSelection(goal, selected.get(j))) {
                        throw new IllegalArgumentException("Two selected goals would share the same subject and schedule. No goals were changed.");
                    }
                }
            }
        }
        List<Goal> originalOrder = new ArrayList<>(profile.getGoals());
        List<GrinderProfile.ArchivedHistory> originalArchives = new ArrayList<>(profile.archivedHistory());
        Map<Goal, GoalMetadata> metadata = new LinkedHashMap<>();
        Map<Goal, Consumer<Goal>> restore = new LinkedHashMap<>();
        Map<RepeatSchedule, Long> periods = new LinkedHashMap<>();
        for (Goal goal : selected) {
            GoalMetadata saved = profile.metadata(goal).copy();
            metadata.put(goal, saved);
            restore.put(goal, profile.snapshotEdit(goal));
            RepeatSchedule previous = saved.recurring().getSchedule();
            if (previous != RepeatSchedule.NONE) {
                periods.put(previous, previous.start(now));
            }
        }
        for (Goal goal : selected) {
            GoalMetadata saved = profile.metadata(goal);
            switch (action) {
                case PIN:
                    saved.setPinned(true);
                    break;
                case UNPIN:
                    saved.setPinned(false);
                    break;
                case CATEGORY:
                    saved.setDisplayCategory(category);
                    break;
                case RESET_CATEGORY:
                    saved.setDisplayCategory(null);
                    break;
                case EXAMPLE:
                    saved.setSample(true);
                    break;
                case PERSONAL:
                    saved.setSample(false);
                    break;
                case DEADLINE:
                    saved.setDeadline(deadlineAt, reminder);
                    break;
                case SCHEDULE:
                    long periodTarget = goal instanceof ChecklistGoal ? goal.getProgressMaximum() : target;
                    boolean changed = saved.recurring().getSchedule() != schedule || saved.recurring().getTarget() != periodTarget;
                    saved.recurring().configure(schedule, periodTarget, now);
                    if (changed && schedule != RepeatSchedule.NONE && goal instanceof ManualGoal) {
                        ((ManualGoal) goal).resetProgress();
                    }
                    if (schedule != RepeatSchedule.NONE && goal instanceof CustomGoal) {
                        goal.setTarget(periodTarget);
                    }
                    break;
                case DELETE:
                    profile.remove(goal);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported bulk action.");
            }
        }
        for (Goal goal : selected) {
            RepeatSchedule current = profile.metadata(goal).recurring().getSchedule();
            if (current != RepeatSchedule.NONE) {
                periods.put(current, current.start(now));
            }
        }
        profile.touch();
        BooleanSupplier allowed = () -> periods.entrySet().stream().allMatch(entry -> entry.getKey().start(System.currentTimeMillis()) == entry.getValue());
        Runnable undo = () -> {
            if (!allowed.getAsBoolean()) {
                throw new IllegalStateException("A recurring period reset; this bulk edit can no longer be undone.");
            }
            if (action == BulkGoalAction.DELETE) {
                for (Goal goal : selected) {
                    profile.add(goal, metadata.get(goal));
                }
                profile.replace(originalOrder);
                profile.restoreArchiveSnapshot(originalArchives);
            } else {
                restore.forEach((goal, operation) -> operation.accept(goal));
            }
            profile.touch();
        };
        return new Change(undo, allowed);
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 10/19/2026
     */
    public static final class Change {
        public final Runnable undo;
        public final BooleanSupplier allowed;

        private Change(Runnable undo, BooleanSupplier allowed) {
            this.undo = undo;
            this.allowed = allowed;
        }
    }
}
