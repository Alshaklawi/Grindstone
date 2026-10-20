package com.grindstone.repository;

import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.impl.CustomMilestone;
import com.grindstone.content.milestone.impl.experience.SkillExperienceMilestone;
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
public class RecurringMilestoneTest {
    @Test
    public void restartingArchivesObservedProgressWithoutChangingTheSchedule() {
        CustomMilestone goal = new CustomMilestone("Daily", "", MilestoneCategory.OTHER, 2, 5);
        RecurringMilestone recurring = new RecurringMilestone();
        long now = time("2026-10-19T10:00:00Z");
        recurring.configure(RepeatSchedule.DAILY, 5, now);
        recurring.observe(goal, now);
        recurring.restart(now + 1000);
        assertEquals(RepeatSchedule.DAILY, recurring.getSchedule());
        assertEquals(5, recurring.getTarget());
        assertEquals(0, recurring.getBaselineAt());
        assertEquals(1, recurring.getHistory().size());
        assertEquals(2, recurring.getHistory().get(0).getProgress());
        assertEquals("Period restarted · last observed", recurring.getHistory().get(0).getOutcome());
        goal.update("Daily", "", MilestoneCategory.OTHER, 0, 5);
        recurring.observe(goal, now + 2000);
        assertEquals(0, recurring.getProgress());
        assertEquals(now + 2000, recurring.getBaselineAt());
    }
    private long time(String value) {
        return Instant.parse(value).toEpochMilli();
    }
    @Test
    public void usesUtcCalendarBoundaries() {
        long now = time("2026-10-21T12:00:00Z");
        assertEquals(time("2026-10-21T00:00:00Z"), RepeatSchedule.DAILY.getPeriodStart(now));
        assertEquals(time("2026-10-19T00:00:00Z"), RepeatSchedule.WEEKLY.getPeriodStart(now));
        assertEquals(time("2026-11-01T00:00:00Z"), RepeatSchedule.MONTHLY.getNextReset(RepeatSchedule.MONTHLY.getPeriodStart(now)));
        assertEquals(time("2028-03-01T00:00:00Z"), RepeatSchedule.MONTHLY.getNextReset(time("2028-02-01T00:00:00Z")));
    }
    @Test
    public void resetsManualProgressAndKeepsObservedCompletion() {
        CustomMilestone goal = new CustomMilestone("Daily", "", MilestoneCategory.OTHER, 0, 5);
        RecurringMilestone recurring = new RecurringMilestone();
        long now = time("2026-10-19T10:00:00Z");
        recurring.configure(RepeatSchedule.DAILY, 5, now);
        recurring.observe(goal, now);
        goal.update("Daily", "", MilestoneCategory.OTHER, 5, 5);
        recurring.observe(goal, now + 1000);
        assertEquals(5, recurring.getProgress());
        recurring.observe(goal, time("2026-10-20T10:00:00Z"));
        assertEquals(0, goal.getProgressCurrent());
        assertEquals(0, recurring.getProgress());
        assertEquals("Completed", recurring.getHistory().get(0).getOutcome());
        assertEquals(now + 1000, recurring.getHistory().get(0).getCompletedAt());
    }
    @Test
    public void offlineGapsAreNotInventedMissesAndPersist() {
        GrindstoneProfile profile = TestProfiles.populated(42);
        CustomMilestone goal = new CustomMilestone("Daily", "", MilestoneCategory.OTHER, 2, 5);
        profile.replace(Collections.singletonList(goal));
        RecurringMilestone recurring = profile.getMetadata(goal).getRecurring();
        long now = time("2026-10-19T10:00:00Z");
        recurring.configure(RepeatSchedule.DAILY, 5, now);
        recurring.observe(goal, now);
        recurring.observe(goal, time("2026-11-01T10:00:00Z"));
        assertEquals(2, recurring.getHistory().size());
        assertEquals("Not observed", recurring.getHistory().get(1).getOutcome());
        GrindstoneProfile loaded = TestProfiles.REPOSITORY.fromJson(TestProfiles.REPOSITORY.toJson(profile));
        RecurringMilestone restored = loaded.getMetadata(loaded.getGoals().get(0)).getRecurring();
        assertEquals(RepeatSchedule.DAILY, restored.getSchedule());
        assertEquals(2, restored.getHistory().size());
        assertEquals(recurring.getBaselineAt(), restored.getBaselineAt());
    }
    @Test
    public void automaticCountersCountGainsNotExistingTotals() {
        long[] value = {1_000_000};
        SkillExperienceMilestone goal = new SkillExperienceMilestone(Skill.ATTACK) {
            @Override
            public long getProgressCurrent() {
                return value[0];
            }
        };
        RecurringMilestone recurring = new RecurringMilestone();
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
    @Test
    public void sameSettingsPreserveProgressAndChangesArchiveTheOldTarget() {
        CustomMilestone goal = new CustomMilestone("Daily", "", MilestoneCategory.OTHER, 2, 5);
        RecurringMilestone recurring = new RecurringMilestone();
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
    @Test
    public void transferredBackupsKeepSchedulesButNotAnotherPlayersPeriods() {
        GrindstoneProfile profile = TestProfiles.populated(42);
        CustomMilestone goal = new CustomMilestone("Daily", "", MilestoneCategory.OTHER, 2, 5);
        profile.replace(Collections.singletonList(goal));
        RecurringMilestone recurring = profile.getMetadata(goal).getRecurring();
        long now = time("2026-10-19T10:00:00Z");
        recurring.configure(RepeatSchedule.DAILY, 5, now);
        recurring.observe(goal, now);
        recurring.observe(goal, time("2026-10-20T10:00:00Z"));
        GrindstoneProfile restored = TestProfiles.REPOSITORY.importBackup(TestProfiles.REPOSITORY.exportBackup(profile), 99);
        RecurringMilestone transferred = restored.getMetadata(restored.getGoals().get(0)).getRecurring();
        assertEquals(RepeatSchedule.DAILY, transferred.getSchedule());
        assertEquals(5, transferred.getTarget());
        assertEquals(0, transferred.getBaselineAt());
        assertTrue(transferred.getHistory().isEmpty());
    }
    @Test
    public void retainsOnlyTheLatestFiveHundredPeriodsAndIgnoresClockRollback() {
        CustomMilestone goal = new CustomMilestone("Daily", "", MilestoneCategory.OTHER, 0, 5);
        RecurringMilestone recurring = new RecurringMilestone();
        long now = time("2026-10-19T10:00:00Z");
        recurring.configure(RepeatSchedule.DAILY, 5, now);
        for (int index = 0; index < 510; index++) {
            recurring.observe(goal, now + index * 86_400_000L);
        }
        assertEquals(500, recurring.getHistory().size());
        long baseline = recurring.getBaselineAt();
        recurring.observe(goal, now);
        assertEquals(baseline, recurring.getBaselineAt());
        recurring.validate();
    }
}
