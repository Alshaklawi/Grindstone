package com.grinder.repository;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.impl.TrackerGoal;
import com.grinder.content.goal.impl.experience.SkillExperienceGoal;
import com.grinder.content.goal.impl.experience.TotalExperienceGoal;
import com.grinder.content.goal.impl.level.CombatLevelGoal;
import com.grinder.content.goal.impl.level.SkillLevelGoal;
import com.grinder.content.goal.impl.level.TotalLevelGoal;
import com.grinder.content.tracker.TrackerEntry;
import java.util.Arrays;
import java.util.Collections;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class GoalManagementTest {
    @Test
    public void deletingAllGoalsSurvivesProfileReload() {
        GrinderProfile profile = TestProfiles.populated(42);
        assertFalse(profile.getGoals().isEmpty());
        profile.replace(Collections.emptyList());
        GrinderProfile restored = TestProfiles.REPOSITORY.fromJson(TestProfiles.REPOSITORY.toJson(profile));
        assertTrue(restored.getGoals().isEmpty());
        assertEquals(42, restored.getHash());
    }

    @Test
    public void editedTargetsSurviveSerializationForEveryEditableType() {
        GrinderProfile profile = TestProfiles.populated(42);
        profile.replace(Collections.emptyList());
        Goal[] goals = {new SkillLevelGoal(Skill.ATTACK), new SkillExperienceGoal(Skill.FISHING),
            new CombatLevelGoal(), new TotalLevelGoal(), new TotalExperienceGoal(),
            new TrackerGoal(TrackerEntry.values()[0], 5000)};
        long[] targets = {75, 2_000_000, 100, 1500, 4_000_000_000L, 250};
        for (int i = 0; i < goals.length; i++) {
            goals[i].setTarget(targets[i]);
            assertTrue(profile.add(goals[i]));
        }
        GrinderProfile restored = TestProfiles.REPOSITORY.fromJson(TestProfiles.REPOSITORY.toJson(profile));
        assertEquals(goals.length, restored.getGoals().size());
        for (int i = 0; i < goals.length; i++) {
            assertEquals(goals[i].getClass(), restored.getGoals().get(i).getClass());
            assertEquals(targets[i], restored.getGoals().get(i).getProgressMaximum());
        }
    }

    @Test
    public void oldGoalWithoutTargetUsesOriginalDefault() {
        GrinderProfile profile = TestProfiles.populated(42);
        profile.replace(Collections.singletonList(new SkillLevelGoal(Skill.ATTACK)));
        JsonObject json = TestProfiles.REPOSITORY.toJson(profile);
        json.getAsJsonArray("goals").get(0).getAsJsonObject().getAsJsonObject("data").remove("target");
        GrinderProfile restored = TestProfiles.REPOSITORY.fromJson(json);
        assertEquals(99, restored.getGoals().get(0).getProgressMaximum());
    }

    @Test
    public void goalsSavedBeforePackageReorganizationStillLoad() {
        GrinderProfile profile = TestProfiles.populated(42);
        profile.replace(Arrays.asList(new SkillLevelGoal(Skill.ATTACK), new SkillExperienceGoal(Skill.FISHING), new TotalLevelGoal(), new TotalExperienceGoal(), new CombatLevelGoal()));
        JsonObject json = TestProfiles.REPOSITORY.toJson(profile);
        for (JsonElement element : json.getAsJsonArray("goals")) {
            JsonObject entry = element.getAsJsonObject();
            String oldClass = SavedGoalTypes.resolve(entry.get("type").getAsString()).getName()
                    .replace(".impl.level.", ".impl.").replace(".impl.experience.", ".impl.");
            entry.addProperty("class", oldClass);
            entry.remove("type");
        }
        assertEquals(5, TestProfiles.REPOSITORY.fromJson(json).getGoals().size());
    }

    @Test
    public void addingExistingSkillFindsItInsteadOfDuplicatingIt() {
        GrinderProfile profile = TestProfiles.populated(42);
        profile.replace(Collections.emptyList());
        SkillLevelGoal attack = new SkillLevelGoal(Skill.ATTACK);
        assertTrue(profile.add(attack));
        assertFalse(profile.add(new SkillLevelGoal(Skill.ATTACK)));
        assertSame(attack, profile.findMatchingGoal(new SkillLevelGoal(Skill.ATTACK)));
        assertTrue(profile.add(new SkillLevelGoal(Skill.DEFENCE)));
        assertTrue(profile.add(new SkillExperienceGoal(Skill.ATTACK)));
        assertEquals(3, profile.getGoals().size());
    }

    @Test
    public void restoringDefaultsIsExplicitAndDoesNotResetEditedTargets() {
        GrinderProfile profile = TestProfiles.populated(42);
        Goal goal = profile.getGoals().stream().filter(g -> g instanceof SkillLevelGoal).findFirst().get();
        goal.setTarget(75);
        Goal deleted = profile.getGoals().get(0);
        int count = profile.getGoals().size();
        assertTrue(profile.remove(deleted));
        assertEquals(count - 1, profile.getGoals().size());
        TestProfiles.populate(profile);
        TestProfiles.populate(profile);
        assertEquals(count, profile.getGoals().size());
        assertEquals(75, goal.getProgressMaximum());
    }

    @Test(expected = IllegalArgumentException.class)
    public void levelTargetRejectsLevelsAboveCap() {
        new SkillLevelGoal(Skill.ATTACK).setTarget(100);
    }
}
