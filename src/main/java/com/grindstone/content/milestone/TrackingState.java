package com.grindstone.content.milestone;

import com.grindstone.content.milestone.impl.collectionlog.CollectionLogTotalMilestone;
import com.grindstone.content.milestone.impl.LinkedMilestone;
import com.grindstone.content.milestone.impl.TrackerMilestone;
import com.grindstone.content.milestone.impl.item.PossessedItemMilestone;
import com.grindstone.repository.RecurringMilestone;
import lombok.Getter;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public enum TrackingState {
    MANUAL("Manual progress", "Progress is entered by you. Grindstone does not perform game actions."),
    LINKED("Group progress", "Combines selected milestones using this group's targets."),
    LIVE("Live progress", "Progress updates while logged in."),
    COUNTER("Activity count", "Tracks your activity total. Open its in-game screen or complete the activity to refresh."),
    WAITING("Waiting for data", "Log in or open the relevant in-game screen to load progress."),
    PARTIAL_STORAGE("Bank data missing", "Bank data is missing. Open your bank to include its contents."),
    SNAPSHOT("Saved storage", "Uses saved storage contents. Reopen storage to refresh."),
    OBSERVED_LOG("Collection unlocks", "Tracks recorded collection-log unlocks. Open your collection log to refresh."),
    LATE_BASELINE("Partial period", "Tracking started partway through this period. Only progress recorded since then is counted.");
    @Getter
    private final String label;
    @Getter
    private final String detail;
    TrackingState(String label, String detail) {
        this.label = label;
        this.detail = detail;
    }
    public String getCardLabel() {
        return this == LATE_BASELINE ? "Partial period" : null;
    }
    public static TrackingState of(Milestone goal, RecurringMilestone recurring, boolean loggedIn, boolean bankLoaded) {
        if (goal instanceof LinkedMilestone) {
            return ((LinkedMilestone) goal).isAvailable() ? LINKED : WAITING;
        }
        if (goal instanceof ManualMilestone) {
            return MANUAL;
        }
        if (goal instanceof CollectionLogTotalMilestone) {
            return loggedIn && ((CollectionLogTotalMilestone) goal).isLoaded() ? LIVE : WAITING;
        }
        if (!loggedIn || recurring.isActive() && (recurring.getBaselineAt() == 0 || recurring.isAwaitingReading()) || goal instanceof ClientCompletionMilestone && goal.getProgressMaximum() == 0 || goal.getCategory() == MilestoneCategory.COLLECTION_LOG && goal.getProgressMaximum() == 0) {
            return WAITING;
        }
        if (goal instanceof PossessedItemMilestone) {
            return bankLoaded ? SNAPSHOT : PARTIAL_STORAGE;
        }
        if (goal.getCategory() == MilestoneCategory.COLLECTION_LOG) {
            return OBSERVED_LOG;
        }
        if (recurring.isActive() && recurring.getBaselineAt() > recurring.getStart() + 60_000) {
            return LATE_BASELINE;
        }
        return goal instanceof TrackerMilestone ? COUNTER : LIVE;
    }
}
