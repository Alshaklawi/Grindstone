package com.grinder.repository;

import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.impl.ChecklistGoal;
import com.grinder.content.goal.impl.CustomGoal;
import java.time.Instant;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class LombokCompatibilityTest {
    @Test
    public void generatedMetadataAccessorsKeepValidatedAndDerivedBehavior() {
        GoalMetadata metadata = new GoalMetadata();
        metadata.setPinned(true);
        metadata.setSample(true);
        metadata.setDisplayCategory(GoalCategory.SKILLING);
        assertTrue(metadata.isPinned());
        assertTrue(metadata.isSample());
        assertEquals(GoalCategory.SKILLING, metadata.getDisplayCategory());
        assertTrue(metadata.isCollapsed(true));
        assertFalse(metadata.isCollapsed(false));
        metadata.setCollapsed(false);
        assertFalse(metadata.isCollapsed(true));
        metadata.setDeadline(1000, true);
        assertEquals(1000, metadata.getDeadlineAt());
        assertTrue(metadata.isDeadlineReminder());
        metadata.setDeadline(0, true);
        assertFalse(metadata.isDeadlineReminder());
        try {
            metadata.setDeadline(-1, true);
            fail("Invalid deadlines must still be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals(0, metadata.getDeadlineAt());
        }
    }

    @Test
    public void generatedHistoryConstructorsPreserveFieldOrderAndReadOnlyViews() {
        long now = Instant.parse("2026-10-19T10:00:00Z").toEpochMilli();
        CustomGoal goal = new CustomGoal("Routine", "Notes", GoalCategory.OTHER, 5, 5);
        GoalMetadata metadata = new GoalMetadata();
        assertTrue(metadata.observe(goal, "Routine", now));
        GoalMetadata.Completion completion = metadata.getCompletions().get(0);
        assertEquals("Routine", completion.getTitle());
        assertEquals(5, completion.getTarget());
        assertEquals(now, completion.getObservedAt());
        assertReadOnly(metadata.getCompletions());
        RecurringGoal recurring = metadata.recurring();
        recurring.configure(RepeatSchedule.DAILY, 5, now);
        recurring.observe(goal, now);
        recurring.restart(now + 1000);
        RecurringGoal.Period period = recurring.getHistory().get(0);
        assertEquals(RepeatSchedule.DAILY, period.getSchedule());
        assertEquals(RepeatSchedule.DAILY.start(now), period.getStart());
        assertEquals(now + 1000, period.getEnd());
        assertEquals(5, period.getProgress());
        assertEquals(5, period.getTarget());
        assertEquals(now, period.getBaselineAt());
        assertEquals(now, period.getLastObservedAt());
        assertEquals(now, period.getCompletedAt());
        assertEquals("Period restarted · last observed", period.getOutcome());
        assertReadOnly(recurring.getHistory());
    }

    @Test
    public void checklistCollectionsAndArraysRemainDefensive() {
        ChecklistGoal goal = new ChecklistGoal("Routine", "Notes", Arrays.asList("First", "Second"));
        assertFalse(goal.getId().isEmpty());
        assertEquals("Notes", goal.getDescription());
        assertReadOnly(goal.getSteps());
        boolean[] checks = goal.getChecks();
        checks[0] = true;
        assertEquals(0, goal.getProgressCurrent());
    }

    private static void assertReadOnly(java.util.List<?> list) {
        try {
            list.clear();
            fail("History and checklist views must stay read-only");
        } catch (UnsupportedOperationException expected) {
            assertFalse(list.isEmpty());
        }
    }
}
