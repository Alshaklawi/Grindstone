package com.grinder.repository;

import com.google.gson.JsonObject;
import com.grinder.content.collectionlog.CollectionLogEntry;
import com.grinder.content.collectionlog.CollectionLogHistory;
import com.grinder.content.goal.GoalStatus;
import com.grinder.content.goal.impl.collectionlog.CollectionLogEntryGoal;
import com.grinder.content.goal.impl.collectionlog.CollectionLogItemGoal;
import java.util.Arrays;
import java.util.Collections;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class CollectionLogHistoryTest {
    @Test
    public void repeatedObservationsKeepEarliestTime() {
        CollectionLogHistory history = new CollectionLogHistory();
        history.record(ItemID.DRAGON_PICKAXE, 1000);
        history.record(ItemID.DRAGON_PICKAXE, 2000);
        assertEquals(Long.valueOf(1000), history.firstSeenAt(ItemID.DRAGON_PICKAXE));
        assertTrue(history.firstSeenText(ItemID.DRAGON_PICKAXE).startsWith("First seen:"));
    }

    @Test
    public void observationsSurviveProfileReloadAndAttachToSingleItemGoals() {
        GrinderProfile profile = new GrinderProfile(42);
        profile.getCollectionLogHistory().record(ItemID.DRAGON_PICKAXE, 1000);
        profile.replace(Arrays.asList(new CollectionLogItemGoal(ItemID.DRAGON_PICKAXE, 1), new CollectionLogEntryGoal(null, CollectionLogEntry.VETION_AND_CALVARION)));
        GrinderProfile restored = TestProfiles.REPOSITORY.fromJson(TestProfiles.REPOSITORY.toJson(profile));
        assertEquals(2, restored.getGoals().size());
        assertEquals(Long.valueOf(1000), restored.getCollectionLogHistory().firstSeenAt(ItemID.DRAGON_PICKAXE));
        CollectionLogItemGoal goal = (CollectionLogItemGoal) restored.getGoals().get(0);
        assertEquals(GoalStatus.COMPLETE, goal.getStatus());
        assertEquals(1, goal.getProgressCurrent());
        assertEquals(restored.getCollectionLogHistory().firstSeenText(ItemID.DRAGON_PICKAXE), goal.getDescription());
    }

    @Test
    public void deletingGoalDoesNotEraseObservedHistory() {
        GrinderProfile profile = new GrinderProfile(42);
        profile.getCollectionLogHistory().record(ItemID.DRAGON_PICKAXE, 1000);
        profile.replace(Collections.emptyList());
        GrinderProfile restored = TestProfiles.REPOSITORY.fromJson(TestProfiles.REPOSITORY.toJson(profile));
        assertTrue(restored.getGoals().isEmpty());
        CollectionLogItemGoal newGoal = new CollectionLogItemGoal(ItemID.DRAGON_PICKAXE, 1);
        assertTrue(restored.add(newGoal));
        assertEquals(GoalStatus.COMPLETE, newGoal.getStatus());
        assertTrue(newGoal.getDescription().startsWith("First seen:"));
    }

    @Test
    public void historyIsSeparateForEachPlayer() {
        GrinderProfile first = new GrinderProfile(42);
        GrinderProfile second = new GrinderProfile(43);
        first.getCollectionLogHistory().record(ItemID.DRAGON_PICKAXE, 1000);
        assertFalse(second.getCollectionLogHistory().contains(ItemID.DRAGON_PICKAXE));
    }

    @Test
    public void legacyProfilesDoNotInventUnlockDates() {
        GrinderProfile profile = new GrinderProfile(42);
        JsonObject json = TestProfiles.REPOSITORY.toJson(profile);
        json.remove("collectionLogFirstSeen");
        GrinderProfile restored = TestProfiles.REPOSITORY.fromJson(json);
        assertTrue(restored.getCollectionLogHistory().snapshot().isEmpty());
        assertEquals("Not yet observed in your collection log", restored.getCollectionLogHistory().firstSeenText(ItemID.DRAGON_PICKAXE));
    }
}
