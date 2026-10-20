package com.grindstone.content.milestone;

import com.grindstone.ui.Icon;
import lombok.Getter;
import java.util.Locale;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public enum MilestoneCategory {
    BOSSING("Bossing", Icon.BOSSING),
    RAIDS("Raids", Icon.BOSSING),
    SLAYER("Slayer", Icon.BOSSING),
    CLUES("Clues", Icon.COLLECTION_LOG),
    MINIGAMES("Minigames", Icon.MINIGAMES),
    COLLECTION_LOG("Collection Log", Icon.COLLECTION_LOG),
    ITEMS("Items", Icon.MAIN),
    SKILLING("Skilling", Icon.SKILLING),
    QUEST("Quests", Icon.QUEST),
    ACHIEVEMENT_DIARY("Achievement Diary", Icon.ACHIEVEMENT_DIARY),
    COMBAT_ACHIEVEMENT("Combat Achievements", Icon.COMBAT_ACHIEVEMENT),
    OTHER("Personal", Icon.MAIN);

    @Getter
    private final String displayName;
    @Getter
    private final Icon icon;

    MilestoneCategory(String displayName, Icon icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static MilestoneCategory fromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return OTHER;
        }

        String normalized = name.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("slayer")) {
            return SLAYER;
        }
        if (normalized.contains("clue")) {
            return CLUES;
        }
        if (normalized.contains("raid")) {
            return RAIDS;
        }
        if (normalized.contains("minigame")) {
            return MINIGAMES;
        }

        if (normalized.contains("collection") || normalized.contains("collectionlog")) {
            return COLLECTION_LOG;
        }

        if (normalized.contains("combat achievement") || normalized.contains("combat_achievement")) {
            return COMBAT_ACHIEVEMENT;
        }

        if (normalized.contains("diary")) {
            return ACHIEVEMENT_DIARY;
        }

        if (normalized.contains("quest")) {
            return QUEST;
        }

        if (normalized.contains("boss")) {
            return BOSSING;
        }

        if (normalized.contains("skill")) {
            return SKILLING;
        }

        return OTHER;
    }
}
