package com.grinder.config;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
@ConfigGroup("grinder")
public interface GrinderConfig extends Config {
    @ConfigItem(keyName = "showDeadlineReminders", name = "Show deadline reminders", description = "Show opted-in personal deadlines in Grindstone's header within 24 hours and while past due. This is not an external notification or game reset.", section = DISPLAY_SECTION, position = 18)
    default boolean showDeadlineReminders() {
        return true;
    }
    @ConfigItem(keyName = "linkedGoalsEnabled", name = "Linked goals", description = "Show linked checklists, milestone ladders, prerequisites, achievement milestones and repeat-completion goals.", section = GOALS_SECTION, position = 9)
    default boolean linkedGoalsEnabled() {
        return true;
    }
    @ConfigItem(keyName = "groupSkillGoals", name = "Group related skill goals", description = "Keep each skill's level and XP targets together inside its category. Filters, pinning and recurring sections still apply.", section = DISPLAY_SECTION, position = 16)
    default boolean groupSkillGoals() {
        return true;
    }
    @ConfigItem(keyName = "questGoalsEnabled", name = "Quest goals", description = "Show individual quests and quest series.", section = GOALS_SECTION, position = 4)
    default boolean questGoalsEnabled() {
        return true;
    }
    @ConfigItem(keyName = "diaryGoalsEnabled", name = "Achievement diary goals", description = "Show region and all-region diary tier goals.", section = GOALS_SECTION, position = 5)
    default boolean diaryGoalsEnabled() {
        return true;
    }
    @ConfigItem(keyName = "combatAchievementGoalsEnabled", name = "Combat achievement goals", description = "Show boss, tier, difficulty and individual combat-task goals.", section = GOALS_SECTION, position = 6)
    default boolean combatAchievementGoalsEnabled() {
        return true;
    }
    @ConfigItem(keyName = "ownedItemGoalsEnabled", name = "Owned item goals", description = "Show quantities across known personal item storage.", section = GOALS_SECTION, position = 7)
    default boolean ownedItemGoalsEnabled() {
        return true;
    }
    @ConfigItem(keyName = "customGoalsEnabled", name = "Custom goals", description = "Show manually managed goals and notes.", section = GOALS_SECTION, position = 8)
    default boolean customGoalsEnabled() {
        return true;
    }
    @ConfigItem(keyName = "categorizedGoals", name = "Group by category", description = "Group unpinned goals into categories. Turn off for a single sorted list.", section = DISPLAY_SECTION, position = 6)
    default boolean categorizedGoals() {
        return true;
    }
    @ConfigItem(keyName = "showUpdateStatus", name = "Show last snapshot", description = "Show the latest inventory, equipment or storage snapshot time in the top header.", section = DISPLAY_SECTION, position = 7)
    default boolean showUpdateStatus() {
        return true;
    }
    @ConfigItem(keyName = "showPinnedSection", name = "Show pinned section", description = "Keep pinned goals above categories. Turning this off displays them in the normal list.", section = DISPLAY_SECTION, position = 8)
    default boolean showPinnedSection() {
        return true;
    }
    @ConfigItem(keyName = "showCompletionHistory", name = "Show goal history", description = "Show recurring period history, observed lifetime completions and reminders. Saved history is retained.", section = DISPLAY_SECTION, position = 9)
    default boolean showCompletionHistory() {
        return true;
    }
    @ConfigItem(keyName = "showActivityIcons", name = "Activity-specific icons", description = "Use native item icons for activities when goal icons are enabled.", section = DISPLAY_SECTION, position = 10)
    default boolean showActivityIcons() {
        return true;
    }
    @ConfigItem(keyName = "groupRecurringGoals", name = "Group recurring goals", description = "Show daily, weekly and monthly sections with UTC reset countdowns. Turning this off keeps repeat goals in the normal layout.", section = DISPLAY_SECTION, position = 11)
    default boolean groupRecurringGoals() {
        return true;
    }
    @ConfigItem(keyName = "showTodayOverview", name = "Show routine overview", description = "Show recurring completion and next reset above the All goals list. The Today view always shows it.", section = DISPLAY_SECTION, position = 12)
    default boolean showTodayOverview() {
        return true;
    }
    @ConfigItem(keyName = "showTrackingState", name = "Show period warnings", description = "Flag partially observed recurring periods. Other source details are available on title hover.", section = DISPLAY_SECTION, position = 13)
    default boolean showTrackingState() {
        return true;
    }
    @Range(min = 1, max = 20)
    @ConfigItem(keyName = "maxProgressChunks", name = "Maximum progress chunks", description = "At most 20 chunks. For a target of 100 and 20 chunks, each chunk represents 5.", section = DISPLAY_SECTION, position = 4)
    default int maxProgressChunks() {
        return 20;
    }
    @ConfigItem(keyName = "collapseDetailsByDefault", name = "Start details collapsed", description = "Default for goals you have not expanded or collapsed manually. Progress remains visible.", section = DISPLAY_SECTION, position = 5)
    default boolean collapseDetailsByDefault() {
        return false;
    }
    String GENERAL_SECTION = "general";
    String GOALS_SECTION = "goals";
    String COLLECTION_LOG_SECTION = "collectionLog";
    String DISPLAY_SECTION = "display";
    String NOTIFICATIONS_SECTION = "notifications";

    @ConfigSection(name = "Notifications", description = "Celebrate newly reached personal goals without repeating alerts on login or refresh.", position = 4)
    String notificationsSection = NOTIFICATIONS_SECTION;

