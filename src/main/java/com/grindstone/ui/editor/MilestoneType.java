package com.grindstone.ui.editor;

import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.impl.experience.BaseExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.SessionExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.SkillExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.TotalExperienceMilestone;
import com.grindstone.content.milestone.impl.level.BaseLevelMilestone;
import com.grindstone.content.milestone.impl.level.CombatLevelMilestone;
import com.grindstone.content.milestone.impl.level.SkillLevelMilestone;
import com.grindstone.content.milestone.impl.level.TotalLevelMilestone;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Skill;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
@RequiredArgsConstructor
public enum MilestoneType {
    SKILL_LEVEL("Skill level", MilestoneFocus.SKILLS),
    SKILL_EXPERIENCE("Skill experience", MilestoneFocus.SKILLS),
    COMBAT_LEVEL("Combat level", MilestoneFocus.SKILLS),
    TOTAL_LEVEL("Total level", MilestoneFocus.SKILLS),
    TOTAL_EXPERIENCE("Total experience", MilestoneFocus.SKILLS),
    BOSS_KILLS("Boss kills & completions", MilestoneFocus.BOSSING),
    COLLECTION_PAGE("Page", MilestoneFocus.COLLECTION),
    COLLECTION_ITEM("Item unlock", MilestoneFocus.COLLECTION),
    CUSTOM("Custom milestone", MilestoneFocus.PERSONAL),
    QUEST("Quest", MilestoneFocus.QUESTS),
    QUEST_SERIES("Quest series", MilestoneFocus.QUESTS),
    DIARY_REGION("Region & tier", MilestoneFocus.DIARIES),
    DIARY_TIER("Tier across all regions", MilestoneFocus.DIARIES),
    COMBAT_BOSS("Boss tasks", MilestoneFocus.COMBAT),
    COMBAT_TIER("Reward tier", MilestoneFocus.COMBAT),
    COMBAT_DIFFICULTY("Difficulty tasks", MilestoneFocus.COMBAT),
    OWNED_ITEM("Owned quantity", MilestoneFocus.ITEMS),
    COMBAT_TASK("Individual task", MilestoneFocus.COMBAT),
    BASE_LEVEL("Base level (all skills)", MilestoneFocus.SKILLS),
    BASE_EXPERIENCE("Base experience (all skills)", MilestoneFocus.SKILLS),
    SESSION_EXPERIENCE("Session experience", MilestoneFocus.SKILLS),
    CHECKLIST("Checklist / routine", MilestoneFocus.PERSONAL),
    COLLECTION_TOTAL("Total unlocks", MilestoneFocus.COLLECTION),
    RAID_COMPLETIONS("Raid completions", MilestoneFocus.RAIDS),
    SLAYER_COUNTER("Slayer counters", MilestoneFocus.SLAYER),
    CLUE_COMPLETIONS("Clue scroll completions", MilestoneFocus.CLUES),
    MINIGAME_COUNTER("Minigame counters", MilestoneFocus.MINIGAMES),
    SKILLING_ACTIVITY("Skilling activities", MilestoneFocus.SKILLS),
    QUEST_SPEEDRUN("Speedrun completions", MilestoneFocus.QUESTS),
    COLLECTION_TAB("Tab", MilestoneFocus.COLLECTION);

    private final String title;
    @Getter
    private final MilestoneFocus focus;

    public boolean isActivity() {
        return this == BOSS_KILLS || this == RAID_COMPLETIONS || this == SLAYER_COUNTER || this == CLUE_COMPLETIONS || this == MINIGAME_COUNTER || this == SKILLING_ACTIVITY || this == QUEST_SPEEDRUN;
    }

    public boolean isSkill() {
        return this == SKILL_LEVEL || this == SKILL_EXPERIENCE;
    }

    public boolean isCompletion() {
        return focus == MilestoneFocus.QUESTS || focus == MilestoneFocus.DIARIES || focus == MilestoneFocus.COMBAT;
    }

    public boolean isClientType() {
        return isCompletion() || isActivity() || this == OWNED_ITEM || this == BASE_LEVEL || this == BASE_EXPERIENCE || this == SESSION_EXPERIENCE || this == COLLECTION_TOTAL || this == COLLECTION_TAB;
    }

    public boolean isTier() {
        return this == DIARY_TIER || this == COMBAT_TIER || this == COMBAT_DIFFICULTY;
    }

    public boolean requiresCollectionSnapshot() {
        return this == COLLECTION_PAGE || this == COLLECTION_ITEM || this == COLLECTION_TAB;
    }

    public Map<String, Supplier<Milestone>> createDefaultChoices() {
        Map<String, Supplier<Milestone>> choices = new LinkedHashMap<>();
        switch (this) {
            case SKILL_LEVEL:
            case SKILL_EXPERIENCE:
            case SESSION_EXPERIENCE:
                if (this == SESSION_EXPERIENCE) {
                    choices.put("All skills (total gains)", SessionExperienceMilestone::new);
                }
                for (Skill skill : Skill.values()) {
                    choices.put(skill.getName(), this == SKILL_LEVEL ? () -> new SkillLevelMilestone(skill) : this == SKILL_EXPERIENCE ? () -> new SkillExperienceMilestone(skill) : () -> new SessionExperienceMilestone(skill));
                }
                break;
            case COMBAT_LEVEL:
                choices.put("Combat level", CombatLevelMilestone::new);
                break;
            case TOTAL_LEVEL:
                choices.put("Total level", TotalLevelMilestone::new);
                break;
            case TOTAL_EXPERIENCE:
                choices.put("Total experience", TotalExperienceMilestone::new);
                break;
            case BASE_LEVEL:
                choices.put("Base level", BaseLevelMilestone::new);
                break;
            case BASE_EXPERIENCE:
                choices.put("Base experience", BaseExperienceMilestone::new);
                break;
            default:
                break;
        }
        return choices;
    }

    @Override
    public String toString() {
        return title;
    }
}
