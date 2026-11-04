package com.grindstone.ui;

import com.grindstone.content.collectionlog.CollectionLogPages;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.MilestonePreset;
import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.content.milestone.impl.collectionlog.CollectionLogEntryMilestone;
import com.grindstone.content.milestone.impl.collectionlog.CollectionLogTotalMilestone;
import com.grindstone.repository.GrindstoneProfile;
import com.grindstone.repository.GrindstoneRepository;
import com.grindstone.repository.MilestoneMetadata;
import com.grindstone.repository.RepeatSchedule;
import com.grindstone.ui.editor.CompletionMilestoneChoices;
import com.grindstone.ui.editor.MilestoneCreationDialog;
import com.grindstone.ui.editor.MilestoneEditorPanel;
import com.grindstone.ui.editor.MilestoneType;
import com.grindstone.ui.editor.RoutinePresetForm;
import com.grindstone.ui.item.ItemSearchService;
import com.grindstone.ui.util.DialogPlacement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.RequiredArgsConstructor;
import net.runelite.api.GameState;
import net.runelite.client.game.ItemManager;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
@RequiredArgsConstructor
public final class MilestoneCreationActions {
    private final MilestoneActionSupport support;
    private final ItemManager itemManager;
    private final ItemSearchService itemSearchService;
    private final Consumer<MilestoneCategory> addedCategory;

    private boolean isCompletedQuestOrDiary(Milestone goal) {
        if (goal.getCategory() != MilestoneCategory.QUEST && goal.getCategory() != MilestoneCategory.ACHIEVEMENT_DIARY) {
            return false;
        }
        goal.refresh(support.getClient());
        return goal.getStatus() == MilestoneStatus.COMPLETE;
    }

