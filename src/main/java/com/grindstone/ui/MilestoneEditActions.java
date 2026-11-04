package com.grindstone.ui;

import com.grindstone.content.milestone.ManualMilestone;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.impl.ChecklistMilestone;
import com.grindstone.content.milestone.impl.CustomMilestone;
import com.grindstone.content.milestone.impl.LinkedMilestone;
import com.grindstone.content.milestone.impl.collectionlog.CollectionLogItemMilestone;
import com.grindstone.content.milestone.impl.collectionlog.CollectionLogTotalMilestone;
import com.grindstone.repository.GrindstoneProfile;
import com.grindstone.repository.MilestoneMetadata;
import com.grindstone.repository.RecurringMilestone;
import com.grindstone.repository.RepeatSchedule;
import com.grindstone.ui.editor.CompletionMilestoneChoices;
import com.grindstone.ui.editor.CustomMilestoneForm;
import com.grindstone.ui.editor.DeadlineForm;
import com.grindstone.ui.editor.LinkedMilestoneForm;
import com.grindstone.ui.editor.MilestoneCreationDialog;
import com.grindstone.ui.editor.MilestoneEditorPanel;
import com.grindstone.ui.editor.MilestoneType;
import com.grindstone.ui.editor.RepeatMilestoneForm;
import com.grindstone.ui.item.ItemSearchService;
import com.grindstone.ui.util.DialogPlacement;
import com.grindstone.ui.util.UiTypography;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongUnaryOperator;
import java.util.function.Supplier;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import lombok.RequiredArgsConstructor;
import net.runelite.client.game.ItemManager;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
@RequiredArgsConstructor
public final class MilestoneEditActions {
    private final MilestoneActionSupport support;
    private final ItemManager itemManager;
    private final ItemSearchService itemSearchService;

