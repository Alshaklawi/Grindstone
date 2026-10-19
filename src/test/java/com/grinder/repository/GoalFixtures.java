package com.grinder.repository;

import com.grinder.content.collectionlog.CollectionLogEntry;
import com.grinder.content.combatachievement.CombatAchievementTier;
import com.grinder.content.diary.AchievementDiaryArea;
import com.grinder.content.diary.DiaryTier;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalCategory;
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
import com.grinder.content.quest.QuestSeries;
import com.grinder.content.tracker.TrackerEntry;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
final class GoalFixtures {
    private final GrinderProfile profile;

    GoalFixtures(GrinderProfile profile) {
        this.profile = profile;
    }

    void populate() {
        for (CollectionLogEntry entry : CollectionLogEntry.values()) {
            if (!hasCollectionEntry(entry)) {
                profile.add(new CollectionLogEntryGoal(null, entry));
            }
        }

        for (TrackerEntry entry : TrackerEntry.values()) {
            if (!hasTracker(entry)) {
                profile.add(new TrackerGoal(entry, entry.getDefaultTarget()));
            }
        }

        for (Skill skill : Skill.values()) {
            if (!hasSkillGoal(skill)) {
                profile.add(new SkillLevelGoal(skill));
            }

            if (!hasExperienceGoal(skill)) {
                profile.add(new SkillExperienceGoal(skill));
            }
        }

        if (profile.getGoals().stream().noneMatch(goal -> goal instanceof CombatLevelGoal)) {
            profile.add(new CombatLevelGoal());
        }

        if (profile.getGoals().stream().noneMatch(goal -> goal instanceof TotalLevelGoal)) {
            profile.add(new TotalLevelGoal());
        }

        if (profile.getGoals().stream().noneMatch(goal -> goal instanceof TotalExperienceGoal && profile.metadata(goal).recurring().getSchedule() == RepeatSchedule.NONE)) {
            profile.add(new TotalExperienceGoal());
        }

        addDefaultCollectionItem(ItemID.DRAGON_PICKAXE);
        profile.add(new BaseLevelGoal());
        profile.add(new BaseExperienceGoal());
        profile.add(new SessionExperienceGoal(Skill.WOODCUTTING));
        Goal sampleSource = profile.findMatchingGoal(new SkillLevelGoal(Skill.ATTACK));
        String linkedExampleId = UUID.nameUUIDFromBytes("grinder-default-linked-goal".getBytes(StandardCharsets.UTF_8)).toString();
        profile.add(new LinkedGoal(linkedExampleId, "Example: Attack milestones", "A ladder example. Edit its source and milestones, or delete it.", LinkedGoal.Mode.MILESTONES, Collections.singletonList(profile.metadata(sampleSource).getId()), new long[]{50, 75, 99}, 1));
        addDefaultCollectionItem(ItemID.TWISTED_BOW);
        addDefaultCollectionItem(ItemID.JAR_OF_SPIRITS);
        profile.add(new PossessedItemGoal(ItemID.COINS, 1_500_000_000L));
        profile.add(new PossessedItemGoal(ItemID.DRAGON_PICKAXE, 1));
        profile.add(new CombatAchievementBossGoal("Barrows"));
        profile.add(new CombatAchievementDifficultyGoal(CombatAchievementTier.EASY));
        profile.add(new CombatAchievementTaskGoal(0, "Combat task 0", "The native task name, requirements and completion state load when logged in."));
        String customExampleId = UUID.nameUUIDFromBytes("grinder-default-custom-goal".getBytes(StandardCharsets.UTF_8)).toString();
        profile.add(new CustomGoal(customExampleId, "Example: complete 10 personal challenges", "A manual tracker example. Edit its title, description, target and progress to suit your own goal.", GoalCategory.OTHER, 0, 10));
        String checklistExampleId = UUID.nameUUIDFromBytes("grinder-default-checklist".getBytes(StandardCharsets.UTF_8)).toString();
        profile.add(new ChecklistGoal(checklistExampleId, "Example: personal routine", "Check steps manually. Add a repeat schedule to reset and archive this routine each period.", Arrays.asList("Choose today's activity", "Prepare supplies", "Finish the activity"), new boolean[3]));
        profile.add(new QuestGoal(Quest.DRAGON_SLAYER_I));
        profile.add(new QuestSeriesGoal(QuestSeries.ELF));
        profile.add(new AchievementDiaryTierAreaGoal( AchievementDiaryArea.LUMBRIDGE, DiaryTier.EASY));
        profile.add(new AchievementDiaryTierGoal(DiaryTier.EASY));
        profile.add(new CombatAchievementTierGoal( CombatAchievementTier.EASY));
        GoalExamples.addLinkedExamples(profile);
        profile.setItemManager(null);
    }

    private boolean hasCollectionEntry(CollectionLogEntry entry) {
        return profile.getGoals().stream().anyMatch(goal -> goal instanceof CollectionLogEntryGoal && ((CollectionLogEntryGoal) goal).getEntry() == entry);
    }

    private boolean hasTracker(TrackerEntry entry) {
        return profile.getGoals().stream().anyMatch(goal -> goal instanceof TrackerGoal && ((TrackerGoal) goal).getTracker() == entry && profile.metadata(goal).recurring().getSchedule() == RepeatSchedule.NONE);
    }

    private boolean hasSkillGoal(Skill skill) {
        return profile.getGoals().stream().anyMatch(goal -> goal instanceof SkillLevelGoal && ((SkillLevelGoal) goal).getSkill() == skill);
    }

    private boolean hasExperienceGoal(Skill skill) {
        return profile.getGoals().stream().anyMatch(goal -> goal instanceof SkillExperienceGoal && ((SkillExperienceGoal) goal).getSkill() == skill && profile.metadata(goal).recurring().getSchedule() == RepeatSchedule.NONE);
    }

    private void addDefaultCollectionItem(int itemId) {
        boolean exists = profile.getGoals().stream().anyMatch(goal -> goal instanceof CollectionLogItemGoal && ((CollectionLogItemGoal) goal).getItem() == itemId);
        if (!exists) {
            profile.add(new CollectionLogItemGoal(itemId, 1));
        }
    }
}
