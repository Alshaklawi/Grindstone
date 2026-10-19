package com.grinder.notification;

import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class CompletionTransitionsTest {
    @Test
    public void initialCompletedGoalsAreSilentAndUnfinishedGoalsAlertOnce() {
        CompletionTransitions transitions = new CompletionTransitions();
        assertFalse(transitions.observe("loaded", "99", true));
        assertFalse(transitions.observe("loaded", "99", true));
        assertFalse(transitions.observe("active", "99", false));
        assertTrue(transitions.observe("active", "99", true));
        assertFalse(transitions.observe("active", "99", true));
    }

    @Test
    public void spendingPointsOrUncheckingDoesNotRepeatTheSameAlert() {
        CompletionTransitions transitions = new CompletionTransitions();
        assertFalse(transitions.observe("goal", "1000", false));
        assertTrue(transitions.observe("goal", "1000", true));
        assertFalse(transitions.observe("goal", "1000", false));
        assertFalse(transitions.observe("goal", "1000", true));
    }

    @Test
    public void targetEditsSeedStateAndLaterProgressCanReachTheNewTarget() {
        CompletionTransitions transitions = new CompletionTransitions();
        transitions.observe("goal", "1000", false);
        assertFalse(transitions.observe("goal", "50", true));
        assertFalse(transitions.observe("goal", "2000", false));
        assertTrue(transitions.observe("goal", "2000", true));
    }

    @Test
    public void recurringPeriodsEachHaveAnIndependentCompletion() {
        CompletionTransitions transitions = new CompletionTransitions();
        transitions.observe("daily", "10:Daily:1", false);
        assertTrue(transitions.observe("daily", "10:Daily:1", true));
        assertFalse(transitions.observe("daily", "10:Daily:2", false));
        assertTrue(transitions.observe("daily", "10:Daily:2", true));
        assertFalse(transitions.observe("daily", "10:Daily:3", true));
    }

    @Test
    public void deletedUnavailableAndReloadedGoalsAreSeededSilently() {
        CompletionTransitions transitions = new CompletionTransitions();
        transitions.observe("goal", "10", false);
        transitions.retain(Collections.emptySet());
        assertFalse(transitions.observe("goal", "10", true));
        transitions.clear();
        assertFalse(transitions.observe("goal", "10", true));
    }

    @Test
    public void sameTargetForDifferentGoalsDoesNotShareNotificationState() {
        CompletionTransitions transitions = new CompletionTransitions();
        transitions.observe("normal", "100", false);
        transitions.observe("wilderness", "100", false);
        assertTrue(transitions.observe("normal", "100", true));
        assertTrue(transitions.observe("wilderness", "100", true));
    }
}