    public void editRepeat(GrindstoneProfile active, Milestone goal, RepeatSchedule schedule, long target) {
        if (active == null || support.getProfile() != active) {
            return;
        }
        RepeatMilestoneForm form = new RepeatMilestoneForm();
        form.load(schedule, target);
        if (goal instanceof ChecklistMilestone) {
            form.setFixedTarget(goal.getProgressMaximum());
        }
        while (JOptionPane.showConfirmDialog(DialogPlacement.getOwner(support.getOwner()), form, "Repeat schedule", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                form.commit();
                RepeatSchedule selected = form.getSchedule();
                long selectedTarget = form.getTarget();
                if ((schedule != selected || target != selectedTarget) && JOptionPane.showConfirmDialog(DialogPlacement.getOwner(support.getOwner()), "Repeat: " + schedule + " → " + selected + "\nPeriod target: " + target + " → " + selectedTarget + "\nThe current observed period will be archived.", "Preview schedule change", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                support.getClientThread().invokeLater(() -> {
                    if (support.getProfile() == active && active.getGoals().contains(goal)) {
                        Milestone collision = active.findMatchingGoal(goal, selected);
                        if (collision != null && collision != goal) {
                            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), "That tracker already has a milestone with this schedule."));
                            return;
                        }
                        support.configureRepeat(active, goal, selected, selectedTarget);
                        active.touch();
                        support.render();
                    }
                });
                return;
            } catch (ParseException exception) {
                JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), "Enter a valid period target.");
            }
        }
    }

    public void restartPeriod(GrindstoneProfile active, Milestone goal) {
        if (support.getProfile() != active || JOptionPane.showConfirmDialog(DialogPlacement.getOwner(support.getOwner()), "Archive the current observed period and capture a fresh baseline?\nCustom progress resets to zero. Your game progress is unaffected.", "Restart current period", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        support.getClientThread().invokeLater(() -> {
            if (support.getProfile() != active || !active.getGoals().contains(goal)) {
                return;
            }
            RecurringMilestone recurring = active.getMetadata(goal).getRecurring();
            if (!recurring.isActive()) {
                return;
            }
            recurring.restart(System.currentTimeMillis());
            if (goal instanceof ManualMilestone) {
                ((ManualMilestone) goal).resetProgress();
            }
            active.touch();
            support.render();
        });
    }

    public void editMilestone(GrindstoneProfile active, Milestone goal, String title) {
        if (support.getProfile() != active) {
            return;
        }
        if (goal instanceof LinkedMilestone) {
            editLinkedMilestone(active, (LinkedMilestone) goal);
            return;
        }
        if (goal instanceof CustomMilestone) {
            editCustomMilestone(active, (CustomMilestone) goal, title);
            return;
        }
        support.getClientThread().invokeLater(() -> {
            if (support.getProfile() != active || !active.getGoals().contains(goal)) {
                return;
            }
            if (isCollectionLogMilestone(goal) && !(goal instanceof CollectionLogTotalMilestone) && !active.getCollectionLogHistory().hasSnapshot()) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), "Load a full collection-log snapshot before editing collection milestones. Open your collection log to capture it automatically."));
                return;
            }
            Map<MilestoneType, Map<String, Supplier<Milestone>>> choices = CompletionMilestoneChoices.getForEditing(support.getClient(), active.getCollectionLogHistory());
            RepeatSchedule schedule = active.getMetadata(goal).getRecurring().getSchedule();
            long periodTarget = active.getMetadata(goal).getRecurring().getTarget();
            SwingUtilities.invokeLater(() -> {
                if (support.getProfile() != active) {
                    return;
                }
                MilestoneEditorPanel form = new MilestoneEditorPanel(itemManager, itemSearchService, choices, true);
                form.setCollectionSnapshotStatus(active.getCollectionLogHistory().hasSnapshot(), active.getCollectionLogHistory().getKnownUnlockCount(), active.getCollectionLogHistory().getSnapshotAt());
                try {
                    form.loadMilestone(goal);
                } catch (IllegalArgumentException exception) {
                    form.stopSearch();
                    JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), exception.getMessage());
                    return;
                }
                form.loadRepeat(schedule, periodTarget);
                Milestone replacement = MilestoneCreationDialog.showDialog(support.getOwner(), form, "Edit " + title, "Save changes");
                if (replacement == null) {
                    return;
                }
                RepeatSchedule selectedSchedule = form.getRepeatSchedule();
                long selectedTarget = form.getRepeatTarget();
                if (!confirmEdit(title, goal, replacement, schedule, selectedSchedule, periodTarget, selectedTarget)) {
                    return;
                }
                support.getClientThread().invokeLater(() -> {
                    if (support.getProfile() != active) {
                        return;
                    }
                    try {
                        if (active.replaceGoal(goal, replacement, selectedSchedule)) {
                            replacement.refresh(support.getClient());
                            support.configureRepeat(active, replacement, selectedSchedule, selectedTarget);
                            support.render();
                        }
                    } catch (IllegalArgumentException exception) {
                        SwingUtilities.invokeLater(() -> {
                            if (support.getProfile() == active) {
                                JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), exception.getMessage());
                            }
                        });
                    }
                });
            });
        });
    }

    public void editLinkedMilestone(GrindstoneProfile active, LinkedMilestone original) {
        if (active == null || support.getProfile() != active) {
            return;
        }
        support.getClientThread().invokeLater(() -> {
            if (support.getProfile() != active || original != null && !active.getGoals().contains(original)) {
                return;
            }
            List<LinkedMilestoneForm.Choice> sources = new ArrayList<>();
            for (Milestone goal : active.getGoals()) {
                if (!(goal instanceof LinkedMilestone)) {
                    MilestoneMetadata metadata = active.getMetadata(goal);
                    boolean recurring = metadata.getRecurring().isActive();
                    sources.add(new LinkedMilestoneForm.Choice(metadata.getId(), support.getTitle(goal) + (recurring ? " · " + metadata.getRecurring().getSchedule() : ""), recurring ? metadata.getRecurring().getTarget() : goal.getProgressMaximum(), goal instanceof CollectionLogItemMilestone, recurring || !metadata.getRecurring().getHistory().isEmpty()));
                }
            }
            SwingUtilities.invokeLater(() -> {
                if (support.getProfile() != active) {
                    return;
                }
                LinkedMilestoneForm form = new LinkedMilestoneForm(sources, original);
                JScrollPane scroll = new JScrollPane(form);
                scroll.setPreferredSize(new Dimension(520, 570));
                while (JOptionPane.showConfirmDialog(DialogPlacement.getOwner(support.getOwner()), scroll, original == null ? "Add linked milestone" : "Edit linked milestone", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                    try {
                        LinkedMilestone candidate = form.read();
                        if (JOptionPane.showConfirmDialog(DialogPlacement.getOwner(support.getOwner()), support.createPreview(candidate.getTitle(null) + "\n" + candidate.getMode() + " · " + candidate.getSources().size() + " source(s)\nTargets: " + Arrays.toString(candidate.getTargets()) + "\nMissing sources remain unavailable; this never performs game actions."), "Preview linked milestone", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                            continue;
                        }
                        support.getClientThread().invokeLater(() -> {
                            if (support.getProfile() != active || original != null && !active.getGoals().contains(original)) {
                                return;
                            }
                            if (original == null) {
                                active.add(candidate);
                            } else {
                                active.replaceGoal(original, candidate);
                            }
                            active.updateLinkedGoals(support.hasClientProfile(active));
                            support.render();
                        });
                        return;
                    } catch (IllegalArgumentException | ParseException failure) {
                        JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), failure.getMessage(), "Check linked milestone", JOptionPane.WARNING_MESSAGE);
                    }
                }
            });
        });
    }

    public void editDeadline(GrindstoneProfile active, Milestone goal) {
        if (active == null || support.getProfile() != active) {
            return;
        }
        support.getClientThread().invokeLater(() -> {
            if (support.getProfile() != active || !active.getGoals().contains(goal)) {
                return;
            }
            MilestoneMetadata metadata = active.getMetadata(goal);
            long originalDeadline = metadata.getDeadlineAt();
            boolean originalReminder = metadata.isDeadlineReminder();
            SwingUtilities.invokeLater(() -> {
                DeadlineForm form = new DeadlineForm(originalDeadline, originalReminder);
                while (support.getProfile() == active && JOptionPane.showConfirmDialog(DialogPlacement.getOwner(support.getOwner()), form, "Personal deadline", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                    try {
                        long deadlineAt = form.getDeadlineAt();
                        boolean reminder = form.isReminderEnabled();
                        support.getClientThread().invokeLater(() -> {
                            if (support.getProfile() == active && active.getGoals().contains(goal) && active.getMetadata(goal).getDeadlineAt() == originalDeadline && active.getMetadata(goal).isDeadlineReminder() == originalReminder) {
                                active.getMetadata(goal).setDeadline(deadlineAt, reminder);
                                active.touch();
                                support.render();
                            }
                        });
                        return;
                    } catch (IllegalArgumentException failure) {
                        JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), failure.getMessage());
                    }
                }
            });
        });
    }

    public void editCustomMilestone(GrindstoneProfile active, CustomMilestone goal, String title) {
        RepeatSchedule openedSchedule = active.getMetadata(goal).getRecurring().getSchedule();
        long openedPeriod = openedSchedule.getPeriodStart(System.currentTimeMillis());
        CustomMilestoneForm form = new CustomMilestoneForm();
        form.load(goal);
        RepeatMilestoneForm repeat = new RepeatMilestoneForm();
        repeat.load(active.getMetadata(goal).getRecurring().getSchedule(), active.getMetadata(goal).getRecurring().getTarget());
        form.setRecurring(repeat.getSchedule() != RepeatSchedule.NONE);
        repeat.addScheduleListener(() -> form.setRecurring(repeat.getSchedule() != RepeatSchedule.NONE));
        JPanel editor = new JPanel(new BorderLayout(0, 8));
        editor.add(form, BorderLayout.CENTER);
        editor.add(repeat, BorderLayout.SOUTH);
        while (JOptionPane.showConfirmDialog(DialogPlacement.getOwner(support.getOwner()), editor, "Edit " + title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                repeat.commit();
                RepeatSchedule schedule = repeat.getSchedule();
                long periodTarget = repeat.getTarget();
                CustomMilestone draft = form.read(schedule == RepeatSchedule.NONE ? 0 : periodTarget);
                if (!confirmEdit(title, goal, draft, active.getMetadata(goal).getRecurring().getSchedule(), schedule, active.getMetadata(goal).getRecurring().getTarget(), periodTarget)) {
                    return;
                }
                support.getClientThread().invokeLater(() -> {
                    if (support.getProfile() == active && active.getGoals().contains(goal)) {
                        if (openedSchedule != RepeatSchedule.NONE && openedSchedule.getPeriodStart(System.currentTimeMillis()) != openedPeriod) {
                            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), "The period reset while you were editing. Reopen the editor to avoid applying the previous period's progress."));
                            support.render();
                            return;
                        }
                        Milestone collision = active.findMatchingGoal(goal, schedule);
                        if (collision != null && collision != goal) {
                            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), "That personal milestone already has this schedule."));
                            return;
                        }
                        goal.update(draft.getTitle(null), draft.getDescription(), draft.getCategory(), draft.getProgressCurrent(), draft.getProgressMaximum());
                        support.configureRepeat(active, goal, schedule, periodTarget);
                        active.touch();
                        support.render();
                    }
                });
                return;
            } catch (ParseException | IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), exception instanceof IllegalArgumentException ? exception.getMessage() : "Enter valid progress and target numbers.", "Check milestone details", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private boolean confirmEdit(String title, Milestone original, Milestone replacement, RepeatSchedule before, RepeatSchedule after, long previousTarget, long nextTarget) {
        String summary = title + " → " + replacement.getTitle(null) + "\nTarget: " + String.format(Locale.ROOT, "%,d → %,d", original.getProgressMaximum(), replacement.getProgressMaximum()) + "\nRepeat: " + before + " → " + after + "\nPeriod target: " + String.format(Locale.ROOT, "%,d → %,d", previousTarget, nextTarget) + "\n\nChanged subjects retain old observations in account history.\nChanged repeat settings archive the current observed period.";
        JTextArea preview = new JTextArea(summary, 10, 38);
        preview.setEditable(false);
        preview.setLineWrap(true);
        preview.setWrapStyleWord(true);
        preview.setFont(UiTypography.BODY);
        return JOptionPane.showConfirmDialog(DialogPlacement.getOwner(support.getOwner()), new JScrollPane(preview), "Preview milestone changes", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    public void deleteMilestone(GrindstoneProfile active, Milestone goal, String title) {
        if (support.getProfile() != active || JOptionPane.showConfirmDialog(DialogPlacement.getOwner(support.getOwner()), "Remove " + title + " from your milestones?\nYour game progress is unaffected.", "Delete milestone", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        support.getClientThread().invokeLater(() -> {
            if (support.getProfile() == active && active.remove(goal)) {
                support.render();
            }
        });
    }

    public void promptManualProgress(GrindstoneProfile active, CustomMilestone goal, long current, long target) {
        RepeatSchedule schedule = active.getMetadata(goal).getRecurring().getSchedule();
        long window = schedule.getPeriodStart(System.currentTimeMillis());
        JSpinner value = new JSpinner(new SpinnerNumberModel(current, 0L, target, 1L));
        value.setEditor(new JSpinner.NumberEditor(value, "#,##0"));
        while (JOptionPane.showConfirmDialog(DialogPlacement.getOwner(support.getOwner()), value, "Set progress (0–" + String.format(Locale.ROOT, "%,d", target) + ")", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                value.commitEdit();
                long requested = ((Number) value.getValue()).longValue();
                changeManualProgress(active, goal, previous -> {
                    if (schedule != RepeatSchedule.NONE && schedule.getPeriodStart(System.currentTimeMillis()) != window) {
                        throw new IllegalStateException("The period reset while this dialog was open. Reopen Set progress for the new period.");
                    }
                    return requested;
                });
                return;
            } catch (ParseException exception) {
                JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), "Enter a whole number within the target.");
            }
        }
    }

    public void changeManualProgress(GrindstoneProfile active, CustomMilestone goal, LongUnaryOperator change) {
        support.getClientThread().invokeLater(() -> {
            if (support.getProfile() != active || !active.getGoals().contains(goal)) {
                return;
            }
            long now = System.currentTimeMillis();
            RecurringMilestone recurring = active.getMetadata(goal).getRecurring();
            if (recurring.isActive()) {
                recurring.observe(goal, now);
            }
            long requested;
            try {
                requested = change.applyAsLong(goal.getProgressCurrent());
            } catch (IllegalStateException exception) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), exception.getMessage()));
                support.render();
                return;
            }
            goal.update(goal.getTitle(null), goal.getDescription(), goal.getCategory(), Math.min(requested, goal.getProgressMaximum()), goal.getProgressMaximum());
            if (recurring.isActive()) {
                recurring.observe(goal, now);
            } else {
                active.getMetadata(goal).observe(goal, goal.getTitle(null), now);
            }
            active.touch();
            support.render();
        });
    }

    private boolean isCollectionLogMilestone(Milestone goal) {
        return goal.getCategory() == MilestoneCategory.COLLECTION_LOG;
    }
}
