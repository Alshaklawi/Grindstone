package com.grinder.content.goal;
import com.grinder.content.combatachievement.CombatAchievementTier;
import com.grinder.content.diary.AchievementDiaryArea;
import com.grinder.content.diary.DiaryTier;
import com.grinder.content.goal.impl.level.SkillLevelGoal;
import com.grinder.content.goal.impl.level.CombatLevelGoal;
import com.grinder.content.goal.impl.experience.SkillExperienceGoal;
import com.grinder.content.goal.impl.quest.QuestGoal;
import com.grinder.content.goal.impl.quest.QuestSeriesGoal;
import com.grinder.content.goal.impl.achievementdiary.AchievementDiaryTierAreaGoal;
import com.grinder.content.goal.impl.achievementdiary.AchievementDiaryTierGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementBossGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementTierGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementDifficultyGoal;
import com.grinder.content.quest.QuestSeries;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public enum GoalPreset {
    BEGINNER("Beginner"),
    SKILLING("Skilling"),
    PVM("PvM"),
    QUESTING("Questing"),
    COMPLETIONIST("Completionist");
    private final String name;
    GoalPreset(String name) {
        this.name = name;
    }
    @Override
    public String toString() {
        return name;
    }
    public List<Goal> createGoals() {
        List<Goal> goals = new ArrayList<>();
        switch (this) {
            case BEGINNER:
                SkillLevelGoal attack = new SkillLevelGoal(Skill.ATTACK);
                attack.setTarget(40);
                goals.add(attack);
                CombatLevelGoal combat = new CombatLevelGoal();
                combat.setTarget(50);
                goals.add(combat);
                goals.add(new QuestGoal(Quest.COOKS_ASSISTANT));
                goals.add(new QuestGoal(Quest.DRAGON_SLAYER_I));
                goals.add(new AchievementDiaryTierAreaGoal(AchievementDiaryArea.LUMBRIDGE, DiaryTier.EASY));
                break;
            case SKILLING:
                for (Skill skill : Skill.values()) {
                    goals.add(new SkillLevelGoal(skill));
                    goals.add(new SkillExperienceGoal(skill));
                }
                break;
            case PVM:
                goals.add(new CombatLevelGoal());
                goals.add(new CombatAchievementTierGoal(CombatAchievementTier.EASY));
                goals.add(new CombatAchievementDifficultyGoal(CombatAchievementTier.EASY));
                goals.add(new CombatAchievementBossGoal("Barrows"));
                goals.add(new QuestGoal(Quest.A_NIGHT_AT_THE_THEATRE));
                break;
            case QUESTING:
                goals.add(new QuestGoal(Quest.COOKS_ASSISTANT));
                goals.add(new QuestGoal(Quest.DRAGON_SLAYER_I));
                goals.add(new QuestSeriesGoal(QuestSeries.ELF));
                goals.add(new QuestSeriesGoal(QuestSeries.MYREQUE));
                goals.add(new QuestSeriesGoal(QuestSeries.FREMENNIK));
                break;
            case COMPLETIONIST:
                for (Skill skill : Skill.values()) {
                    goals.add(new SkillLevelGoal(skill));
                }
                for (QuestSeries series : QuestSeries.values()) {
                    goals.add(new QuestSeriesGoal(series));
                }
                for (DiaryTier tier : DiaryTier.values()) {
                    goals.add(new AchievementDiaryTierGoal(tier));
                }
                for (CombatAchievementTier tier : CombatAchievementTier.values()) {
                    goals.add(new CombatAchievementTierGoal(tier));
                    goals.add(new CombatAchievementDifficultyGoal(tier));
                }
                break;
            default: throw new IllegalStateException("Unknown preset");
        }
        return goals;
    }
}
