package com.grindstone.ui.editor;

import com.grindstone.content.collectionlog.CollectionLogHistory;
import com.grindstone.content.collectionlog.CollectionLogPages;
import com.grindstone.content.combatachievement.CombatAchievementData;
import com.grindstone.content.combatachievement.CombatAchievementTier;
import com.grindstone.content.diary.AchievementDiaryArea;
import com.grindstone.content.diary.DiaryTier;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.content.milestone.impl.TrackerMilestone;
import com.grindstone.content.tracker.TrackerEntry;
import com.grindstone.content.milestone.impl.achievementdiary.AchievementDiaryTierAreaMilestone;
import com.grindstone.content.milestone.impl.achievementdiary.AchievementDiaryTierMilestone;
import com.grindstone.content.milestone.impl.collectionlog.CollectionLogEntryMilestone;
import com.grindstone.content.milestone.impl.combatachievement.CombatAchievementBossMilestone;
import com.grindstone.content.milestone.impl.combatachievement.CombatAchievementDifficultyMilestone;
import com.grindstone.content.milestone.impl.combatachievement.CombatAchievementTaskMilestone;
import com.grindstone.content.milestone.impl.combatachievement.CombatAchievementTierMilestone;
import com.grindstone.content.milestone.impl.experience.BaseExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.SessionExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.SkillExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.TotalExperienceMilestone;
import com.grindstone.content.milestone.impl.level.BaseLevelMilestone;
import com.grindstone.content.milestone.impl.level.CombatLevelMilestone;
import com.grindstone.content.milestone.impl.level.SkillLevelMilestone;
import com.grindstone.content.milestone.impl.level.TotalLevelMilestone;
import com.grindstone.content.milestone.impl.quest.QuestMilestone;
import com.grindstone.content.milestone.impl.quest.QuestSeriesMilestone;
import com.grindstone.content.quest.QuestSeries;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class CompletionMilestoneChoices {
    private CompletionMilestoneChoices() {
    }
    public static Map<Integer, Map<String, Supplier<Milestone>>> available(Client client, CollectionLogHistory history) {
        Map<Integer, Map<String, Supplier<Milestone>>> types = create(client);
        types.put(0, new LinkedHashMap<>());
        types.put(1, new LinkedHashMap<>());
        for (Skill skill : Skill.values()) {
            types.get(0).put(skill.getName(), () -> new SkillLevelMilestone(skill));
            types.get(1).put(skill.getName(), () -> new SkillExperienceMilestone(skill));
        }
        types.put(2, new LinkedHashMap<>());
        types.get(2).put("Combat level", CombatLevelMilestone::new);
        types.put(3, new LinkedHashMap<>());
        types.get(3).put("Total level", TotalLevelMilestone::new);
        types.put(4, new LinkedHashMap<>());
        types.get(4).put("Total experience", TotalExperienceMilestone::new);
        types.put(5, new LinkedHashMap<>());
        for (TrackerEntry entry : TrackerEntry.values()) {
            types.get(5).put(entry.getDisplayName(), () -> new TrackerMilestone(entry, entry.getDefaultTarget()));
        }
        types.put(18, new LinkedHashMap<>());
        types.get(18).put("Base level", BaseLevelMilestone::new);
        types.put(19, new LinkedHashMap<>());
        types.get(19).put("Base experience", BaseExperienceMilestone::new);
        // Capture readings on the client thread before the editor moves to the EDT.
        types.forEach((kind, choices) -> {
            choices.replaceAll((name, factory) -> {
                Milestone goal = factory.get();
                if (goal instanceof CollectionLogEntryMilestone) {
                    ((CollectionLogEntryMilestone) goal).setHistory(history);
                }
                goal.refresh(client);
                if (goal instanceof CollectionLogEntryMilestone) {
                    goal.prepare(client, null);
                }
                return () -> goal;
            });
            choices.values().removeIf(factory -> {
                Milestone goal = factory.get();
                return !hasRemainingTarget(goal);
            });
        });
        Map<String, Supplier<Milestone>> sessions = new LinkedHashMap<>();
        for (Skill skill : Skill.values()) {
            if (client.getSkillExperience(skill) < 200_000_000) {
                sessions.put(skill.getName(), () -> new SessionExperienceMilestone(skill));
            }
        }
        if (!sessions.isEmpty()) {
            Map<String, Supplier<Milestone>> ordered = new LinkedHashMap<>();
            ordered.put("All skills (total gains)", SessionExperienceMilestone::new);
            ordered.putAll(sessions);
            sessions = ordered;
        }
        types.put(20, sessions);
        return types;
    }

    public static boolean hasRemainingTarget(Milestone goal) {
        if (!goal.supportsTarget()) {
            return goal.getStatus() != MilestoneStatus.COMPLETE;
        }
        long cap = ((Number) MilestoneTargetModels.create(goal).getMaximum()).longValue();
        return goal.getProgressCurrent() < cap;
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
