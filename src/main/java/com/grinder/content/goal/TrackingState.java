package com.grinder.content.goal;

import com.grinder.content.goal.impl.TrackerGoal;
import com.grinder.content.goal.impl.item.PossessedItemGoal;
import com.grinder.repository.RecurringGoal;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public enum TrackingState {
    MANUAL("Manual progress", "Progress is entered by you. Grinder does not perform game actions."),
    LINKED("Linked tracker readings", "Uses selected local trackers and captured targets. Collection sources count observed unlocks, not confirmed absence; owned items use known storage, not guaranteed all-storage totals. Missing sources are unavailable. Repeat counts cover observed completed periods; prerequisites never gate gameplay."),
    LIVE("Live client read", "Read from the logged-in client. Only data exposed by the client can be tracked."),
    COUNTER("Client-reported counter", "Uses the client's activity total. Some counters require opening the native activity interface or completing that activity to refresh; a client-reported zero is not proof of a fully synchronized server total."),
    WAITING("Waiting for data", "Log in or open the relevant native interface. No confirmed zero is assumed while data is unavailable."),
    PARTIAL_STORAGE("Partial known storage", "Bank contents have not loaded this login. Counts cover known storage only, not all possible player storage."),
    SNAPSHOT("Known storage snapshot", "Last known personal storage contents, not a live all-storage guarantee. Reopen storage to refresh it."),
    OBSERVED_LOG("Observed unlocks", "Only known collection-log observations are counted. Zero observed does not prove an item is unobtained; open your collection log to refresh known unlocks."),
    LATE_BASELINE("Captured-period gains", "The baseline was captured after the period began. Only gains since that observation are counted; offline progress across a reset cannot be attributed exactly.");
    private final String label;
    private final String detail;
    TrackingState(String label, String detail) {
        this.label = label;
        this.detail = detail;
    }
    public String getLabel() {
        return label;
    }
    public String getDetail() {
        return detail;
    }
    public static TrackingState of(Goal goal, RecurringGoal recurring, boolean loggedIn, boolean bankLoaded) {
        if (goal instanceof com.grinder.content.goal.impl.LinkedGoal) {
            return ((com.grinder.content.goal.impl.LinkedGoal) goal).isAvailable() ? LINKED : WAITING;
        }
        if (goal instanceof com.grinder.content.goal.ManualGoal) {
            return MANUAL;
        }
        if (!loggedIn || recurring.isActive() && recurring.getBaselineAt() == 0 || goal instanceof ClientCompletionGoal && goal.getProgressMaximum() == 0 || goal.getCategory() == GoalCategory.COLLECTION_LOG && goal.getProgressMaximum() == 0) {
            return WAITING;
        }
        if (goal instanceof PossessedItemGoal) {
            return bankLoaded ? SNAPSHOT : PARTIAL_STORAGE;
        }
        if (goal.getCategory() == GoalCategory.COLLECTION_LOG) {
            return OBSERVED_LOG;
        }
        if (recurring.isActive() && recurring.getBaselineAt() > recurring.getStart() + 60_000) {
            return LATE_BASELINE;
        }
        return goal instanceof TrackerGoal ? COUNTER : LIVE;
    }
}
