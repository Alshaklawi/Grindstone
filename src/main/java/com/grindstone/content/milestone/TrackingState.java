package com.grindstone.content.milestone;

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
    LINKED("Linked tracker readings", "Uses selected local trackers and captured targets. Collection sources count observed unlocks, not confirmed absence; owned items use known storage, not guaranteed all-storage totals. Missing sources are unavailable. Repeat counts cover observed completed periods; prerequisites never gate gameplay."),
    LIVE("Live client read", "Read from the logged-in client. Only data exposed by the client can be tracked."),
    COUNTER("Client-reported counter", "Uses the client's activity total. Some counters require opening the native activity interface or completing that activity to refresh; a client-reported zero is not proof of a fully synchronized server total."),
    WAITING("Waiting for data", "Log in or open the relevant native interface. No confirmed zero is assumed while data is unavailable."),
    PARTIAL_STORAGE("Partial known storage", "Bank contents have not loaded this login. Counts cover known storage only, not all possible player storage."),
    SNAPSHOT("Known storage snapshot", "Last known personal storage contents, not a live all-storage guarantee. Reopen storage to refresh it."),
    OBSERVED_LOG("Observed unlocks", "Only known collection-log observations are counted. Zero observed does not prove an item is unobtained; open your collection log to refresh known unlocks."),
    LATE_BASELINE("Captured-period gains", "The baseline was captured after the period began. Only gains since that observation are counted; offline progress across a reset cannot be attributed exactly.");
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
        if (!loggedIn || recurring.isActive() && recurring.getBaselineAt() == 0 || goal instanceof ClientCompletionMilestone && goal.getProgressMaximum() == 0 || goal.getCategory() == MilestoneCategory.COLLECTION_LOG && goal.getProgressMaximum() == 0) {
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
