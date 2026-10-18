package com.grinder.repository;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.impl.TrackerGoal;
import com.grinder.content.goal.impl.CustomGoal;
import com.grinder.content.goal.impl.level.SkillLevelGoal;
import com.grinder.content.goal.impl.level.CombatLevelGoal;
import com.grinder.content.goal.impl.level.TotalLevelGoal;
import com.grinder.content.goal.impl.experience.SkillExperienceGoal;
import com.grinder.content.goal.impl.experience.TotalExperienceGoal;
import com.grinder.content.goal.impl.collectionlog.CollectionLogEntryGoal;
import com.grinder.content.goal.impl.collectionlog.CollectionLogItemGoal;
import com.grinder.content.goal.impl.quest.QuestGoal;
import com.grinder.content.goal.impl.quest.QuestSeriesGoal;
import com.grinder.content.goal.impl.achievementdiary.AchievementDiaryTierGoal;
import com.grinder.content.goal.impl.achievementdiary.AchievementDiaryTierAreaGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementTierGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementBossGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementDifficultyGoal;
import com.grinder.content.goal.impl.item.PossessedItemGoal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
final class SavedGoalTypes {
    private static final Map<String, Class<? extends Goal>> TYPES = createTypes();
    private SavedGoalTypes() {}
    private static Map<String, Class<? extends Goal>> createTypes() {
        Map<String, Class<? extends Goal>> types = new LinkedHashMap<>();
        register(types, TrackerGoal.class, "com.grinder.content.goal.impl.TrackerGoal");
        register(types, CustomGoal.class, "com.grinder.content.goal.impl.CustomGoal");
        register(types, SkillLevelGoal.class, "com.grinder.content.goal.impl.level.SkillLevelGoal", "com.grinder.content.goal.impl.SkillLevelGoal");
        register(types, CombatLevelGoal.class, "com.grinder.content.goal.impl.level.CombatLevelGoal", "com.grinder.content.goal.impl.CombatLevelGoal");
        register(types, TotalLevelGoal.class, "com.grinder.content.goal.impl.level.TotalLevelGoal", "com.grinder.content.goal.impl.TotalLevelGoal");
        register(types, SkillExperienceGoal.class, "com.grinder.content.goal.impl.experience.SkillExperienceGoal", "com.grinder.content.goal.impl.SkillExperienceGoal");
        register(types, TotalExperienceGoal.class, "com.grinder.content.goal.impl.experience.TotalExperienceGoal", "com.grinder.content.goal.impl.TotalExperienceGoal");
        register(types, CollectionLogEntryGoal.class, "com.grinder.content.goal.impl.colletion_log.CollectionLogEntryGoal");
        register(types, CollectionLogItemGoal.class, "com.grinder.content.goal.impl.colletion_log.CollectionLogItemGoal");
        register(types, QuestGoal.class, "com.grinder.content.goal.impl.quest.QuestGoal");
        register(types, QuestSeriesGoal.class, "com.grinder.content.goal.impl.quest.QuestSeriesGoal");
        register(types, AchievementDiaryTierGoal.class, "com.grinder.content.goal.impl.achievement_diary.AchievementDiaryTierGoal");
        register(types, AchievementDiaryTierAreaGoal.class, "com.grinder.content.goal.impl.achievement_diary.AchievementDiaryTierAreaGoal");
        register(types, CombatAchievementTierGoal.class, "com.grinder.content.goal.impl.combat_achievement.CombatAchievementTierGoal");
        register(types, CombatAchievementBossGoal.class, "com.grinder.content.goal.impl.combat_achievement.CombatAchievementBossGoal");
        register(types, CombatAchievementDifficultyGoal.class, "com.grinder.content.goal.impl.combat_achievement.CombatAchievementDifficultyGoal");
        register(types, PossessedItemGoal.class, "com.grinder.content.goal.impl.item.PossesedItemGoal");
        return Collections.unmodifiableMap(types);
    }
    private static void register(Map<String, Class<? extends Goal>> types, Class<? extends Goal> type, String... aliases) {
        for (String alias : aliases) { types.put(alias, type); }
        types.put(type.getName(), type);
    }
    static Class<? extends Goal> resolve(String name) { return TYPES.get(name); }
    static java.util.Set<Class<? extends Goal>> registeredTypes() { return Collections.unmodifiableSet(new java.util.HashSet<>(TYPES.values())); }
    static String nameOf(Goal goal) {
        for (Map.Entry<String, Class<? extends Goal>> entry : TYPES.entrySet()) {
            if (entry.getValue() == goal.getClass()) { return entry.getKey(); }
        }
        throw new IllegalArgumentException("Unsupported goal type: " + goal.getClass().getName());
    }
}
