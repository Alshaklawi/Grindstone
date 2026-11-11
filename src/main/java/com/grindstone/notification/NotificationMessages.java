package com.grindstone.notification;

import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.impl.ChecklistMilestone;
import com.grindstone.content.milestone.impl.CustomMilestone;
import com.grindstone.content.milestone.impl.LinkedMilestone;
import com.grindstone.content.milestone.impl.TrackerMilestone;
import com.grindstone.content.milestone.impl.experience.BaseExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.SessionExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.SkillExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.TotalExperienceMilestone;
import com.grindstone.content.milestone.impl.item.PossessedItemMilestone;
import com.grindstone.content.milestone.impl.level.BaseLevelMilestone;
import com.grindstone.content.milestone.impl.level.CombatLevelMilestone;
import com.grindstone.content.milestone.impl.level.SkillLevelMilestone;
import com.grindstone.content.milestone.impl.level.TotalLevelMilestone;
import com.grindstone.repository.RepeatSchedule;
import java.util.Locale;
import net.runelite.api.Client;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.util.Text;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/20/2026
 */
public final class NotificationMessages {
    private NotificationMessages() {
    }

    public static String formatTitle(String title) {
        String clean = Text.removeTags(title == null ? "" : title).replaceAll("[\\r\\n\\t]", " ").trim();
        if (clean.isEmpty()) {
            return "Personal milestone";
        }
        return clean.length() > 100 ? clean.substring(0, 97) + "..." : clean;
    }

    public static String formatProgress(long current, long target) {
        return target > 0 ? String.format(Locale.ROOT, " (%,d / %,d)", Math.max(0, current), target) : "";
    }

    public static String formatProgress(Milestone goal, long current, long target) {
        if (target <= 0) {
            return "";
        }
        return " (" + getProgressDetail(goal, current, target) + ")";
    }

    private static String getProgressDetail(Milestone goal, long current, long target) {
        String unit = getUnit(goal, target);
        return String.format(Locale.ROOT, "%,d / %,d%s", Math.max(0, current), target, unit.isEmpty() ? "" : " " + unit);
    }

    public static String getTitle(Milestone goal, Client client) {
        return formatTitle(goal.getTitle(client));
    }

    public static String getCompletionHeading(Milestone goal, RepeatSchedule schedule) {
        if (schedule != RepeatSchedule.NONE) {
            return schedule + " routine complete";
        }
        return goal instanceof LinkedMilestone ? "Group complete" : "Milestone complete";
    }

    public static String getCompletionDetail(Milestone goal, long current, long target, RepeatSchedule schedule, boolean numbers) {
        if (schedule != RepeatSchedule.NONE) {
            return schedule + (numbers ? " · " + getProgressDetail(goal, current, target) : " target reached");
        }
        if (isExperience(goal)) {
            return numbers ? String.format(Locale.ROOT, "%,d XP reached", target) : "XP target reached";
        }
        if (isLevel(goal)) {
            return numbers ? String.format(Locale.ROOT, "Level %,d reached", target) : "Level target reached";
        }
        if (goal instanceof TrackerMilestone) {
            String unit = getUnit(goal, target);
            String action = "points".equals(unit) || "point".equals(unit) || "kills".equals(unit) || "kill".equals(unit) ? "reached" : "chests".equals(unit) || "chest".equals(unit) ? "opened" : "rifts".equals(unit) || "rift".equals(unit) ? "closed" : "completed";
            return numbers ? String.format(Locale.ROOT, "%,d %s %s", target, unit, action) : "Activity target reached";
        }
        if (goal instanceof PossessedItemMilestone) {
            return numbers ? String.format(Locale.ROOT, "%,d / %,d owned", current, target) : "Item target reached";
        }
        if (goal.getCategory() == MilestoneCategory.COLLECTION_LOG) {
            return target > 1 && numbers ? String.format(Locale.ROOT, "%,d / %,d items unlocked", current, target) : target == 1 ? "Item unlocked" : "Collection target complete";
        }
        if (goal instanceof ChecklistMilestone || goal instanceof LinkedMilestone || target > 1) {
            return numbers ? String.format(Locale.ROOT, "%,d / %,d %s complete", current, target, getUnit(goal, target)).replace("  ", " ") : "Target reached";
        }
        switch (goal.getCategory()) {
            case QUEST:
                return "Quest complete";
            case ACHIEVEMENT_DIARY:
                return "Diary target complete";
            case COMBAT_ACHIEVEMENT:
                return "Combat achievement complete";
            default:
                return "Target reached";
        }
    }

    private static boolean isExperience(Milestone goal) {
        return goal instanceof SkillExperienceMilestone || goal instanceof TotalExperienceMilestone || goal instanceof BaseExperienceMilestone || goal instanceof SessionExperienceMilestone;
    }

    private static boolean isLevel(Milestone goal) {
        return goal instanceof SkillLevelMilestone || goal instanceof TotalLevelMilestone || goal instanceof BaseLevelMilestone || goal instanceof CombatLevelMilestone;
    }

    private static String getUnit(Milestone goal, long count) {
        if (isExperience(goal)) {
            return "XP";
        }
        if (isLevel(goal)) {
            return count == 1 ? "level" : "levels";
        }
        if (goal instanceof CustomMilestone) {
            return "";
        }
        if (goal instanceof ChecklistMilestone) {
            return count == 1 ? "step" : "steps";
        }
        if (goal instanceof LinkedMilestone) {
            LinkedMilestone.Mode mode = ((LinkedMilestone) goal).getMode();
            if (mode == LinkedMilestone.Mode.REPEAT_COMPLETIONS) {
                return count == 1 ? "period" : "periods";
            }
            if (mode == LinkedMilestone.Mode.MILESTONES) {
                return count == 1 ? "checkpoint" : "checkpoints";
            }
            return count == 1 ? "target" : "targets";
        }
        if (goal instanceof TrackerMilestone) {
            return ((TrackerMilestone) goal).getTracker().getProgressUnit(count);
        }
        MilestoneCategory category = goal.getCategory();
        if (category == MilestoneCategory.RAIDS) {
            return count == 1 ? "raid" : "raids";
        }
        if (category == MilestoneCategory.COLLECTION_LOG || goal instanceof PossessedItemMilestone) {
            return count == 1 ? "item" : "items";
        }
        if (category == MilestoneCategory.QUEST) {
            return count == 1 ? "quest" : "quests";
        }
        if (category == MilestoneCategory.COMBAT_ACHIEVEMENT || category == MilestoneCategory.ACHIEVEMENT_DIARY) {
            return count == 1 ? "task" : "tasks";
        }
        if (category == MilestoneCategory.CLUES) {
            return count == 1 ? "clue" : "clues";
        }
        return count == 1 ? "target" : "targets";
    }

    public static String formatChat(String heading, String detail, boolean colours) {
        return new ChatMessageBuilder()
                .append(ChatColorType.NORMAL)
                .append("[Grindstone] ")
                .append(colours ? ChatColorType.HIGHLIGHT : ChatColorType.NORMAL)
                .append(heading)
                .append(ChatColorType.NORMAL)
                .append(": " + detail)
                .build();
    }
}