    public void addPreset(MilestonePreset preset) {
        GrindstoneProfile active = support.getProfile();
        if (active == null) {
            return;
        }
        support.getClientThread().invokeLater(() -> {
            if (support.getProfile() != active) {
                return;
            }
            List<Milestone> goals = preset.createGoals();
            goals.removeIf(this::isCompletedQuestOrDiary);
            String summary = additionSummary(active, goals, goal -> RepeatSchedule.NONE);
            SwingUtilities.invokeLater(() -> {
                if (support.getProfile() != active || JOptionPane.showConfirmDialog(DialogPlacement.getOwner(support.getOwner()), support.createPreview(summary), "Preview " + preset, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                support.getClientThread().invokeLater(() -> {
                    if (support.getProfile() != active) {
                        return;
                    }
                    for (Milestone goal : goals) {
                        if (!isCompletedQuestOrDiary(goal) && active.add(goal)) {
                            goal.refresh(support.getClient());
                        }
                    }
                    support.render();
                });
            });
        });
    }

    private String additionSummary(GrindstoneProfile active, List<Milestone> candidates, Function<Milestone, RepeatSchedule> schedule) {
        StringBuilder rows = new StringBuilder();
        int missing = 0;
        for (Milestone goal : candidates) {
            RepeatSchedule repeat = schedule.apply(goal);
            boolean exists = active.findMatchingGoal(goal, repeat) != null;
            if (!exists) {
                missing++;
            }
            rows.append(exists ? "Keep existing · " : "Add · ").append(goal.getTitle(null)).append(" · ").append(repeat);
            if (goal.getProgressMaximum() > 0) {
                rows.append(" · target ").append(String.format(Locale.ROOT, "%,d", goal.getProgressMaximum()));
            }
            rows.append('\n');
        }
        return missing + " new milestones · " + (candidates.size() - missing) + " existing matches kept\nExisting targets and schedules are never overwritten.\n\n" + rows;
    }

    public void addAllCollectionPages() {
        GrindstoneProfile active = support.getProfile();
        if (active == null) {
            return;
        }
        support.getClientThread().invokeLater(() -> {
            if (support.getProfile() != active) {
                return;
            }
            if (!active.getCollectionLogHistory().hasSnapshot()) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), "First open your in-game collection log to load a full snapshot. Then add collection-log milestones once the snapshot is complete."));
                return;
            }
            Map<Integer, String> pages = CollectionLogPages.discover(support.getClient());
            SwingUtilities.invokeLater(() -> {
                if (support.getProfile() != active) {
                    return;
                }
                if (pages.isEmpty()) {
                    JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), "Collection-log metadata is not ready. Log in and try again.");
                    return;
                }
                if (JOptionPane.showConfirmDialog(DialogPlacement.getOwner(support.getOwner()), "Add missing milestones from all " + pages.size() + " collection-log pages? Existing milestones are kept.", "Add collection-log pages", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                support.getClientThread().invokeLater(() -> {
                    if (support.getProfile() != active) {
                        return;
                    }
                    pages.forEach((id, name) -> active.add(new CollectionLogEntryMilestone(itemManager, id, name)));
                    support.render();
                });
            });
        });
    }

    public void addRoutine(boolean weekly) {
        GrindstoneProfile active = support.getProfile();
        if (active == null) {
            return;
        }
        RoutinePresetForm form = new RoutinePresetForm(weekly);
        while (JOptionPane.showConfirmDialog(DialogPlacement.getOwner(support.getOwner()), form, "Preview recurring routine", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                List<Milestone> selected = form.getSelectedMilestones();
                Map<Milestone, MilestoneMetadata> metadata = new IdentityHashMap<>();
                for (Milestone goal : selected) {
                    metadata.put(goal, form.getMetadata(goal, System.currentTimeMillis()));
                }
                support.getClientThread().invokeLater(() -> {
                    if (support.getProfile() != active) {
                        return;
                    }
                    List<Milestone> before = new ArrayList<>(active.getGoals());
                    for (Milestone goal : selected) {
                        if (active.add(goal, metadata.get(goal))) {
                            goal.refresh(support.getClient());
                        }
                    }
                    support.render();
                    int count = active.getGoals().size() - before.size();
                    SwingUtilities.invokeLater(() -> {
                        if (support.getProfile() == active) {
                            JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), count + " routines added. Existing schedule matches were skipped.");
                        }
                    });
                });
                return;
            } catch (ParseException exception) {
                JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), "Enter valid targets for the selected milestones.");
            }
        }
    }

    public void showAddMilestoneDialog() {
        showAddMilestoneDialog(null);
    }

    public void showAddMilestoneDialog(Milestone initialGoal) {
        GrindstoneProfile active = support.getProfile();
        if (active == null) {
            JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), "Log in to add milestones to your player profile.");
            return;
        }
        support.getClientThread().invokeLater(() -> {
            if (support.getProfile() != active || support.getClient().getGameState() != GameState.LOGGED_IN) {
                return;
            }
            Map<MilestoneType, Map<String, Supplier<Milestone>>> choices =
            CompletionMilestoneChoices.getAvailable(support.getClient(), active.getCollectionLogHistory());
            Map<Milestone, RepeatSchedule> existing = new LinkedHashMap<>();
            for (Milestone goal : active.getGoals()) {
                existing.put(goal, active.getMetadata(goal).getRecurring().getSchedule());
            }
            Set<Integer> obtained = new LinkedHashSet<>(active.getCollectionLogHistory().getSnapshot().keySet());
            SwingUtilities.invokeLater(() -> {
                if (support.getProfile() == active) {
                    showAddMilestoneDialog(active, choices, existing, initialGoal, obtained);
                }
            });
        });
    }

    private void showAddMilestoneDialog(GrindstoneProfile active, Map<MilestoneType, Map<String, Supplier<Milestone>>> choices, Map<Milestone, RepeatSchedule> existingGoalsSnapshot, Milestone initialGoal, Set<Integer> obtained) {
        MilestoneEditorPanel form = new MilestoneEditorPanel(itemManager, itemSearchService, choices);
        form.setExistingMilestones(existingGoalsSnapshot);
        form.setCollectionSnapshotStatus(active.getCollectionLogHistory().hasSnapshot(), active.getCollectionLogHistory().getKnownUnlockCount(), active.getCollectionLogHistory().getSnapshotAt());
        form.hideObtainedCollectionItems(obtained);
        if (initialGoal != null) {
            try {
                form.loadMilestone(initialGoal);
            } catch (IllegalArgumentException exception) {
                form.stopSearch();
                JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), exception.getMessage(), "Add milestone", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        Milestone candidate = MilestoneCreationDialog.showDialog(support.getOwner(), form);
        if (candidate == null) {
            return;
        }
        RepeatSchedule schedule = form.getRepeatSchedule();
        long periodTarget = form.getRepeatTarget();
        support.getClientThread().invokeLater(() -> {
            if (support.getProfile() != active) {
                return;
            }
            if (isCollectionLogMilestone(candidate) && !(candidate instanceof CollectionLogTotalMilestone) && !active.getCollectionLogHistory().hasSnapshot()) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), "A full collection-log snapshot is required before adding collection milestones. Open your collection log to capture it automatically."));
                return;
            }
            Milestone existing = active.findMatchingGoal(candidate, schedule);
            if (existing == null && active.getGoals().size() >= GrindstoneRepository.MAX_MILESTONES) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), "This profile has reached its milestone limit. Remove an entry before adding another."));
                return;
            }
            if (existing != null) {
                String title = support.getTitle(existing);
                long oldTarget = existing.getProgressMaximum();
                long newTarget = candidate.getProgressMaximum();
                SwingUtilities.invokeLater(() -> {
                    if (support.getProfile() != active) {
                        return;
                    }
                    if (existing.supportsTarget()) {
                        if (JOptionPane.showConfirmDialog(DialogPlacement.getOwner(support.getOwner()), title + " is already tracked.\nUpdate its target from " + String.format(Locale.ROOT, "%,d to %,d?", oldTarget, newTarget) + "\nRepeat: " + schedule + (schedule == RepeatSchedule.NONE ? " (lifetime tracking)" : String.format(Locale.ROOT, " · %,d per period", periodTarget)) + "\nChanging repeat settings archives the current observed period.", "Update existing milestone", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
                            support.getClientThread().invokeLater(() -> {
                                if (support.getProfile() == active && active.getGoals().contains(existing)) {
                                    existing.setTarget(newTarget);
                                    support.configureRepeat(active, existing, schedule, periodTarget);
                                    active.touch();
                                    support.render();
                                }
                            });
                        }
                    } else {
                        JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), "This milestone is already in your profile.");
                    }
                });
            } else {
                candidate.refresh(support.getClient());
                boolean diary = candidate.getCategory() == MilestoneCategory.ACHIEVEMENT_DIARY;
                boolean quest = candidate.getCategory() == MilestoneCategory.QUEST;
                if ((diary || quest) && candidate.getStatus() == MilestoneStatus.COMPLETE) {
                    SwingUtilities.invokeLater(() -> {
                        if (support.getProfile() == active) {
                            JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), diary ? "This achievement diary is already complete. Choose an unfinished diary to track." : "This quest or quest series is already complete. Choose an unfinished quest to track.");
                        }
                    });
                    return;
                }
                support.configureRepeat(active, candidate, schedule, periodTarget);
                active.add(candidate, active.getMetadata(candidate));
                SwingUtilities.invokeLater(() -> {
                    if (support.getProfile() != active) {
                        return;
                    }
                    addedCategory.accept(candidate.getCategory());
                });
                support.render();
            }
        });
    }

    private boolean isCollectionLogMilestone(Milestone goal) {
        return goal.getCategory() == MilestoneCategory.COLLECTION_LOG;
    }
}
