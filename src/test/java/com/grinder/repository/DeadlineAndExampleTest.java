package com.grinder.repository;

import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.impl.ChecklistGoal;
import com.grinder.ui.editor.DeadlineForm;
import java.time.Instant;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class DeadlineAndExampleTest {
    @Test
    public void deadlinesUseEndOfUtcDateAndNeverResetProgress() {
        long deadline = DeadlineForm.parseDate("2026-10-19");
        assertEquals(Instant.parse("2026-10-20T00:00:00Z").toEpochMilli(), deadline);
        assertEquals(0, DeadlineForm.parseDate(" "));
        GoalMetadata metadata = new GoalMetadata();
        metadata.setDeadline(deadline, true);
        assertTrue(metadata.deadlineText(deadline - 1).startsWith("Due"));
        assertTrue(metadata.deadlineText(deadline).startsWith("Past due"));
        assertTrue(metadata.deadlineText(deadline).contains("2026-10-19 UTC"));
        assertTrue(metadata.isDeadlineReminder());
        metadata.setDeadline(0, true);
        assertFalse(metadata.isDeadlineReminder());
    }

    @Test
    public void savedMetadataAndCopiesKeepPersonalPresentationSettings() {
        GrinderProfile profile = TestProfiles.populated(42);
        Goal goal = profile.getGoals().get(0);
        GoalMetadata metadata = profile.metadata(goal);
        metadata.setDeadline(DeadlineForm.parseDate("2026-12-31"), true);
        metadata.setDisplayCategory(GoalCategory.OTHER);
        GoalMetadata copied = metadata.copy();
        copied.setDeadline(0, false);
        assertTrue(metadata.getDeadlineAt() > 0);
        GrinderProfile restored = TestProfiles.REPOSITORY.fromJson(TestProfiles.REPOSITORY.toJson(profile));
        GoalMetadata saved = restored.metadata(restored.getGoals().get(0));
        assertEquals(metadata.getDeadlineAt(), saved.getDeadlineAt());
        assertTrue(saved.isDeadlineReminder());
        assertEquals(GoalCategory.OTHER, saved.getDisplayCategory());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidCalendarDates() {
        DeadlineForm.parseDate("2026-02-30");
    }
}
