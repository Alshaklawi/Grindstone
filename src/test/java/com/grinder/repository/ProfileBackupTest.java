package com.grinder.repository;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.impl.CustomGoal;
import java.util.Collections;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class ProfileBackupTest {
    private GrinderProfile completedProfile() {
        GrinderProfile profile = new GrinderProfile(42);
        CustomGoal goal = new CustomGoal("Routine", "Notes", GoalCategory.OTHER, 10, 10);
        profile.replace(Collections.singletonList(goal));
        profile.metadata(goal).observe(goal, "Routine", 1000);
        profile.metadata(goal).setPinned(true);
        profile.getCollectionLogHistory().record(ItemID.TWISTED_BOW, 1000);
        return profile;
    }
    @Test public void sameAccountBackupPreservesPinsHistoryAndManualProgress() {
        GrinderProfile original = completedProfile();
        GrinderProfile restored = GrinderRepository.importBackup(GrinderRepository.exportBackup(original), 42);
        assertEquals(10, restored.getGoals().get(0).getProgressCurrent());
        assertTrue(restored.metadata(restored.getGoals().get(0)).isPinned());
        assertEquals(1, restored.metadata(restored.getGoals().get(0)).getCompletions().size());
        assertTrue(restored.getCollectionLogHistory().contains(ItemID.TWISTED_BOW));
    }
    @Test public void transferringSetupDoesNotTransferAnotherCharactersProgress() {
        GrinderProfile restored = GrinderRepository.importBackup(GrinderRepository.exportBackup(completedProfile()), 99);
        assertEquals(99, restored.getHash());
        assertEquals(0, restored.getGoals().get(0).getProgressCurrent());
        assertTrue(restored.metadata(restored.getGoals().get(0)).isPinned());
        assertTrue(restored.metadata(restored.getGoals().get(0)).getCompletions().isEmpty());
        assertFalse(restored.getCollectionLogHistory().contains(ItemID.TWISTED_BOW));
    }
    @Test public void mergingRecoversMissingGoalsWithoutResettingEditedTargets() {
        GrinderProfile original = completedProfile();
        String backup = GrinderRepository.exportBackup(original);
        CustomGoal current = (CustomGoal) original.getGoals().get(0);
        current.setTarget(20);
        assertEquals(0, original.mergeBackup(GrinderRepository.importBackup(backup, 42)));
        assertEquals(20, current.getProgressMaximum());
        original.remove(current);
        assertEquals(1, original.mergeBackup(GrinderRepository.importBackup(backup, 42)));
        assertTrue(original.metadata(original.getGoals().get(0)).isPinned());
    }
    @Test public void unsupportedGoalTypesAndFutureBackupVersionsAreRejected() {
        com.google.gson.JsonObject json = GrinderRepository.toJson(completedProfile());
        json.addProperty("backupVersion", 2);
        try {
            GrinderRepository.importBackup(json.toString(), 42);
            fail("Future version must be rejected");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
        json.addProperty("backupVersion", 1);
        json.getAsJsonArray("goals").get(0).getAsJsonObject().addProperty("class", "java.lang.Runtime");
        try {
            GrinderRepository.importBackup(json.toString(), 42);
            fail("Unknown classes must be rejected");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }
}
