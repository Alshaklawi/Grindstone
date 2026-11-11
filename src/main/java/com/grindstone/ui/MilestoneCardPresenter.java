package com.grindstone.ui;

import com.grindstone.config.GrindstoneConfig;
import com.grindstone.content.milestone.ManualMilestone;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.content.milestone.TrackingState;
import com.grindstone.content.milestone.impl.ChecklistMilestone;
import com.grindstone.content.milestone.impl.CustomMilestone;
import com.grindstone.content.milestone.impl.LinkedMilestone;
import com.grindstone.content.milestone.impl.TrackerMilestone;
import com.grindstone.content.milestone.impl.experience.BaseExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.SessionExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.SkillExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.TotalExperienceMilestone;
import com.grindstone.content.milestone.impl.item.PossessedItemMilestone;
import com.grindstone.content.milestone.impl.level.BaseLevelMilestone;
import com.grindstone.content.milestone.impl.level.CombatLevelMilestone;
import com.grindstone.content.milestone.impl.level.SkillLevelMilestone;
import com.grindstone.content.milestone.impl.level.TotalLevelMilestone;
import com.grindstone.overlay.ProgressOverlayEntry;
import com.grindstone.repository.GrindstoneProfile;
import com.grindstone.repository.MilestoneMetadata;
import com.grindstone.repository.RecurringMilestone;
import com.grindstone.repository.RepeatSchedule;
import com.grindstone.ui.components.ContentHeightPanel;
import com.grindstone.ui.components.MilestoneComponent;
import com.grindstone.ui.components.MilestoneHistoryPanel;
import com.grindstone.ui.util.DialogPlacement;
import com.grindstone.ui.util.MilestoneCardCache;
import com.grindstone.ui.util.UiTypography;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
final class MilestoneCardPresenter {
    private final MilestoneActionSupport support;
    private final Client client;
    private final ClientThread clientThread;
    private final GrindstoneConfig config;
    private final MilestoneEditActions editActions;
    private final MilestoneCardCache<MilestoneView> cardCache = new MilestoneCardCache<>();

    MilestoneCardPresenter(MilestoneActionSupport support, GrindstoneConfig config, MilestoneEditActions editActions) {
        this.support = support;
        this.client = support.getClient();
        this.clientThread = support.getClientThread();
        this.config = config;
        this.editActions = editActions;
    }

    void clear() {
        cardCache.clear();
    }

    void invalidate(Milestone goal) {
        cardCache.invalidate(goal);
    }