    @ConfigItem(keyName = "completionPopup", name = "Completion popup", description = "Show a non-blocking RuneLite overlay when a personal goal is newly completed. Already-complete goals loaded or added are excluded.", section = NOTIFICATIONS_SECTION, position = 0)
    default boolean completionPopup() {
        return true;
    }

    @ConfigItem(keyName = "completionChat", name = "Completion chat message", description = "Add a local RuneLite chat message for newly completed personal goals. Nothing is sent to other players.", section = NOTIFICATIONS_SECTION, position = 1)
    default boolean completionChat() {
        return true;
    }

    @Range(min = 3, max = 15)
    @ConfigItem(keyName = "completionPopupSeconds", name = "Popup duration (seconds)", description = "How long a completion popup remains visible.", section = NOTIFICATIONS_SECTION, position = 2)
    default int completionPopupSeconds() {
        return 6;
    }

    @ConfigItem(keyName = "completionNotification", name = "Desktop completion notification", description = "Optional RuneLite notification with the standard sound, focus and desktop settings. Customize using its cogwheel; its own game-message option may duplicate Grindstone's separate chat message.", section = NOTIFICATIONS_SECTION, position = 3)
    default Notification completionNotification() {
        return Notification.OFF;
    }

    @ConfigSection(name = "General", description = "Enable Grindstone and control when your profile is saved.", position = 0)
    String generalSection = GENERAL_SECTION;

    @ConfigSection(name = "Goals", description = "Choose the kinds of goals shown in the side panel.", position = 1)
    String goalsSection = GOALS_SECTION;

    @ConfigSection(name = "Collection Log", description = "Control collection-log cards and item details.", position = 2)
    String collectionLogSection = COLLECTION_LOG_SECTION;

    @ConfigSection(name = "Display", description = "Adjust the side panel layout without affecting your saved goals.", position = 3)
    String displaySection = DISPLAY_SECTION;

    @ConfigItem(keyName = "enabled", name = "Enable Grindstone", description = "Show the Grindstone button and begin tracking while logged in.", section = GENERAL_SECTION, position = 0)
    default boolean enabled() {
        return true;
    }

    @ConfigItem(keyName = "saveOnLogout", name = "Save on logout", description = "Keep changes made to your goals when you log out.", section = GENERAL_SECTION, position = 1)
    default boolean saveOnLogout() {
        return true;
    }

    @ConfigItem(keyName = "saveOnShutdown", name = "Save on shutdown", description = "Keep changes made to your goals when the plugin is disabled or RuneLite closes.", section = GENERAL_SECTION, position = 2)
    default boolean saveOnShutdown() {
        return true;
    }

    @ConfigItem(keyName = "levelGoalsEnabled", name = "Level goals", description = "Show skill levels, combat level, and total level.", section = GOALS_SECTION, position = 0)
    default boolean levelGoalsEnabled() {
        return true;
    }

    @ConfigItem(keyName = "experienceGoalsEnabled", name = "Experience goals", description = "Show per-skill experience and total experience.", section = GOALS_SECTION, position = 1)
    default boolean experienceGoalsEnabled() {
        return true;
    }

    @ConfigItem(keyName = "trackerGoalsEnabled", name = "Tracker goals", description = "Show kill-count and completion trackers.", section = GOALS_SECTION, position = 2)
    default boolean trackerGoalsEnabled() {
        return true;
    }

    @ConfigItem(keyName = "hideCompletedGoals", name = "Hide completed goals", description = "Keep completed goals out of the main list. You can turn this off at any time.", section = GOALS_SECTION, position = 3)
    default boolean hideCompletedGoals() {
        return false;
    }

    @ConfigItem(keyName = "collectionLogEnabled", name = "Collection log goals", description = "Show collection-log entry and item goals.", section = COLLECTION_LOG_SECTION, position = 0)
    default boolean collectionLogEnabled() {
        return true;
    }

    @ConfigItem(keyName = "showObtainedItems", name = "Show obtained items", description = "Include items you already have in collection-log grids.", section = COLLECTION_LOG_SECTION, position = 1)
    default boolean showObtainedItems() {
        return true;
    }

    @ConfigItem(keyName = "showItemTooltips", name = "Show item tooltips", description = "Show an item name when you hover its icon.", section = COLLECTION_LOG_SECTION, position = 2)
    default boolean showItemTooltips() {
        return true;
    }

    @ConfigItem(keyName = "showGoalIcons", name = "Show goal icons", description = "Show category icons on goal cards.", section = DISPLAY_SECTION, position = 0)
    default boolean showGoalIcons() {
        return true;
    }

    @ConfigItem(keyName = "showProgressBar", name = "Show progress bars", description = "Show visual progress bars below goal cards.", section = DISPLAY_SECTION, position = 1)
    default boolean showProgressBar() {
        return true;
    }

    @Range(min = 1, max = 4)
    @ConfigItem(keyName = "itemsPerRow", name = "Items per row", description = "Maximum collection-log columns. Narrow sidebars automatically use fewer columns to keep icons clear.", section = DISPLAY_SECTION, position = 2)
    default int itemsPerRow() {
        return 4;
    }

    @Range(min = 1, max = 10)
    @ConfigItem(keyName = "maxItemRows", name = "Preview item rows", description = "Rows shown before Show all appears. Expanded grids use the main sidebar scrollbar.", section = DISPLAY_SECTION, position = 3)
    default int maxItemRows() {
        return 5;
    }
}
