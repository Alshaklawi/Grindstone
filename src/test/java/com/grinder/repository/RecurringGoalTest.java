package com.grinder.repository;

import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.impl.CustomGoal;
import com.grinder.content.goal.impl.experience.SkillExperienceGoal;
import java.time.Instant;
import java.util.Collections;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class RecurringGoalTest {
    private long time(String value) { return Instant.parse(value).toEpochMilli(); }
    @Test public void usesUtcCalendarBoundaries() {
        long now = time("2026-10-21T12:00:00Z");
        assertEquals(time("2026-10-21T00:00:00Z"), RepeatSchedule.DAILY.start(now));
        assertEquals(time("2026-10-19T00:00:00Z"), RepeatSchedule.WEEKLY.start(now));
        assertEquals(time("2026-11-01T00:00:00Z"), RepeatSchedule.MONTHLY.next(RepeatSchedule.MONTHLY.start(now)));
        assertEquals(time("2028-03-01T00:00:00Z"), RepeatSchedule.MONTHLY.next(time("2028-02-01T00:00:00Z")));
    }
    @Test public void resetsManualProgressAndKeepsObservedCompletion() {
        CustomGoal goal = new CustomGoal("Daily", "", GoalCategory.OTHER, 0, 5);
        RecurringGoal recurring = new RecurringGoal();
        long now = time("2026-10-19T10:00:00Z");
        recurring.configure(RepeatSchedule.DAILY, 5, now);
        recurring.observe(goal, now);
        goal.update("Daily", "", GoalCategory.OTHER, 5, 5);
        recurring.observe(goal, now + 1000);
        assertEquals(5, recurring.getProgress());
        recurring.observe(goal, time("2026-10-20T10:00:00Z"));
        assertEquals(0, goal.getProgressCurrent());
        assertEquals(0, recurring.getProgress());
        assertEquals("Completed", recurring.getHistory().get(0).getOutcome());
        assertEquals(now + 1000, recurring.getHistory().get(0).getCompletedAt());
    }
    @Test public void offlineGapsAreNotInventedMissesAndPersist() {
        GrinderProfile profile = new GrinderProfile(42);
        CustomGoal goal = new CustomGoal("Daily", "", GoalCategory.OTHER, 2, 5);
        profile.replace(Collections.singletonList(goal));
        RecurringGoal recurring = profile.metadata(goal).recurring();
        long now = time("2026-10-19T10:00:00Z");
        recurring.configure(RepeatSchedule.DAILY, 5, now);
        recurring.observe(goal, now);
        recurring.observe(goal, time("2026-11-01T10:00:00Z"));
        assertEquals(2, recurring.getHistory().size());
        assertEquals("Not observed", recurring.getHistory().get(1).getOutcome());
        GrinderProfile loaded = GrinderRepository.fromJson(GrinderRepository.toJson(profile));
        RecurringGoal restored = loaded.metadata(loaded.getGoals().get(0)).recurring();
        assertEquals(RepeatSchedule.DAILY, restored.getSchedule());
        assertEquals(2, restored.getHistory().size());
        assertEquals(recurring.getBaselineAt(), restored.getBaselineAt());
    }
    @Test public void automaticCountersCountGainsNotExistingTotals() {
        long[] value = {1_000_000};
        SkillExperienceGoal goal = new SkillExperienceGoal(Skill.ATTACK) {
            @Override public long getProgressCurrent() { return value[0]; }
        };
        RecurringGoal recurring = new RecurringGoal();
        long now = time("2026-10-19T10:00:00Z");
        recurring.configure(RepeatSchedule.DAILY, 100, now);
        recurring.observe(goal, now);
        assertEquals(0, recurring.getProgress());
        value[0] += 60;
        recurring.observe(goal, now + 1);
        assertEquals(60, recurring.getProgress());
        value[0] += 90;
        recurring.observe(goal, time("2026-10-20T10:00:00Z"));
        assertEquals(0, recurring.getProgress());
        assertEquals(60, recurring.getHistory().get(0).getProgress());
        value[0] += 20;
        recurring.observe(goal, time("2026-10-20T10:00:01Z"));
        assertEquals(20, recurring.getProgress());
    }
    @Test public void sameSettingsPreserveProgressAndChangesArchiveTheOldTarget() {
        CustomGoal goal = new CustomGoal("Daily", "", GoalCategory.OTHER, 2, 5);
        RecurringGoal recurring = new RecurringGoal();
        long now = time("2026-10-19T00:00:00Z");
        recurring.configure(RepeatSchedule.DAILY, 5, now);
        recurring.observe(goal, now);
        recurring.configure(RepeatSchedule.DAILY, 5, now);
        assertEquals(2, recurring.getProgress());
        assertTrue(recurring.getHistory().isEmpty());
        recurring.configure(RepeatSchedule.WEEKLY, 10, now);
        recurring.validate();
        assertEquals(5, recurring.getHistory().get(0).getTarget());
        assertEquals("Schedule changed", recurring.getHistory().get(0).getOutcome());
        assertEquals(0, recurring.getBaselineAt());
    }
    @Test public void transferredBackupsKeepSchedulesButNotAnotherPlayersPeriods() {
        GrinderProfile profile = new GrinderProfile(42);
        CustomGoal goal = new CustomGoal("Daily", "", GoalCategory.OTHER, 2, 5);
        profile.replace(Collections.singletonList(goal));
        RecurringGoal recurring = profile.metadata(goal).recurring();
        long now = time("2026-10-19T10:00:00Z");
        recurring.configure(RepeatSchedule.DAILY, 5, now);
        recurring.observe(goal, now);
        recurring.observe(goal, time("2026-10-20T10:00:00Z"));
        GrinderProfile restored = GrinderRepository.importBackup(GrinderRepository.exportBackup(profile), 99);
        RecurringGoal transferred = restored.metadata(restored.getGoals().get(0)).recurring();
        assertEquals(RepeatSchedule.DAILY, transferred.getSchedule());
        assertEquals(5, transferred.getTarget());
        assertEquals(0, transferred.getBaselineAt());
        assertTrue(transferred.getHistory().isEmpty());
    }
    @Test public void retainsOnlyTheLatestFiveHundredPeriodsAndIgnoresClockRollback() {
        CustomGoal goal = new CustomGoal("Daily", "", GoalCategory.OTHER, 0, 5);
        RecurringGoal recurring = new RecurringGoal();
        long now = time("2026-10-19T10:00:00Z");
        recurring.configure(RepeatSchedule.DAILY, 5, now);
        for (int index = 0; index < 510; index++) { recurring.observe(goal, now + index * 86_400_000L); }
        assertEquals(500, recurring.getHistory().size());
        long baseline = recurring.getBaselineAt();
        recurring.observe(goal, now);
        assertEquals(baseline, recurring.getBaselineAt());
        recurring.validate();
    }
}
