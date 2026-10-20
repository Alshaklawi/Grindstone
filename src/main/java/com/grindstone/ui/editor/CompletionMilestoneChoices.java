package com.grindstone.ui.editor;

import com.grindstone.content.collectionlog.CollectionLogPages;
import com.grindstone.content.combatachievement.CombatAchievementData;
import com.grindstone.content.combatachievement.CombatAchievementTier;
import com.grindstone.content.diary.AchievementDiaryArea;
import com.grindstone.content.diary.DiaryTier;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.impl.achievementdiary.AchievementDiaryTierAreaMilestone;
import com.grindstone.content.milestone.impl.achievementdiary.AchievementDiaryTierMilestone;
import com.grindstone.content.milestone.impl.collectionlog.CollectionLogEntryMilestone;
import com.grindstone.content.milestone.impl.combatachievement.CombatAchievementBossMilestone;
import com.grindstone.content.milestone.impl.combatachievement.CombatAchievementDifficultyMilestone;
import com.grindstone.content.milestone.impl.combatachievement.CombatAchievementTaskMilestone;
import com.grindstone.content.milestone.impl.combatachievement.CombatAchievementTierMilestone;
import com.grindstone.content.milestone.impl.quest.QuestMilestone;
import com.grindstone.content.milestone.impl.quest.QuestSeriesMilestone;
import com.grindstone.content.quest.QuestSeries;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.Quest;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class CompletionMilestoneChoices {
    private CompletionMilestoneChoices() {
    }
    public static Map<Integer, Map<String, Supplier<Milestone>>> create(Client client) {
        Map<Integer, Map<String, Supplier<Milestone>>> types = new LinkedHashMap<>();
        types.put(6, new TreeMap<>());
        types.put(17, new TreeMap<>());
        CombatAchievementData.tasks(client).forEach((id, task) -> types.get(17).put(task.getName() + " (" + task.getTier() + ")", () -> new CombatAchievementTaskMilestone(id, task.getName(), task.getDescription())));
        CollectionLogPages.discover(client).forEach((id, name) -> types.get(6).put(name, () -> new CollectionLogEntryMilestone(null, id, name)));
        for (int kind = 9; kind <= 15; kind++) {
            types.put(kind, kind == 12 || kind >= 14 ? new LinkedHashMap<>() : new TreeMap<>());
        }
        for (Quest quest : Quest.values()) {
            types.get(9).put(quest.getName(), () -> new QuestMilestone(quest));
        }
        for (QuestSeries series : QuestSeries.values()) {
            types.get(10).put(series.toString(), () -> new QuestSeriesMilestone(series));
        }
        for (DiaryTier tier : DiaryTier.values()) {
            types.get(12).put(tier.toString(), () -> new AchievementDiaryTierMilestone(tier));
            for (AchievementDiaryArea area : AchievementDiaryArea.values()) {
                types.get(11).put(area + " — " + tier, () -> new AchievementDiaryTierAreaMilestone(area, tier));
            }
        }
        for (CombatAchievementTier tier : CombatAchievementTier.values()) {
            types.get(14).put(tier.toString(), () -> new CombatAchievementTierMilestone(tier));
            types.get(15).put(tier.toString(), () -> new CombatAchievementDifficultyMilestone(tier));
        }
        if (client != null) {
            CombatAchievementData.tasksByBoss(client).forEach((boss, tasks) -> {
                String name = CombatAchievementData.bossName(client, boss);
                types.get(13).put(name, () -> new CombatAchievementBossMilestone(boss, name));
            });
        }
        return types;
    }
}
