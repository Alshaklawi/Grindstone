package com.grinder.ui.editor;

import com.grinder.content.combatachievement.CombatAchievementData;
import com.grinder.content.combatachievement.CombatAchievementTier;
import com.grinder.content.diary.AchievementDiaryArea;
import com.grinder.content.diary.DiaryTier;
import com.grinder.content.quest.QuestSeries;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.impl.quest.QuestGoal;
import com.grinder.content.goal.impl.quest.QuestSeriesGoal;
import com.grinder.content.goal.impl.achievementdiary.AchievementDiaryTierGoal;
import com.grinder.content.goal.impl.achievementdiary.AchievementDiaryTierAreaGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementBossGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementTierGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementDifficultyGoal;
import java.util.Map;
import java.util.TreeMap;
import java.util.LinkedHashMap;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.Quest;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class CompletionGoalChoices {
    private CompletionGoalChoices() {}
    public static Map<Integer, Map<String, Supplier<Goal>>> create(Client client) {
        Map<Integer, Map<String, Supplier<Goal>>> types = new LinkedHashMap<>();
        types.put(6, new TreeMap<>());
        types.put(17, new TreeMap<>());
        com.grinder.content.combatachievement.CombatAchievementData.tasks(client).forEach((id, task) -> types.get(17).put(task.getName() + " (" + task.getTier() + ")", () -> new com.grinder.content.goal.impl.combatachievement.CombatAchievementTaskGoal(id, task.getName(), task.getDescription())));
        com.grinder.content.collectionlog.CollectionLogPages.discover(client).forEach((id, name) -> types.get(6).put(name, () -> new com.grinder.content.goal.impl.collectionlog.CollectionLogEntryGoal(null, id, name)));
        for (int kind = 9; kind <= 15; kind++)
        { types.put(kind, kind == 12 || kind >= 14 ? new LinkedHashMap<>() : new TreeMap<>()); }
        for (Quest quest : Quest.values())
        { types.get(9).put(quest.getName(), () -> new QuestGoal(quest)); }
        for (QuestSeries series : QuestSeries.values())
        { types.get(10).put(series.toString(), () -> new QuestSeriesGoal(series)); }
        for (DiaryTier tier : DiaryTier.values()) {
            types.get(12).put(tier.toString(), () -> new AchievementDiaryTierGoal(tier));
            for (AchievementDiaryArea area : AchievementDiaryArea.values())
            { types.get(11).put(area + " — " + tier, () -> new AchievementDiaryTierAreaGoal(area, tier)); }
        }
        for (CombatAchievementTier tier : CombatAchievementTier.values()) {
            types.get(14).put(tier.toString(), () -> new CombatAchievementTierGoal(tier));
            types.get(15).put(tier.toString(), () -> new CombatAchievementDifficultyGoal(tier));
        }
        if (client != null) {
            CombatAchievementData.tasksByBoss(client).forEach((boss, tasks) -> {
                String name = CombatAchievementData.bossName(client, boss);
                types.get(13).put(name, () -> new CombatAchievementBossGoal(boss, name));
            });
        }
        return types;
    }
}
