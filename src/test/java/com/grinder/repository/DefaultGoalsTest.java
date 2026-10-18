package com.grinder.repository;

import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.impl.CustomGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementBossGoal;
import com.grinder.content.goal.impl.item.PossessedItemGoal;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class DefaultGoalsTest {
    @Test public void defaultsIncludeEveryRegisteredTrackerType() {
        GrinderProfile profile = new GrinderProfile(42);
        Set<Class<?>> represented = profile.getGoals().stream().map(Goal::getClass).collect(Collectors.toSet());
        assertEquals(SavedGoalTypes.registeredTypes(), represented);
        assertTrue(profile.getGoals().stream().anyMatch(goal -> goal instanceof PossessedItemGoal && ((PossessedItemGoal) goal).getItem() == ItemID.COINS && goal.getProgressMaximum() == 1_500_000_000L));
        assertTrue(profile.getGoals().stream().anyMatch(goal -> goal instanceof PossessedItemGoal && goal.getProgressMaximum() == 1));
    }

    @Test public void restoringDefaultsPreservesEditedExamplesAndDoesNotDuplicateThem() {
        GrinderProfile profile = new GrinderProfile(42);
        CustomGoal custom = (CustomGoal) profile.getGoals().stream().filter(goal -> goal instanceof CustomGoal).findFirst().get();
        custom.update("My own routine", "Keep these edits", GoalCategory.OTHER, 2, 20);
        PossessedItemGoal coins = (PossessedItemGoal) profile.findMatchingGoal(new PossessedItemGoal(ItemID.COINS));
        coins.setTarget(3_000_000_000L);
        int size = profile.getGoals().size();
        profile.ensureDefaultGoals(null);
        profile.ensureDefaultGoals(null);
        assertEquals(size, profile.getGoals().size());
        assertEquals("My own routine", custom.getTitle(null));
        assertEquals(2, custom.getProgressCurrent());
        assertEquals(20, custom.getProgressMaximum());
        assertEquals(3_000_000_000L, coins.getAmount());
    }

    @Test public void namedBossExampleDeduplicatesAgainstClientResolvedBossGoals() {
        GrinderProfile profile = new GrinderProfile(42);
        CombatAchievementBossGoal barrows = new CombatAchievementBossGoal("Barrows");
        assertFalse(profile.add(barrows));
        profile.remove(profile.findMatchingGoal(barrows));
        assertTrue(profile.add(new CombatAchievementBossGoal(123, "Barrows")));
        int size = profile.getGoals().size();
        profile.ensureDefaultGoals(null);
        assertEquals(size, profile.getGoals().size());
    }

    @Test public void everyDefaultTypeSurvivesSavingAndReloadingWithoutExtraExamples() {
        GrinderProfile profile = new GrinderProfile(42);
        GrinderProfile restored = GrinderRepository.fromJson(GrinderRepository.toJson(profile));
        int size = profile.getGoals().size();
        assertEquals(size, restored.getGoals().size());
        restored.ensureDefaultGoals(null);
        assertEquals(size, restored.getGoals().size());
    }
}
