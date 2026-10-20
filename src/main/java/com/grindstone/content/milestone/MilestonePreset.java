package com.grindstone.content.milestone;
import com.grindstone.content.combatachievement.CombatAchievementTier;
import com.grindstone.content.diary.AchievementDiaryArea;
import com.grindstone.content.diary.DiaryTier;
import com.grindstone.content.milestone.impl.level.SkillLevelMilestone;
import com.grindstone.content.milestone.impl.level.CombatLevelMilestone;
import com.grindstone.content.milestone.impl.experience.SkillExperienceMilestone;
import com.grindstone.content.milestone.impl.quest.QuestMilestone;
import com.grindstone.content.milestone.impl.quest.QuestSeriesMilestone;
import com.grindstone.content.milestone.impl.achievementdiary.AchievementDiaryTierAreaMilestone;
import com.grindstone.content.milestone.impl.achievementdiary.AchievementDiaryTierMilestone;
import com.grindstone.content.milestone.impl.combatachievement.CombatAchievementBossMilestone;
import com.grindstone.content.milestone.impl.combatachievement.CombatAchievementTierMilestone;
import com.grindstone.content.milestone.impl.combatachievement.CombatAchievementDifficultyMilestone;
import com.grindstone.content.quest.QuestSeries;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public enum MilestonePreset {
    BEGINNER("Beginner"),
    SKILLING("Skilling"),
    PVM("PvM"),
    QUESTING("Questing"),
    COMPLETIONIST("Completionist");
    private final String name;
    MilestonePreset(String name) {
        this.name = name;
    }
    @Override
    public String toString() {
        return name;
    }
    public List<Milestone> createGoals() {
        List<Milestone> goals = new ArrayList<>();
        switch (this) {
            case BEGINNER:
                SkillLevelMilestone attack = new SkillLevelMilestone(Skill.ATTACK);
                attack.setTarget(40);
                goals.add(attack);
                CombatLevelMilestone combat = new CombatLevelMilestone();
                combat.setTarget(50);
                goals.add(combat);
                goals.add(new QuestMilestone(Quest.COOKS_ASSISTANT));
                goals.add(new QuestMilestone(Quest.DRAGON_SLAYER_I));
                goals.add(new AchievementDiaryTierAreaMilestone(AchievementDiaryArea.LUMBRIDGE, DiaryTier.EASY));
                break;
            case SKILLING:
                for (Skill skill : Skill.values()) {
                    goals.add(new SkillLevelMilestone(skill));
                    goals.add(new SkillExperienceMilestone(skill));
                }
                break;
            case PVM:
                goals.add(new CombatLevelMilestone());
                goals.add(new CombatAchievementTierMilestone(CombatAchievementTier.EASY));
                goals.add(new CombatAchievementDifficultyMilestone(CombatAchievementTier.EASY));
                goals.add(new CombatAchievementBossMilestone("Barrows"));
                goals.add(new QuestMilestone(Quest.A_NIGHT_AT_THE_THEATRE));
                break;
            case QUESTING:
                goals.add(new QuestMilestone(Quest.COOKS_ASSISTANT));
                goals.add(new QuestMilestone(Quest.DRAGON_SLAYER_I));
                goals.add(new QuestSeriesMilestone(QuestSeries.ELF));
                goals.add(new QuestSeriesMilestone(QuestSeries.MYREQUE));
                goals.add(new QuestSeriesMilestone(QuestSeries.FREMENNIK));
                break;
            case COMPLETIONIST:
                for (Skill skill : Skill.values()) {
                    goals.add(new SkillLevelMilestone(skill));
                }
                for (QuestSeries series : QuestSeries.values()) {
                    goals.add(new QuestSeriesMilestone(series));
                }
                for (DiaryTier tier : DiaryTier.values()) {
                    goals.add(new AchievementDiaryTierMilestone(tier));
                }
                for (CombatAchievementTier tier : CombatAchievementTier.values()) {
                    goals.add(new CombatAchievementTierMilestone(tier));
                    goals.add(new CombatAchievementDifficultyMilestone(tier));
                }
                break;
            default: throw new IllegalStateException("Unknown preset");
        }
        return goals;
    }
}
