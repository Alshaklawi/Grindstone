package com.grindstone.repository;
import com.google.gson.JsonObject;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.impl.item.PossessedItemMilestone;
import com.grindstone.ui.editor.CompletionMilestoneChoices;
import java.util.Collections;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class SavedMilestoneTypesTest {
    @Test
    public void newlySavedProfilesContainOnlyStableTypeTagsNotJavaClassNames() {
        String json = TestProfiles.REPOSITORY.exportBackup(TestProfiles.populated(42));
        assertTrue(json.contains("\"type\""));
        assertFalse(json.contains("\"class\""));
        assertFalse(json.contains("com.grindstone."));
    }
    @Test
    public void goalsUseStableTagsWithoutClassNameAliases() {
        PossessedItemMilestone goal = new PossessedItemMilestone(ItemID.COINS, 1_500_000_000L);
        assertEquals("owned-item", SavedMilestoneTypes.nameOf(goal));
        assertSame(PossessedItemMilestone.class, SavedMilestoneTypes.resolve(SavedMilestoneTypes.nameOf(goal)));
        assertNull(SavedMilestoneTypes.resolve(PossessedItemMilestone.class.getName()));
    }
    @Test
    public void stableTagsRestoreTheSameSelections() {
        GrindstoneProfile profile = TestProfiles.populated(42);
        CompletionMilestoneChoices.create(null).values().forEach(choices -> choices.values().forEach(factory -> profile.add(factory.get())));
        profile.add(new PossessedItemMilestone(ItemID.COINS, 3_000_000_000L));
        JsonObject json = TestProfiles.REPOSITORY.toJson(profile);
        for (int i = 0; i < profile.getGoals().size(); i++) {
            Milestone goal = profile.getGoals().get(i);
            assertSame(goal.getClass(), SavedMilestoneTypes.resolve(SavedMilestoneTypes.nameOf(goal)));
        }
        GrindstoneProfile restored = TestProfiles.REPOSITORY.fromJson(json);
        assertEquals(profile.getGoals().size(), restored.getGoals().size());
        for (int i = 0; i < profile.getGoals().size(); i++) {
            assertEquals(profile.getGoals().get(i).getClass(), restored.getGoals().get(i).getClass());
        }
    }
    @Test
    public void arbitraryClassNamesRemainBlocked() {
        assertNull(SavedMilestoneTypes.resolve("java.lang.Runtime"));
        assertNull(SavedMilestoneTypes.resolve("com.example.ExamplePlugin"));
        GrindstoneProfile profile = TestProfiles.populated(42);
        profile.replace(Collections.singletonList(new PossessedItemMilestone(ItemID.COINS)));
        JsonObject json = TestProfiles.REPOSITORY.toJson(profile);
        json.getAsJsonArray("goals").get(0).getAsJsonObject().addProperty("class", "java.lang.Runtime");
        assertTrue(TestProfiles.REPOSITORY.fromJson(json).getGoals().isEmpty());
    }
}
