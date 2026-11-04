package com.grindstone.ui;

import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.repository.BulkMilestoneAction;
import com.grindstone.repository.BulkMilestoneChanges;
import com.grindstone.repository.GrindstoneProfile;
import com.grindstone.repository.MilestoneMetadata;
import com.grindstone.repository.RepeatSchedule;
import com.grindstone.ui.editor.BulkMilestoneForm;
import com.grindstone.ui.util.DialogPlacement;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.RequiredArgsConstructor;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
@RequiredArgsConstructor
public final class BulkMilestoneActions {
    private final MilestoneActionSupport support;

    public void showBulkManager() {
        GrindstoneProfile active = support.getProfile();
        if (active == null) {
            return;
        }
        support.getClientThread().invokeLater(() -> {
            if (support.getProfile() != active) {
                return;
            }
            long revision = active.getRevision();
            List<BulkMilestoneForm.Row> rows = new ArrayList<>();
            for (Milestone goal : active.getGoals()) {
                MilestoneMetadata metadata = active.getMetadata(goal);
                rows.add(new BulkMilestoneForm.Row(goal, support.getTitle(goal), support.getCategory(goal), metadata.getRecurring().getSchedule()));
            }
            SwingUtilities.invokeLater(() -> {
                if (support.getProfile() != active) {
                    return;
                }
                BulkMilestoneForm form = new BulkMilestoneForm(rows);
                while (support.getProfile() == active && JOptionPane.showConfirmDialog(DialogPlacement.getOwner(support.getOwner()), form, "Manage milestones", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                    try {
                        List<Milestone> selected = form.getSelectedMilestones();
                        if (selected.isEmpty()) {
                            throw new IllegalArgumentException("Tick at least one milestone first.");
                        }
                        BulkMilestoneAction action = form.getAction();
                        MilestoneCategory category = form.getCategory();
                        RepeatSchedule schedule = form.getSchedule();
                        long target = action == BulkMilestoneAction.SCHEDULE ? form.getPeriodTarget() : 1;
                        long deadlineAt = form.getDeadlineAt();
                        boolean reminder = form.isReminderEnabled();
                        String details = action == BulkMilestoneAction.CATEGORY ? "\nCategory: " + category.getDisplayName() : action == BulkMilestoneAction.SCHEDULE ? "\nRepeat: " + schedule + " · target " + target + "\nChecklists require all their steps. Active manual progress resets when its period settings change." : action == BulkMilestoneAction.DEADLINE ? "\n" + (deadlineAt == 0 ? "Remove deadlines" : "Due at " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'", Locale.ROOT).withZone(ZoneOffset.UTC).format(Instant.ofEpochMilli(deadlineAt))) : "";
                        if (JOptionPane.showConfirmDialog(DialogPlacement.getOwner(support.getOwner()), action + " · " + selected.size() + " milestones?" + details + (action == BulkMilestoneAction.DELETE ? "\nDeleting source milestones can make linked milestones unavailable.\nObserved history is archived." : "") + "\nThe action is validated before changes. One-step undo is available until a later edit or relevant reset.", "Preview bulk action", JOptionPane.OK_CANCEL_OPTION, action == BulkMilestoneAction.DELETE ? JOptionPane.WARNING_MESSAGE : JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION) {
                            continue;
                        }
                        support.getClientThread().invokeLater(() -> {
                            if (support.getProfile() != active) {
                                return;
                            }
                            try {
                                if (active.getRevision() != revision) {
                                    throw new IllegalArgumentException("Milestones changed while this manager was open. No changes applied; reopen it to review the current milestones.");
                                }
                                BulkMilestoneChanges.Change change = BulkMilestoneChanges.apply(active, selected, action, category, schedule, target, deadlineAt, reminder, System.currentTimeMillis());
                                support.recordUndo(active, "bulk " + action, change.undo, change.allowed);
                                support.render();
                            } catch (IllegalArgumentException failure) {
                                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), failure.getMessage(), "Bulk action not applied", JOptionPane.WARNING_MESSAGE));
                            }
                        });
                        return;
                    } catch (IllegalArgumentException | ParseException failure) {
                        JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), failure.getMessage(), "Review selections", JOptionPane.WARNING_MESSAGE);
                    }
                }
            });
        });
    }

    public void clearAllMilestones() {
        GrindstoneProfile active = support.getProfile();
        if (active == null) {
            return;
        }
        support.getClientThread().invokeLater(() -> {
            if (support.getProfile() != active) {
                return;
            }
            List<Milestone> selected = new ArrayList<>(active.getGoals());
            long revision = active.getRevision();
            if (selected.isEmpty()) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), "There are no milestones to clear."));
                return;
            }
            SwingUtilities.invokeLater(() -> {
                if (support.getProfile() != active || JOptionPane.showConfirmDialog(DialogPlacement.getOwner(support.getOwner()), "Remove all " + selected.size() + " milestones, including hidden milestones?\nRecorded history is retained within the 500-milestone archive limit.\nYou can undo this until another edit or a relevant period reset.", "Clear all milestones", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
                    return;
                }
                support.getClientThread().invokeLater(() -> {
                    if (support.getProfile() != active) {
                        return;
                    }
                    if (active.getRevision() != revision) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), "Milestones changed while confirmation was open. Nothing was cleared; try again."));
                        return;
                    }
                    BulkMilestoneChanges.Change change = BulkMilestoneChanges.apply(active, selected, BulkMilestoneAction.DELETE, null, RepeatSchedule.NONE, 1, 0, false, System.currentTimeMillis());
                    support.recordUndo(active, "clear all milestones", change.undo, change.allowed);
                    support.render();
                });
            });
        });
    }
}
