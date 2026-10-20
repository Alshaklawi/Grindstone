package com.grindstone.repository;
import com.grindstone.content.milestone.impl.collectionlog.CollectionLogTotalMilestone;
import com.google.gson.JsonObject;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.impl.ChecklistMilestone;
import com.grindstone.content.milestone.impl.CustomMilestone;
import com.grindstone.content.milestone.impl.LinkedMilestone;
import com.grindstone.content.milestone.impl.TrackerMilestone;
import com.grindstone.content.milestone.impl.achievementdiary.AchievementDiaryTierAreaMilestone;
import com.grindstone.content.milestone.impl.achievementdiary.AchievementDiaryTierMilestone;
import com.grindstone.content.milestone.impl.collectionlog.CollectionLogEntryMilestone;
import com.grindstone.content.milestone.impl.collectionlog.CollectionLogItemMilestone;
import com.grindstone.content.milestone.impl.combatachievement.CombatAchievementBossMilestone;
import com.grindstone.content.milestone.impl.combatachievement.CombatAchievementDifficultyMilestone;
import com.grindstone.content.milestone.impl.combatachievement.CombatAchievementTaskMilestone;
import com.grindstone.content.milestone.impl.combatachievement.CombatAchievementTierMilestone;
import com.grindstone.content.milestone.impl.experience.BaseExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.SessionExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.SkillExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.TotalExperienceMilestone;
import com.grindstone.content.milestone.impl.item.PossessedItemMilestone;
import com.grindstone.content.milestone.impl.level.BaseLevelMilestone;
import com.grindstone.content.milestone.impl.level.CombatLevelMilestone;
import com.grindstone.content.milestone.impl.level.SkillLevelMilestone;
import com.grindstone.content.milestone.impl.level.TotalLevelMilestone;
import com.grindstone.content.milestone.impl.quest.QuestMilestone;
import com.grindstone.content.milestone.impl.quest.QuestSeriesMilestone;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
final class SavedMilestoneTypes {
    private static final Map<String, Class<? extends Milestone>> TYPES = createTypes();
    private SavedMilestoneTypes() {
    }
    private static Map<String, Class<? extends Milestone>> createTypes() {
        Map<String, Class<? extends Milestone>> types = new LinkedHashMap<>();
        register(types, TrackerMilestone.class, "activity");
        register(types, CustomMilestone.class, "custom");
        register(types, ChecklistMilestone.class, "manual-checklist");
        register(types, LinkedMilestone.class, "linked-goal");
        register(types, SkillLevelMilestone.class, "skill-level");
        register(types, CombatLevelMilestone.class, "combat-level");
        register(types, TotalLevelMilestone.class, "total-level");
        register(types, BaseLevelMilestone.class, "base-level");
        register(types, BaseExperienceMilestone.class, "base-xp");
        register(types, SessionExperienceMilestone.class, "session-xp");
        register(types, SkillExperienceMilestone.class, "skill-xp");
        register(types, TotalExperienceMilestone.class, "total-xp");
        register(types, CollectionLogEntryMilestone.class, "collection-page");
        register(types, CollectionLogItemMilestone.class, "collection-item");
        register(types, CollectionLogTotalMilestone.class, "collection-total");
        register(types, QuestMilestone.class, "quest");
        register(types, QuestSeriesMilestone.class, "quest-series");
        register(types, AchievementDiaryTierMilestone.class, "diary-tier");
        register(types, AchievementDiaryTierAreaMilestone.class, "diary-region");
        register(types, CombatAchievementTierMilestone.class, "combat-tier");
        register(types, CombatAchievementBossMilestone.class, "combat-boss");
        register(types, CombatAchievementDifficultyMilestone.class, "combat-difficulty");
        register(types, PossessedItemMilestone.class, "owned-item");
        register(types, CombatAchievementTaskMilestone.class, "combat-task");
        return Collections.unmodifiableMap(types);
    }
    private static void register(Map<String, Class<? extends Milestone>> types, Class<? extends Milestone> type, String tag) {
        types.put(tag, type);
    }
    static Class<? extends Milestone> fromTag(JsonObject saved) {
        if (!saved.has("type") || saved.has("class")) {
            return null;
        }
        return resolve(saved.get("type").getAsString());
    }
    static Class<? extends Milestone> resolve(String name) {
        return TYPES.get(name);
    }
    static Set<Class<? extends Milestone>> registeredTypes() {
        return Collections.unmodifiableSet(new HashSet<>(TYPES.values()));
    }
    static String nameOf(Milestone goal) {
        for (Map.Entry<String, Class<? extends Milestone>> entry : TYPES.entrySet()) {
            if (entry.getValue() == goal.getClass()) {
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException("Unsupported milestone type: " + goal.getClass().getName());
    }
}
