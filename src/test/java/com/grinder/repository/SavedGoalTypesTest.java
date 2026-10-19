package com.grinder.repository;
import com.google.gson.JsonObject;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.impl.collectionlog.CollectionLogEntryGoal;
import com.grinder.content.goal.impl.item.PossessedItemGoal;
import com.grinder.ui.editor.CompletionGoalChoices;
import java.util.Collections;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class SavedGoalTypesTest {
    @Test public void newlySavedProfilesContainOnlyStableTypeTagsNotJavaClassNames() {
        String json = TestProfiles.REPOSITORY.exportBackup(new GrinderProfile(42));
        assertTrue(json.contains("\"type\""));
        assertFalse(json.contains("\"class\""));
        assertFalse(json.contains("com.grinder."));
    }
    @Test public void renamedGoalsKeepTheirOriginalSavedIdentifiers() {
        PossessedItemGoal goal = new PossessedItemGoal(ItemID.COINS, 1_500_000_000L);
        assertEquals("owned-item", SavedGoalTypes.nameOf(goal));
        assertSame(PossessedItemGoal.class, SavedGoalTypes.resolve("com.grinder.content.goal.impl.item.PossesedItemGoal"));
        assertSame(PossessedItemGoal.class, SavedGoalTypes.resolve(SavedGoalTypes.nameOf(goal)));
        assertSame(PossessedItemGoal.class, SavedGoalTypes.resolve(PossessedItemGoal.class.getName()));
        assertSame(CollectionLogEntryGoal.class, SavedGoalTypes.resolve("com.grinder.content.goal.impl.colletion_log.CollectionLogEntryGoal"));
    }
    @Test public void currentAndLegacyClassNamesLoadTheSameSelections() {
        GrinderProfile profile = new GrinderProfile(42);
        CompletionGoalChoices.create(null).values().forEach(choices -> choices.values().forEach(factory -> profile.add(factory.get())));
        profile.add(new PossessedItemGoal(ItemID.COINS, 3_000_000_000L));
        JsonObject json = TestProfiles.REPOSITORY.toJson(profile);
        for (int i = 0; i < profile.getGoals().size(); i++) {
            Goal goal = profile.getGoals().get(i);
            assertSame(goal.getClass(), SavedGoalTypes.resolve(SavedGoalTypes.nameOf(goal)));
            json.getAsJsonArray("goals").get(i).getAsJsonObject().addProperty("class", goal.getClass().getName());
        }
        GrinderProfile restored = TestProfiles.REPOSITORY.fromJson(json);
        assertEquals(profile.getGoals().size(), restored.getGoals().size());
        for (int i = 0; i < profile.getGoals().size(); i++) {
            assertEquals(profile.getGoals().get(i).getClass(), restored.getGoals().get(i).getClass());
        }
    }
    @Test public void arbitraryClassNamesRemainBlocked() {
        assertNull(SavedGoalTypes.resolve("java.lang.Runtime"));
        assertNull(SavedGoalTypes.resolve("com.example.ExamplePlugin"));
        GrinderProfile profile = new GrinderProfile(42);
        profile.replace(Collections.singletonList(new PossessedItemGoal(ItemID.COINS)));
        JsonObject json = TestProfiles.REPOSITORY.toJson(profile);
        json.getAsJsonArray("goals").get(0).getAsJsonObject().addProperty("class", "java.lang.Runtime");
        assertTrue(TestProfiles.REPOSITORY.fromJson(json).getGoals().isEmpty());
    }
}
