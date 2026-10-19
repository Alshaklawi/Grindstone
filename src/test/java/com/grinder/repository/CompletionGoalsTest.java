package com.grinder.repository;

import com.google.gson.JsonObject;
import com.grinder.content.combatachievement.CombatAchievementData;
import com.grinder.content.combatachievement.CombatAchievementTier;
import com.grinder.content.diary.AchievementDiaryArea;
import com.grinder.content.diary.DiaryTier;
import com.grinder.content.goal.ClientCompletionGoal;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalStatus;
import com.grinder.content.goal.impl.achievementdiary.AchievementDiaryTierAreaGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementBossGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementTierGoal;
import com.grinder.content.goal.impl.quest.QuestGoal;
import com.grinder.content.goal.impl.quest.QuestSeriesGoal;
import com.grinder.content.quest.QuestSeries;
import com.grinder.ui.editor.CompletionGoalChoices;
import com.grinder.ui.editor.GoalEditorPanel;
import java.text.ParseException;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

import static org.junit.Assert.*;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class CompletionGoalsTest {
    @Test
    public void everyNewGoalSurvivesProfileReload() {
        GrinderProfile profile = emptyProfile();
        for (Map<String, Supplier<Goal>> choices : CompletionGoalChoices.create(null).values()) {
            for (Supplier<Goal> choice : choices.values()) {
                assertTrue(profile.add(choice.get()));
            }
        }
        assertTrue(profile.add(new CombatAchievementBossGoal(1, "Test boss")));
        GrinderProfile restored = TestProfiles.REPOSITORY.fromJson(TestProfiles.REPOSITORY.toJson(profile));
        assertEquals(profile.getGoals().size(), restored.getGoals().size());
        for (int i = 0; i < profile.getGoals().size(); i++) {
            ClientCompletionGoal before = (ClientCompletionGoal) profile.getGoals().get(i);
            ClientCompletionGoal after = (ClientCompletionGoal) restored.getGoals().get(i);
            assertEquals(before.getClass(), after.getClass());
            assertEquals(before.selectionKey(), after.selectionKey());
            assertEquals(before.getTitle(null), after.getTitle(null));
        }
    }
    @Test
    public void completionSelectionsDeduplicateWithoutMergingDifferentGoals() {
        GrinderProfile profile = emptyProfile();
        assertTrue(profile.add(new QuestGoal(Quest.COOKS_ASSISTANT)));
        assertFalse(profile.add(new QuestGoal(Quest.COOKS_ASSISTANT)));
        assertTrue(profile.add(new QuestGoal(Quest.DRAGON_SLAYER_I)));
        assertTrue(profile.add(new AchievementDiaryTierAreaGoal(AchievementDiaryArea.VARROCK, DiaryTier.EASY)));
        assertFalse(profile.add(new AchievementDiaryTierAreaGoal(AchievementDiaryArea.VARROCK, DiaryTier.EASY)));
        assertTrue(profile.add(new AchievementDiaryTierAreaGoal(AchievementDiaryArea.VARROCK, DiaryTier.HARD)));
        assertTrue(profile.add(new CombatAchievementBossGoal(1, "One")));
        assertFalse(profile.add(new CombatAchievementBossGoal(1, "Renamed one")));
        assertTrue(profile.add(new CombatAchievementBossGoal(2, "Two")));
    }
    @Test
    public void questStatusesIncludeStartedWithoutPretendingItIsComplete() {
        QuestGoal goal = new QuestGoal(Quest.COOKS_ASSISTANT);
        goal.refreshState(QuestState.NOT_STARTED);
        assertEquals(GoalStatus.NOT_STARTED, goal.getStatus());
        goal.refreshState(QuestState.IN_PROGRESS);
        assertEquals(GoalStatus.IN_PROGRESS, goal.getStatus());
        assertEquals(0, goal.getProgressCurrent());
        goal.refreshState(QuestState.FINISHED);
        assertEquals(GoalStatus.COMPLETE, goal.getStatus());
        assertFalse(goal.refreshState(QuestState.FINISHED));
        goal.refreshState(QuestState.NOT_STARTED);
        assertEquals(GoalStatus.NOT_STARTED, goal.getStatus());
    }
    @Test
    public void questSeriesCountsFinishedQuestsAndHandlesStartedFirstQuest() {
        QuestSeriesGoal goal = new QuestSeriesGoal(QuestSeries.ELF);
        goal.refreshStates(q -> q == Quest.PLAGUE_CITY ? QuestState.IN_PROGRESS : QuestState.NOT_STARTED);
        assertEquals(GoalStatus.IN_PROGRESS, goal.getStatus());
        assertEquals(0, goal.getProgressCurrent());
        goal.refreshStates(q -> q == Quest.PLAGUE_CITY ? QuestState.FINISHED : QuestState.NOT_STARTED);
        assertEquals(1, goal.getProgressCurrent());
        goal.refreshStates(q -> QuestState.FINISHED);
        assertEquals(QuestSeries.ELF.getQuests().size(), goal.getProgressMaximum());
        assertEquals(GoalStatus.COMPLETE, goal.getStatus());
    }
    @Test
    public void combatTierUsesPointsAndLiveThresholdNotTierTaskCount() {
        CombatAchievementTierGoal goal = new CombatAchievementTierGoal(CombatAchievementTier.EASY);
        goal.refreshValues(id -> id == VarbitID.CA_POINTS ? 32 : 35);
        assertEquals(32, goal.getProgressCurrent());
        assertEquals(35, goal.getProgressMaximum());
        assertEquals(GoalStatus.IN_PROGRESS, goal.getStatus());
        goal.refreshValues(id -> id == VarbitID.CA_POINTS ? 100 : 35);
        assertEquals(GoalStatus.COMPLETE, goal.getStatus());
        goal.refreshValues(id -> 0);
        assertFalse(goal.hasProgress());
        assertEquals(GoalStatus.NOT_STARTED, goal.getStatus());
    }
    @Test
    public void combatTaskBitsHandleSignBitBoundariesAndNewestVarp() {
        assertTrue(CombatAchievementData.isComplete(id -> id == VarPlayerID.CA_TASK_COMPLETED_0 ? Integer.MIN_VALUE : 0, 31));
        assertFalse(CombatAchievementData.isComplete(id -> id == VarPlayerID.CA_TASK_COMPLETED_0 ? Integer.MIN_VALUE : 0, 32));
        assertTrue(CombatAchievementData.isComplete(id -> id == VarPlayerID.CA_TASK_COMPLETED_1 ? 1 : 0, 32));
        assertTrue(CombatAchievementData.isComplete(id -> id == VarPlayerID.CA_TASK_COMPLETED_20 ? 1 : 0, 640));
        assertFalse(CombatAchievementData.isComplete(id -> -1, -1));
        assertFalse(CombatAchievementData.isComplete(id -> -1, 672));
    }
    @Test
    public void diaryFlagsAndLegacyKaramjaTaskCountsAreDistinct() {
        assertTrue(AchievementDiaryArea.VARROCK.isComplete(id -> id == VarbitID.VARROCK_DIARY_EASY_COMPLETE ? 1 : 0, DiaryTier.EASY));
        assertFalse(AchievementDiaryArea.VARROCK.isComplete(id -> id == VarbitID.VARROCK_EASY_COUNT ? 10 : 0, DiaryTier.EASY));
        assertFalse(AchievementDiaryArea.KARAMJA.isComplete(id -> id == VarbitID.KARAMJA_MED_COUNT ? 18 : 0, DiaryTier.MEDIUM));
        assertTrue(AchievementDiaryArea.KARAMJA.isComplete(id -> id == VarbitID.KARAMJA_MED_COUNT ? 19 : 0, DiaryTier.MEDIUM));
        assertEquals(12, AchievementDiaryArea.values().length);
    }
    @Test
    public void invalidCompletionSelectionIsSkippedRatherThanBreakingProfile() {
        GrinderProfile profile = emptyProfile();
        profile.add(new QuestGoal(Quest.COOKS_ASSISTANT));
        JsonObject json = TestProfiles.REPOSITORY.toJson(profile);
        json.getAsJsonArray("goals").get(0).getAsJsonObject().getAsJsonObject("data").remove("quest");
        assertTrue(TestProfiles.REPOSITORY.fromJson(json).getGoals().isEmpty());
    }
    @Test
    public void pickerCreatesEveryNewGoalTypeWithoutTargetSpinnerErrors() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Map<Integer, Map<String, Supplier<Goal>>> choices = CompletionGoalChoices.create(null);
            choices.get(13).put("Test boss", () -> new CombatAchievementBossGoal(1, "Test boss"));
            GoalEditorPanel form = new GoalEditorPanel(null, (query, callback) -> () -> {
            }, choices);
            JComboBox<?> types = (JComboBox<?>) ((JPanel) form.getComponent(0)).getComponent(1);
            try {
                for (int kind = 9; kind <= 15; kind++) {
                    types.setSelectedIndex(kind);
                    assertTrue(form.isReady());
                    Goal goal = form.createGoal();
                    assertTrue(goal instanceof ClientCompletionGoal);
                    assertFalse(goal.supportsTarget());
                    assertNotNull(((ClientCompletionGoal) goal).selectionKey());
                }
            } catch (ParseException exception) {
                throw new AssertionError(exception);
            } finally {
                form.stopSearch();
            }
        });
    }
    private GrinderProfile emptyProfile() {
        GrinderProfile profile = new GrinderProfile(42);
        profile.replace(Collections.emptyList());
        return profile;
    }
}
