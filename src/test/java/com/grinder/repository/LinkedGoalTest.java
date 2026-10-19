package com.grinder.repository;

import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.GoalStatus;
import com.grinder.content.goal.impl.CustomGoal;
import com.grinder.content.goal.impl.LinkedGoal;
import com.grinder.content.goal.impl.collectionlog.CollectionLogItemGoal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class LinkedGoalTest {
    @Test
    public void everyLinkedModeHasAHiddenExampleWithResolvableSources() {
        GrinderProfile profile = new GrinderProfile(42);
        Set<LinkedGoal.Mode> modes = new HashSet<>();
        for (Goal goal : profile.getGoals()) {
            if (goal instanceof LinkedGoal) {
                LinkedGoal plan = (LinkedGoal) goal;
                modes.add(plan.getMode());
                assertTrue(profile.metadata(plan).isSample());
                assertTrue(plan.getSources().stream().allMatch(id -> profile.getGoals().stream().anyMatch(source -> profile.metadata(source).getId().equals(id))));
            }
        }
        assertEquals(new HashSet<>(Arrays.asList(LinkedGoal.Mode.values())), modes);
        profile.validateReferences();
    }

    @Test
    public void collectionSubsetsUseObservedUnlocksOnly() {
        GrinderProfile profile = empty();
        CollectionLogItemGoal first = new CollectionLogItemGoal(ItemID.DRAGON_PICKAXE, 1);
        CollectionLogItemGoal second = new CollectionLogItemGoal(ItemID.TWISTED_BOW, 1);
        profile.add(first);
        profile.add(second);
        LinkedGoal goal = new LinkedGoal("Selected unlocks", "", LinkedGoal.Mode.COLLECTION_SET, Arrays.asList(profile.metadata(first).getId(), profile.metadata(second).getId()), new long[]{1, 1}, 1);
        profile.getCollectionLogHistory().record(ItemID.DRAGON_PICKAXE, 1000);
        goal.update(profile, true);
        assertEquals(1, goal.getProgressCurrent());
        assertNotEquals(GoalStatus.COMPLETE, goal.getStatus());
        profile.getCollectionLogHistory().record(ItemID.TWISTED_BOW, 2000);
        goal.update(profile, true);
        assertEquals(GoalStatus.COMPLETE, goal.getStatus());
    }

    @Test
    public void rejectsLinkedReferencesThatCouldCreateCycles() {
        GrinderProfile profile = empty();
        CustomGoal source = manual(profile, "Source", 0, 1);
        LinkedGoal first = new LinkedGoal("First", "", LinkedGoal.Mode.CHECKLIST, Collections.singletonList(profile.metadata(source).getId()), new long[]{1}, 1);
        profile.add(first);
        LinkedGoal second = new LinkedGoal("Second", "", LinkedGoal.Mode.CHECKLIST, Collections.singletonList(profile.metadata(first).getId()), new long[]{1}, 1);
        profile.add(second);
        try {
            TestProfiles.REPOSITORY.importBackup(TestProfiles.REPOSITORY.exportBackup(profile), 42);
            fail("Nested linked references must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Linked goals"));
        }
    }
    @Test
    public void checklistsAnyOfAndPrerequisitesUseExplicitTargets() {
        GrinderProfile profile = empty();
        CustomGoal one = manual(profile, "One", 10, 10);
        CustomGoal two = manual(profile, "Two", 0, 5);
        for (LinkedGoal.Mode mode : new LinkedGoal.Mode[]{LinkedGoal.Mode.CHECKLIST, LinkedGoal.Mode.ANY_OF, LinkedGoal.Mode.DEPENDENCY}) {
            LinkedGoal goal = new LinkedGoal("Plan", "Notes", mode, Arrays.asList(profile.metadata(one).getId(), profile.metadata(two).getId()), new long[]{10, 5}, 1);
            goal.update(profile, true);
            assertEquals(mode == LinkedGoal.Mode.ANY_OF ? GoalStatus.COMPLETE : mode == LinkedGoal.Mode.DEPENDENCY ? GoalStatus.NOT_STARTED : GoalStatus.IN_PROGRESS, goal.getStatus());
            if (mode == LinkedGoal.Mode.DEPENDENCY) {
                assertEquals(0, goal.getProgressCurrent());
                assertTrue(goal.getDescription().contains("Prerequisites pending"));
            }
            two.update("Two", "", GoalCategory.OTHER, 5, 5);
            goal.update(profile, true);
            assertEquals(GoalStatus.COMPLETE, goal.getStatus());
            two.update("Two", "", GoalCategory.OTHER, 0, 5);
        }
    }

    @Test
    public void laddersCountMilestonesAndMissingSourcesNeverComplete() {
        GrinderProfile profile = empty();
        CustomGoal source = manual(profile, "Source", 80, 100);
        LinkedGoal ladder = new LinkedGoal("Ladder", "", LinkedGoal.Mode.MILESTONES, Collections.singletonList(profile.metadata(source).getId()), new long[]{50, 75, 99}, 1);
        ladder.update(profile, true);
        assertEquals(2, ladder.getProgressCurrent());
        profile.remove(source);
        ladder.update(profile, true);
        assertFalse(ladder.isAvailable());
        assertNotEquals(GoalStatus.COMPLETE, ladder.getStatus());
        assertTrue(ladder.getDescription().contains("Missing source"));
    }

    @Test
    public void spendingSafeAchievementsPersistButAreClearedForAnotherAccount() {
        GrinderProfile profile = empty();
        CustomGoal source = manual(profile, "Coins", 100, 100);
        LinkedGoal goal = new LinkedGoal("Reached 100", "", LinkedGoal.Mode.ACHIEVED, Collections.singletonList(profile.metadata(source).getId()), new long[]{100}, 1);
        profile.add(goal);
        goal.update(profile, true);
        source.update("Coins", "", GoalCategory.OTHER, 0, 100);
        goal.update(profile, true);
        assertEquals(GoalStatus.COMPLETE, goal.getStatus());
        GrinderProfile same = TestProfiles.REPOSITORY.importBackup(TestProfiles.REPOSITORY.exportBackup(profile), 42);
        same.updateLinkedGoals(true);
        assertEquals(GoalStatus.COMPLETE, same.findMatchingGoal(goal).getStatus());
        GrinderProfile other = TestProfiles.REPOSITORY.importBackup(TestProfiles.REPOSITORY.exportBackup(profile), 43);
        other.updateLinkedGoals(true);
        assertNotEquals(GoalStatus.COMPLETE, other.findMatchingGoal(goal).getStatus());
    }

    @Test
    public void repeatedObservationsDoNotCountTheSamePeriodTwice() {
        GrinderProfile profile = empty();
        CustomGoal source = manual(profile, "Daily", 0, 1);
        long now = Instant.parse("2026-10-19T10:00:00Z").toEpochMilli();
        RecurringGoal recurring = profile.metadata(source).recurring();
        recurring.configure(RepeatSchedule.DAILY, 1, now);
        recurring.observe(source, now);
        source.update("Daily", "", GoalCategory.OTHER, 1, 1);
        recurring.observe(source, now + 1);
        LinkedGoal goal = new LinkedGoal("Three days", "", LinkedGoal.Mode.REPEAT_COMPLETIONS, Collections.singletonList(profile.metadata(source).getId()), new long[]{1}, 3);
        goal.update(profile, true);
        goal.update(profile, true);
        assertEquals(1, goal.getProgressCurrent());
        recurring.observe(source, now + 86_400_000);
        goal.update(profile, true);
        assertEquals(1, goal.getProgressCurrent());
        source.update("Daily", "", GoalCategory.OTHER, 1, 1);
        recurring.observe(source, now + 86_400_001);
        goal.update(profile, true);
        assertEquals(2, goal.getProgressCurrent());
    }

    @Test
    public void mergedBackupsRemapReferencesToExistingMatchingSources() {
        GrinderProfile original = new GrinderProfile(42);
        GrinderProfile destination = new GrinderProfile(42);
        CustomGoal source = manual(original, "Unique source", 5, 5);
        LinkedGoal plan = new LinkedGoal("Unique plan", "", LinkedGoal.Mode.CHECKLIST, Collections.singletonList(original.metadata(source).getId()), new long[]{5}, 1);
        original.add(plan);
        destination.mergeBackup(TestProfiles.REPOSITORY.importBackup(TestProfiles.REPOSITORY.exportBackup(original), 42));
        destination.updateLinkedGoals(true);
        assertEquals(GoalStatus.COMPLETE, destination.findMatchingGoal(plan).getStatus());
        Goal example = destination.getGoals().stream().filter(goal -> goal instanceof LinkedGoal && goal.getTitle(null).startsWith("Example")).findFirst().get();
        assertTrue(((LinkedGoal) example).getSources().stream().allMatch(id -> destination.getGoals().stream().anyMatch(goal -> destination.metadata(goal).getId().equals(id))));
    }

    private static GrinderProfile empty() {
        GrinderProfile profile = new GrinderProfile(42);
        profile.replace(Collections.emptyList());
        return profile;
    }

    private static CustomGoal manual(GrinderProfile profile, String title, long current, long target) {
        CustomGoal goal = new CustomGoal(title, "", GoalCategory.OTHER, current, target);
        profile.add(goal);
        return goal;
    }
}
