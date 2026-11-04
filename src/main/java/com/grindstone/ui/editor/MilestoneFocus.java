package com.grindstone.ui.editor;

import com.grindstone.content.milestone.MilestoneCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
@Getter
@RequiredArgsConstructor
public enum MilestoneFocus {
    SKILLS("Skills", "Build levels, earn experience or track skilling activities.", MilestoneCategory.SKILLING),
    BOSSING("Bossing", "Track boss kills, chests and challenge completions.", MilestoneCategory.BOSSING),
    RAIDS("Raids", "Track raid completions and difficulty modes.", MilestoneCategory.RAIDS),
    SLAYER("Slayer", "Track points and normal or Wilderness task streaks.", MilestoneCategory.SLAYER),
    CLUES("Clue Scrolls", "Track completed clue scrolls.", MilestoneCategory.CLUES),
    MINIGAMES("Minigames", "Track games, wins, points and activity counts.", MilestoneCategory.MINIGAMES),
    COLLECTION("Collection Log", "Track totals, tabs, pages and individual unlocks.", MilestoneCategory.COLLECTION_LOG),
    ITEMS("Items", "Track owned quantities across known personal storage.", MilestoneCategory.ITEMS),
    QUESTS("Quests", "Finish quests or follow a quest series.", MilestoneCategory.QUEST),
    DIARIES("Achievement Diary", "Complete diary regions and tiers.", MilestoneCategory.ACHIEVEMENT_DIARY),
    COMBAT("Combat Achievements", "Work through tasks and reward tiers.", MilestoneCategory.COMBAT_ACHIEVEMENT),
    PERSONAL("Personal", "Create a milestone, checklist or routine.", MilestoneCategory.OTHER);

    private final String title;
    private final String description;
    private final MilestoneCategory category;

    @Override
    public String toString() {
        return title;
    }
}
