package com.grindstone.repository;

import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.ManualMilestone;
import com.grindstone.content.milestone.impl.ChecklistMilestone;
import com.grindstone.content.milestone.impl.CustomMilestone;
import java.util.HashSet;
import java.util.List;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class BulkMilestoneChanges {
    private BulkMilestoneChanges() {
    }

    public static void apply(GrindstoneProfile profile, List<Milestone> selected, BulkMilestoneAction action, MilestoneCategory category, RepeatSchedule schedule, long target, long deadlineAt, boolean reminder, long now) {
        if (selected.isEmpty() || selected.size() > 5000 || new HashSet<>(selected).size() != selected.size() || !profile.getGoals().containsAll(selected) || action == null || now <= 0) {
            throw new IllegalArgumentException("Select existing milestones before applying a bulk action.");
        }
        if (action == BulkMilestoneAction.CATEGORY && category == null) {
            throw new IllegalArgumentException("Choose a category.");
        }
        if (action == BulkMilestoneAction.DEADLINE) {
            new MilestoneMetadata().setDeadline(deadlineAt, reminder);
        }
        if (action == BulkMilestoneAction.SCHEDULE) {
            if (schedule == null || target < 1 || target > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Choose a repeat schedule and positive period target.");
            }
            for (int i = 0; i < selected.size(); i++) {
                Milestone goal = selected.get(i);
                if (!RecurringMilestone.supports(goal)) {
                    throw new IllegalArgumentException(goal.getTitle(null) + " does not support repeat schedules. No milestones were changed.");
                }
                Milestone matching = profile.findMatchingGoal(goal, schedule);
                if (matching != null && matching != goal) {
                    throw new IllegalArgumentException("A matching milestone already has that schedule. Choose another schedule.");
                }
                for (int j = 0; j < i; j++) {
                    if (GrindstoneProfile.sameSelection(goal, selected.get(j))) {
                        throw new IllegalArgumentException("Two selected milestones would have the same schedule. Choose a different schedule.");
                    }
                }
            }
        }
        for (Milestone goal : selected) {
            MilestoneMetadata saved = profile.getMetadata(goal);
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
                case DEADLINE:
                    saved.setDeadline(deadlineAt, reminder);
                    break;
                case SCHEDULE:
                    long periodTarget = goal instanceof ChecklistMilestone ? goal.getProgressMaximum() : target;
                    boolean changed = saved.getRecurring().getSchedule() != schedule || saved.getRecurring().getTarget() != periodTarget;
                    saved.getRecurring().configure(schedule, periodTarget, now);
                    if (changed && schedule != RepeatSchedule.NONE && goal instanceof ManualMilestone) {
                        ((ManualMilestone) goal).resetProgress();
                    }
                    if (schedule != RepeatSchedule.NONE && goal instanceof CustomMilestone) {
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
        profile.touch();
    }
}
