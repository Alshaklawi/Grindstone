package com.grinder.content.goal;

import com.grinder.ui.Icon;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public enum GoalCategory
{
    BOSSING("Bossing", Icon.BOSSING),
    COLLECTION_LOG("Collection Log", Icon.COLLECTION_LOG),
    SKILLING("Skilling", Icon.SKILLING),
    QUEST("Quests", Icon.QUEST),
    ACHIEVEMENT_DIARY("Achievement Diaries", Icon.ACHIEVEMENT_DIARY),
    COMBAT_ACHIEVEMENT("Combat Achievements", Icon.COMBAT_ACHIEVEMENT),
    OTHER("Other Goals", Icon.MAIN);

    private final String displayName;
    private final Icon icon;

    GoalCategory(String displayName, Icon icon)
    {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public Icon getIcon()
    {
        return icon;
    }

    public static GoalCategory fromName(String name)
    {
        if (name == null || name.trim().isEmpty())
        {
            return OTHER;
        }

        String normalized = name.trim().toLowerCase();

        if (normalized.contains("collection") || normalized.contains("collectionlog"))
        {
            return COLLECTION_LOG;
        }

        if (normalized.contains("combat achievement") || normalized.contains("combat_achievement"))
        {
            return COMBAT_ACHIEVEMENT;
        }

        if (normalized.contains("diary"))
        {
            return ACHIEVEMENT_DIARY;
        }

        if (normalized.contains("quest"))
        {
            return QUEST;
        }

        if (normalized.contains("boss"))
        {
            return BOSSING;
        }

        if (normalized.contains("skill"))
        {
            return SKILLING;
        }

        return OTHER;
    }
}