    List<Supplier<MilestoneView>> prepare(GrindstoneProfile currentProfile, boolean loggedIn, long now, boolean captureOverlay, List<ProgressOverlayEntry> overlayEntries) {
        List<Supplier<MilestoneView>> preparedViews = new ArrayList<>();
        if (currentProfile == null) {
            return preparedViews;
        }
        cardCache.retain(currentProfile.getGoals());
        boolean bankSnapshot = currentProfile.getPlayerStorage().hasBankSnapshot();
        boolean collapseDetails = config.collapseDetailsByDefault();
        List<?> displaySettings = Arrays.asList(config.showGoalIcons(), config.showProgressBar(), config.maxProgressChunks(), config.showCompletionHistory(), config.showActivityIcons(), config.itemsPerRow(), config.maxItemRows(), config.showTrackingState(), config.showObtainedItems(), config.showItemTooltips());
        for (Milestone goal : currentProfile.getGoals()) {
            if (!config.linkedGoalsEnabled() && goal instanceof LinkedMilestone) {
                continue;
            }
            if (goal == null || (!config.collectionLogEnabled() && isCollectionLogMilestone(goal)) || (!config.levelGoalsEnabled() && isLevelMilestone(goal)) || (!config.experienceGoalsEnabled() && isExperienceMilestone(goal)) || (!config.trackerGoalsEnabled() && goal instanceof TrackerMilestone) || (!config.questGoalsEnabled() && goal.getCategory() == MilestoneCategory.QUEST) || (!config.diaryGoalsEnabled() && goal.getCategory() == MilestoneCategory.ACHIEVEMENT_DIARY) || (!config.combatAchievementGoalsEnabled() && goal.getCategory() == MilestoneCategory.COMBAT_ACHIEVEMENT) || (!config.ownedItemGoalsEnabled() && goal instanceof PossessedItemMilestone) || (!config.customGoalsEnabled() && goal instanceof ManualMilestone)) {
                continue;
            }

            String title = support.getTitle(goal);
            MilestoneMetadata metadata = currentProfile.getMetadata(goal);
            RecurringMilestone recurring = metadata.getRecurring();
            boolean repeating = recurring.isActive() && RecurringMilestone.supports(goal);
            TrackingState tracking = TrackingState.of(goal, recurring, loggedIn, bankSnapshot);
            List<?> key = Arrays.asList(currentProfile, metadata, title, goal.getDescription(), goal.getProgressCurrent(), goal.getProgressMaximum(), goal.getStatus(), recurring.getSchedule(), recurring.getProgress(), recurring.getTarget(), recurring.getBaselineAt(), recurring.getStart(), metadata.isPinned(), metadata.isCollapsed(collapseDetails), metadata.getCompletions().size(), tracking, displaySettings);
            if (goal instanceof ChecklistMilestone) {
                key = Arrays.asList(key, Arrays.hashCode(((ChecklistMilestone) goal).getChecks()));
            }
            if (goal instanceof LinkedMilestone) {
                key = Arrays.asList(key, ((LinkedMilestone) goal).getSourceProgress());
            }
            key = Arrays.asList(key, metadata.getDeadlineAt(), metadata.getDeadlineAt() > 0 && now >= metadata.getDeadlineAt(), metadata.getDisplayCategory());
            MilestoneView cached = cardCache.get(goal, key);
            if (cached != null && tracking != TrackingState.WAITING && goal.getProgressMaximum() > 0) {
                if (captureOverlay) {
                    overlayEntries.add(createOverlayEntry(goal, title, repeating ? recurring.getSchedule() : RepeatSchedule.NONE, repeating ? recurring.getProgress() : goal.getProgressCurrent(), repeating ? recurring.getTarget() : goal.getProgressMaximum(), isComplete(repeating ? recurring.getStatus() : goal.getStatus()), true, metadata.isPinned()));
                }
                preparedViews.add(() -> cached);
                continue;
            }
            Supplier<MilestoneComponent> preparedCard = goal.prepare(client, config);
            currentProfile.markDirty();
            tracking = TrackingState.of(goal, recurring, loggedIn, bankSnapshot);
            TrackingState displayTracking = tracking;
            MilestoneStatus status = repeating ? recurring.getStatus() : goal.getStatus();
            long progressCurrent = repeating ? recurring.getProgress() : goal.getProgressCurrent();
            long progressMaximum = repeating ? recurring.getTarget() : goal.getProgressMaximum();
            if (tracking == TrackingState.WAITING) {
                status = MilestoneStatus.NOT_STARTED;
                progressCurrent = 0;
                progressMaximum = 0;
            }
            if (loggedIn && tracking != TrackingState.WAITING) {
                metadata.observeReading(goal, title, now);
            }
            if (!repeating && tracking != TrackingState.WAITING) {
                metadata.observe(goal, title, now);
            }
            MilestoneMetadata displayMetadata = metadata.copy();
            RecurringMilestone displayRecurring = displayMetadata.getRecurring();
            MilestoneCategory category = support.getCategory(goal);
            String searchText = getSearchText(title, goal.getDescription(), category.getDisplayName());
            boolean hasProgress = goal.hasProgress();
            long shownCurrent = progressCurrent;
            String sourceDetail = goal instanceof TrackerMilestone || goal instanceof SessionExperienceMilestone ? goal.getDescription() : "";
            long shownMaximum = progressMaximum;
            MilestoneStatus shownStatus = status;
            if (captureOverlay) {
                overlayEntries.add(createOverlayEntry(goal, title, repeating ? recurring.getSchedule() : RepeatSchedule.NONE, shownCurrent, shownMaximum, isComplete(shownStatus), tracking != TrackingState.WAITING, metadata.isPinned()));
            }
            List<String> steps = goal instanceof ChecklistMilestone ? new ArrayList<>(((ChecklistMilestone) goal).getSteps()) : new ArrayList<>();
            boolean[] checks = goal instanceof ChecklistMilestone ? ((ChecklistMilestone) goal).getChecks().clone() : new boolean[0];
            List<?> cacheKey = key;
            preparedViews.add(() -> {
                MilestoneComponent component = preparedCard.get();
                if (goal instanceof ChecklistMilestone) {
                    ChecklistMilestone checklist = (ChecklistMilestone) goal;
                    boolean[] checked = checks;
                    RepeatSchedule openedSchedule = displayRecurring.getSchedule();
                    long openedPeriod = openedSchedule.getPeriodStart(System.currentTimeMillis());
                    for (int step = 0; step < checked.length; step++) {
                        int index = step;
                        JCheckBox check = new JCheckBox("", checked[step]);
                        check.getAccessibleContext().setAccessibleName(steps.get(step));
                        check.setOpaque(false);
                        check.setFont(UiTypography.BODY);
                        check.setToolTipText("Check off this step. Repeating checklists reset each period.");
                        check.setAlignmentX(Component.LEFT_ALIGNMENT);
                        check.addActionListener(event -> {
                            boolean selected = check.isSelected();
                            clientThread.invokeLater(() -> {
                                if (support.isCurrentProfile(currentProfile) && currentProfile.getGoals().contains(goal) && currentProfile.getMetadata(goal).getRecurring().getSchedule() == openedSchedule && openedSchedule.getPeriodStart(System.currentTimeMillis()) == openedPeriod) {
                                    checklist.setChecked(index, selected);
                                    currentProfile.touch();
                                }
                                support.render();
                            });
                        });
                        JPanel stepRow = new ContentHeightPanel();
                        stepRow.setLayout(new BorderLayout(5, 0));
                        stepRow.setOpaque(false);
                        stepRow.setAlignmentX(Component.LEFT_ALIGNMENT);
                        JTextArea stepText = new JTextArea(steps.get(step), 0, 14);
                        stepText.setEditable(false);
                        stepText.setLineWrap(true);
                        stepText.setWrapStyleWord(true);
                        stepText.setOpaque(false);
                        stepText.setFont(UiTypography.BODY);
                        stepText.setToolTipText(check.getToolTipText());
                        stepRow.add(check, BorderLayout.WEST);
                        stepRow.add(stepText, BorderLayout.CENTER);
                        component.addDetail(stepRow);
                    }
                }
                component.setGoalStatus(shownStatus);
                component.putClientProperty("grindstone.repeat", repeating ? displayRecurring.getSchedule() : RepeatSchedule.NONE);
                component.putClientProperty("grindstone.id", displayMetadata.getId());
                component.putClientProperty("grindstone.ready", displayTracking != TrackingState.WAITING);
                if (repeating) {
                    component.setProgress(shownCurrent, shownMaximum, String.format(Locale.ROOT, "%,d / %,d this period", shownCurrent, shownMaximum));
                }
                if (displayTracking == TrackingState.WAITING) {
                    component.setProgress(0, 0, "Waiting for client data");
                } else if (displayTracking == TrackingState.OBSERVED_LOG) {
                    component.setProgress(shownCurrent, shownMaximum, String.format(Locale.ROOT, "%,d / %,d", shownCurrent, shownMaximum));
                } else if (displayTracking == TrackingState.PARTIAL_STORAGE || displayTracking == TrackingState.SNAPSHOT) {
                    component.setProgress(shownCurrent, shownMaximum, String.format(Locale.ROOT, "%,d / %,d", shownCurrent, shownMaximum));
                }
                String trackingDetail = !sourceDetail.isEmpty() && (displayTracking == TrackingState.COUNTER || displayTracking == TrackingState.LIVE) ? sourceDetail : displayTracking.getDetail();
                component.setReliability(config.showTrackingState() ? displayTracking.getCardLabel() : null, trackingDetail);
                component.setDeadlineNote(displayMetadata.getDeadlineText(System.currentTimeMillis()));
                component.putClientProperty("grindstone.deadline", displayMetadata.getDeadlineAt());
                component.setExpansionListener(null);
                component.setExpanded(!displayMetadata.isCollapsed(config.collapseDetailsByDefault()));
                component.setExpansionListener(expanded -> clientThread.invokeLater(() -> {
                    if (support.isCurrentProfile(currentProfile) && currentProfile.getGoals().contains(goal)) {
                        currentProfile.getMetadata(goal).setCollapsed(!expanded);
                        currentProfile.markDirty();
                    }
                }));
                component.setMaximumProgressChunks(config.maxProgressChunks());
                component.setGoalActions(event -> editActions.editMilestone(currentProfile, goal, title), event -> editActions.deleteMilestone(currentProfile, goal, title));
                component.addGoalAction("Set deadline...", "Choose a UTC deadline and optional sidebar reminder.", event -> editActions.editDeadline(currentProfile, goal));
                if (goal instanceof CustomMilestone) {
                    CustomMilestone custom = (CustomMilestone) goal;
                    long shownTarget = shownMaximum;
                    component.setManualActions(shownCurrent, shownTarget, () -> editActions.changeManualProgress(currentProfile, custom, value -> Math.min(custom.getProgressMaximum(), value + 1)), () -> editActions.changeManualProgress(currentProfile, custom, value -> value >= custom.getProgressMaximum() ? 0 : 1), () -> editActions.promptManualProgress(currentProfile, custom, shownCurrent, shownTarget));
                }
                RepeatSchedule repeatSchedule = displayRecurring.getSchedule();
                long repeatTarget = displayRecurring.getTarget();
                boolean pinned = displayMetadata.isPinned();
                component.putClientProperty("grindstone.pinned", pinned);
                Runnable togglePin = () -> clientThread.invokeLater(() -> {
                    if (support.isCurrentProfile(currentProfile) && currentProfile.getGoals().contains(goal)) {
                        MilestoneMetadata currentMetadata = currentProfile.getMetadata(goal);
                        currentMetadata.setPinned(!currentMetadata.isPinned());
                        currentProfile.touch();
                        support.render();
                    }
                });
                component.setPinAction(pinned, togglePin);
                component.addGoalAction(pinned ? "Unpin" : "Pin", null, event -> togglePin.run());
                if (config.showCompletionHistory()) {
                    component.addGoalAction("History...", "View progress and completion dates.", event -> clientThread.invokeLater(() -> {
                        if (!support.isCurrentProfile(currentProfile) || !currentProfile.getGoals().contains(goal)) {
                            return;
                        }
                        Supplier<MilestoneHistoryPanel> history = MilestoneHistoryPanel.createSnapshot(currentProfile.getMetadata(goal));
                        SwingUtilities.invokeLater(() -> {
                            if (support.isCurrentProfile(currentProfile)) {
                                JOptionPane.showMessageDialog(DialogPlacement.getOwner(support.getOwner()), history.get(), title + " — history", JOptionPane.PLAIN_MESSAGE);
                            }
                        });
                    }));
                }
                if (RecurringMilestone.supports(goal)) {
                    component.addGoalAction(repeating ? "Routine schedule..." : "Make routine...", "Choose a routine schedule or one-time tracking.", event -> editActions.editRepeat(currentProfile, goal, repeatSchedule, repeatTarget));
                }
                if (repeating) {
                    component.addGoalAction("Restart current period...", "Save this period to history and start fresh.", event -> editActions.restartPeriod(currentProfile, goal));
                }
                if (goal instanceof SessionExperienceMilestone) {
                    component.addGoalAction("Restart session XP...", "Start counting session XP again from your current XP.", event -> {
                        if (JOptionPane.showConfirmDialog(DialogPlacement.getOwner(support.getOwner()), "Start this session milestone from zero?", "Restart session milestone", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                            clientThread.invokeLater(() -> {
                                if (support.isCurrentProfile(currentProfile) && currentProfile.getGoals().contains(goal) && support.hasClientProfile(currentProfile)) {
                                    ((SessionExperienceMilestone) goal).restart();
                                    goal.refresh(client);
                                    currentProfile.touch();
                                    support.render();
                                }
                            });
                        }
                    });
                }
                component.setAchievementNote(repeating ? displayRecurring.getSchedule() + " · " + (displayRecurring.getBaselineAt() == 0 ? "Baseline pending" : "This period") : config.showCompletionHistory() && !displayMetadata.getCompletions().isEmpty() && shownStatus != MilestoneStatus.COMPLETE ? "Previously reached · now incomplete" : null);

                MilestoneView view = new MilestoneView(goal, component, title, searchText, category, shownStatus, hasProgress, shownCurrent, shownMaximum);
                cardCache.put(goal, cacheKey, view);
                return view;
            });
        }
        return preparedViews;
    }

    private ProgressOverlayEntry createOverlayEntry(Milestone goal, String title, RepeatSchedule schedule, long current, long target, boolean complete, boolean ready, boolean pinned) {
        if (!(goal instanceof LinkedMilestone)) {
            return new ProgressOverlayEntry(title, schedule, current, target, complete, ready, pinned, goal.getIcon(client));
        }
        LinkedMilestone linked = (LinkedMilestone) goal;
        List<ProgressOverlayEntry.Source> sources = new ArrayList<>(2);
        int remaining = 0;
        for (LinkedMilestone.SourceProgress reading : linked.getSourceProgress()) {
            if (reading.isComplete()) {
                continue;
            }
            if (sources.size() == 2) {
                remaining++;
                continue;
            }
            Milestone source = reading.getSource();
            String sourceTitle = source == null ? reading.getTitle() : source.getTitle(client);
            sources.add(new ProgressOverlayEntry.Source(sourceTitle, reading.getIcon(client, config.showActivityIcons()), reading.getCurrent(), reading.getTarget(), reading.getUnavailableReason() == null));
        }
        String unit;
        switch (linked.getMode()) {
            case MILESTONES:
                unit = "checkpoints";
                break;
            case REPEAT_COMPLETIONS:
                unit = "periods";
                break;
            case DEPENDENCY:
                unit = "linked target";
                break;
            default:
                unit = "targets";
                break;
        }
        return new ProgressOverlayEntry(title, schedule, current, target, complete, ready, pinned, goal.getIcon(client), unit, sources, remaining);
    }

    private static String getSearchText(String title, String description, String category) {
        return ((title == null ? "" : title) + " " + (description == null ? "" : description) + " " + (category == null ? "" : category)).toLowerCase(Locale.ROOT);
    }

    private static boolean isComplete(MilestoneStatus status) {
        return status != null && ("COMPLETE".equalsIgnoreCase(status.name()) || "COMPLETED".equalsIgnoreCase(status.name()));
    }

    private static boolean isCollectionLogMilestone(Milestone goal) {
        return goal.getCategory() == MilestoneCategory.COLLECTION_LOG;
    }

    private static boolean isLevelMilestone(Milestone goal) {
        return goal instanceof SkillLevelMilestone || goal instanceof CombatLevelMilestone || goal instanceof TotalLevelMilestone || goal instanceof BaseLevelMilestone;
    }

    private static boolean isExperienceMilestone(Milestone goal) {
        return goal instanceof SkillExperienceMilestone || goal instanceof TotalExperienceMilestone || goal instanceof BaseExperienceMilestone || goal instanceof SessionExperienceMilestone;
    }
}
