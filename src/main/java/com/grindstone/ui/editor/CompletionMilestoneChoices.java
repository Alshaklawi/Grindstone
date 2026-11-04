package com.grindstone.ui.editor;

import com.grindstone.content.milestone.impl.collectionlog.CollectionLogTotalMilestone;
import com.grindstone.content.collectionlog.CollectionLogHistory;
import com.grindstone.content.collectionlog.CollectionLogPages;
import com.grindstone.content.combatachievement.CombatAchievementData;
import com.grindstone.content.combatachievement.CombatAchievementTier;
import com.grindstone.content.diary.AchievementDiaryArea;
import com.grindstone.content.diary.DiaryTier;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.content.milestone.impl.TrackerMilestone;
import com.grindstone.content.tracker.TrackerEntry;
import com.grindstone.content.milestone.impl.achievementdiary.AchievementDiaryTierAreaMilestone;
import com.grindstone.content.milestone.impl.achievementdiary.AchievementDiaryTierMilestone;
import com.grindstone.content.milestone.impl.collectionlog.CollectionLogEntryMilestone;
import com.grindstone.content.milestone.impl.collectionlog.CollectionLogTabMilestone;
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
import java.util.EnumSet;
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
    public static Map<MilestoneType, Map<String, Supplier<Milestone>>> getAvailable(Client client, CollectionLogHistory history) {
        return getSnapshot(client, history, true);
    }

    public static Map<MilestoneType, Map<String, Supplier<Milestone>>> getForEditing(Client client, CollectionLogHistory history) {
        return getSnapshot(client, history, false);
    }

    private static Map<MilestoneType, Map<String, Supplier<Milestone>>> getSnapshot(Client client, CollectionLogHistory history, boolean unfinishedOnly) {
        Map<MilestoneType, Map<String, Supplier<Milestone>>> types = create(client);
        types.put(MilestoneType.SKILL_LEVEL, new LinkedHashMap<>());
        types.put(MilestoneType.SKILL_EXPERIENCE, new LinkedHashMap<>());
        for (Skill skill : Skill.values()) {
            types.get(MilestoneType.SKILL_LEVEL).put(skill.getName(), () -> new SkillLevelMilestone(skill));
            types.get(MilestoneType.SKILL_EXPERIENCE).put(skill.getName(), () -> new SkillExperienceMilestone(skill));
        }
        types.put(MilestoneType.COMBAT_LEVEL, new LinkedHashMap<>());
        types.get(MilestoneType.COMBAT_LEVEL).put("Combat level", CombatLevelMilestone::new);
        types.put(MilestoneType.TOTAL_LEVEL, new LinkedHashMap<>());
        types.get(MilestoneType.TOTAL_LEVEL).put("Total level", TotalLevelMilestone::new);
        types.put(MilestoneType.TOTAL_EXPERIENCE, new LinkedHashMap<>());
        types.get(MilestoneType.TOTAL_EXPERIENCE).put("Total experience", TotalExperienceMilestone::new);
        types.put(MilestoneType.BASE_LEVEL, new LinkedHashMap<>());
        types.get(MilestoneType.BASE_LEVEL).put("Base level", BaseLevelMilestone::new);
        types.put(MilestoneType.BASE_EXPERIENCE, new LinkedHashMap<>());
        types.get(MilestoneType.BASE_EXPERIENCE).put("Base experience", BaseExperienceMilestone::new);
        // Capture readings on the client thread before the editor moves to the EDT.
        types.forEach((kind, choices) -> {
            choices.replaceAll((name, factory) -> {
                Milestone goal = factory.get();
                if (goal instanceof CollectionLogTabMilestone) {
                    ((CollectionLogTabMilestone) goal).setHistory(history);
                }
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
                return unfinishedOnly && !hasRemainingTarget(goal);
            });
        });
        Map<String, Supplier<Milestone>> sessions = new LinkedHashMap<>();
        for (Skill skill : Skill.values()) {
            if (!unfinishedOnly || client.getSkillExperience(skill) < 200_000_000) {
                sessions.put(skill.getName(), () -> new SessionExperienceMilestone(skill));
            }
        }
        if (!sessions.isEmpty()) {
            Map<String, Supplier<Milestone>> ordered = new LinkedHashMap<>();
            ordered.put("All skills (total gains)", SessionExperienceMilestone::new);
            ordered.putAll(sessions);
            sessions = ordered;
        }
        types.put(MilestoneType.SESSION_EXPERIENCE, sessions);
        return types;
    }

    public static boolean hasRemainingTarget(Milestone goal) {
        if (!goal.supportsTarget()) {
            return goal.getStatus() != MilestoneStatus.COMPLETE;
        }
        long cap = ((Number) MilestoneTargetModels.create(goal).getMaximum()).longValue();
        return goal.getProgressCurrent() < cap;
    }

    public static boolean isActivityType(MilestoneType kind) {
        return kind.isActivity();
    }

    private static MilestoneType getActivityType(MilestoneCategory category) {
        switch (category) {
            case BOSSING:
                return MilestoneType.BOSS_KILLS;
            case RAIDS:
                return MilestoneType.RAID_COMPLETIONS;
            case SLAYER:
                return MilestoneType.SLAYER_COUNTER;
            case CLUES:
                return MilestoneType.CLUE_COMPLETIONS;
            case MINIGAMES:
                return MilestoneType.MINIGAME_COUNTER;
            case SKILLING:
                return MilestoneType.SKILLING_ACTIVITY;
            case QUEST:
                return MilestoneType.QUEST_SPEEDRUN;
            default:
                throw new IllegalArgumentException("Unsupported activity category: " + category);
        }
    }

    public static Map<MilestoneType, Map<String, Supplier<Milestone>>> create(Client client) {
        Map<MilestoneType, Map<String, Supplier<Milestone>>> types = new LinkedHashMap<>();
        types.put(MilestoneType.BOSS_KILLS, new LinkedHashMap<>());
        for (MilestoneType kind : EnumSet.of(MilestoneType.RAID_COMPLETIONS, MilestoneType.SLAYER_COUNTER, MilestoneType.CLUE_COMPLETIONS, MilestoneType.MINIGAME_COUNTER, MilestoneType.SKILLING_ACTIVITY, MilestoneType.QUEST_SPEEDRUN)) {
            types.put(kind, new LinkedHashMap<>());
        }
        for (TrackerEntry entry : TrackerEntry.values()) {
            if (!entry.isSelectable()) {
                continue;
            }
            types.get(getActivityType(entry.getCategory())).put(entry.getDisplayName(), () -> new TrackerMilestone(entry, entry.getDefaultTarget()));
        }
        types.put(MilestoneType.COLLECTION_TAB, new LinkedHashMap<>());
        CollectionLogPages.getTabs(client).forEach((id, name) -> types.get(MilestoneType.COLLECTION_TAB).put(name, () -> new CollectionLogTabMilestone(id, name)));
        types.put(MilestoneType.COLLECTION_TOTAL, new LinkedHashMap<>());
        types.get(MilestoneType.COLLECTION_TOTAL).put("Total unlocks", CollectionLogTotalMilestone::new);
        types.put(MilestoneType.COLLECTION_PAGE, new TreeMap<>());
        types.put(MilestoneType.COMBAT_TASK, new TreeMap<>());
        CombatAchievementData.getTasks(client).forEach((id, task) -> types.get(MilestoneType.COMBAT_TASK).put(task.getName() + " (" + task.getTier() + ")", () -> new CombatAchievementTaskMilestone(id, task.getName(), task.getDescription())));
        CollectionLogPages.discover(client).forEach((id, name) -> types.get(MilestoneType.COLLECTION_PAGE).put(name, () -> new CollectionLogEntryMilestone(null, id, name)));
        for (MilestoneType kind : EnumSet.of(MilestoneType.QUEST, MilestoneType.QUEST_SERIES, MilestoneType.DIARY_REGION, MilestoneType.DIARY_TIER, MilestoneType.COMBAT_BOSS, MilestoneType.COMBAT_TIER, MilestoneType.COMBAT_DIFFICULTY)) {
            types.put(kind, kind.isTier() ? new LinkedHashMap<>() : new TreeMap<>());
        }
        for (Quest quest : Quest.values()) {
            types.get(MilestoneType.QUEST).put(quest.getName(), () -> new QuestMilestone(quest));
        }
        for (QuestSeries series : QuestSeries.values()) {
            types.get(MilestoneType.QUEST_SERIES).put(series.toString(), () -> new QuestSeriesMilestone(series));
        }
        for (DiaryTier tier : DiaryTier.values()) {
            types.get(MilestoneType.DIARY_TIER).put(tier.toString(), () -> new AchievementDiaryTierMilestone(tier));
            for (AchievementDiaryArea area : AchievementDiaryArea.values()) {
                types.get(MilestoneType.DIARY_REGION).put(area + " — " + tier, () -> new AchievementDiaryTierAreaMilestone(area, tier));
            }
        }
        for (CombatAchievementTier tier : CombatAchievementTier.values()) {
            types.get(MilestoneType.COMBAT_TIER).put(tier.toString(), () -> new CombatAchievementTierMilestone(tier));
            types.get(MilestoneType.COMBAT_DIFFICULTY).put(tier.toString(), () -> new CombatAchievementDifficultyMilestone(tier));
        }
        if (client != null) {
            CombatAchievementData.getTasksByBoss(client).forEach((boss, tasks) -> {
                String name = CombatAchievementData.getBossName(client, boss);
                types.get(MilestoneType.COMBAT_BOSS).put(name, () -> new CombatAchievementBossMilestone(boss, name));
            });
        }
        return types;
    }
}
