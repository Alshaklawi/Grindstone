package com.grinder.repository;
import com.google.gson.JsonObject;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.impl.ChecklistGoal;
import com.grinder.content.goal.impl.CustomGoal;
import com.grinder.content.goal.impl.LinkedGoal;
import com.grinder.content.goal.impl.TrackerGoal;
import com.grinder.content.goal.impl.achievementdiary.AchievementDiaryTierAreaGoal;
import com.grinder.content.goal.impl.achievementdiary.AchievementDiaryTierGoal;
import com.grinder.content.goal.impl.collectionlog.CollectionLogEntryGoal;
import com.grinder.content.goal.impl.collectionlog.CollectionLogItemGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementBossGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementDifficultyGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementTaskGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementTierGoal;
import com.grinder.content.goal.impl.experience.BaseExperienceGoal;
import com.grinder.content.goal.impl.experience.SessionExperienceGoal;
import com.grinder.content.goal.impl.experience.SkillExperienceGoal;
import com.grinder.content.goal.impl.experience.TotalExperienceGoal;
import com.grinder.content.goal.impl.item.PossessedItemGoal;
import com.grinder.content.goal.impl.level.BaseLevelGoal;
import com.grinder.content.goal.impl.level.CombatLevelGoal;
import com.grinder.content.goal.impl.level.SkillLevelGoal;
import com.grinder.content.goal.impl.level.TotalLevelGoal;
import com.grinder.content.goal.impl.quest.QuestGoal;
import com.grinder.content.goal.impl.quest.QuestSeriesGoal;
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
final class SavedGoalTypes {
    private static final Map<String, Class<? extends Goal>> TYPES = createTypes();
    private SavedGoalTypes() {
    }
    private static Map<String, Class<? extends Goal>> createTypes() {
        Map<String, Class<? extends Goal>> types = new LinkedHashMap<>();
        register(types, TrackerGoal.class, "activity");
        register(types, CustomGoal.class, "custom");
        register(types, ChecklistGoal.class, "manual-checklist");
        register(types, LinkedGoal.class, "linked-goal");
        register(types, SkillLevelGoal.class, "skill-level");
        register(types, CombatLevelGoal.class, "combat-level");
        register(types, TotalLevelGoal.class, "total-level");
        register(types, BaseLevelGoal.class, "base-level");
        register(types, BaseExperienceGoal.class, "base-xp");
        register(types, SessionExperienceGoal.class, "session-xp");
        register(types, SkillExperienceGoal.class, "skill-xp");
        register(types, TotalExperienceGoal.class, "total-xp");
        register(types, CollectionLogEntryGoal.class, "collection-page");
        register(types, CollectionLogItemGoal.class, "collection-item");
        register(types, QuestGoal.class, "quest");
        register(types, QuestSeriesGoal.class, "quest-series");
        register(types, AchievementDiaryTierGoal.class, "diary-tier");
        register(types, AchievementDiaryTierAreaGoal.class, "diary-region");
        register(types, CombatAchievementTierGoal.class, "combat-tier");
        register(types, CombatAchievementBossGoal.class, "combat-boss");
        register(types, CombatAchievementDifficultyGoal.class, "combat-difficulty");
        register(types, PossessedItemGoal.class, "owned-item");
        register(types, CombatAchievementTaskGoal.class, "combat-task");
        return Collections.unmodifiableMap(types);
    }
    private static void register(Map<String, Class<? extends Goal>> types, Class<? extends Goal> type, String tag) {
        types.put(tag, type);
    }
    static Class<? extends Goal> fromTag(JsonObject saved) {
        if (!saved.has("type") || saved.has("class")) {
            return null;
        }
        return resolve(saved.get("type").getAsString());
    }
    static Class<? extends Goal> resolve(String name) {
        return TYPES.get(name);
    }
    static Set<Class<? extends Goal>> registeredTypes() {
        return Collections.unmodifiableSet(new HashSet<>(TYPES.values()));
    }
    static String nameOf(Goal goal) {
        for (Map.Entry<String, Class<? extends Goal>> entry : TYPES.entrySet()) {
            if (entry.getValue() == goal.getClass()) {
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException("Unsupported goal type: " + goal.getClass().getName());
    }
}
