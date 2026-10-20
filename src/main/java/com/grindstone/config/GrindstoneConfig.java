package com.grindstone.config;

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
@ConfigGroup(GrindstoneConfig.GROUP)
public interface GrindstoneConfig extends Config {
    String GROUP = "grindstone";
    @ConfigItem(keyName = "showDeadlineReminders", name = "Show deadline reminders", description = "Show opted-in personal deadlines in Grindstone's header within 24 hours and while past due. This is not an external notification or game reset.", section = DISPLAY_SECTION, position = 18)
    default boolean showDeadlineReminders() {
        return true;
    }
    @ConfigItem(keyName = "linkedGoalsEnabled", name = "Linked milestones", description = "Show linked checklists, milestone ladders, prerequisites, achievement milestones and repeat-completion milestones.", section = GOALS_SECTION, position = 9)
    default boolean linkedGoalsEnabled() {
        return true;
    }
    @ConfigItem(keyName = "groupSkillGoals", name = "Group related skill milestones", description = "Keep each skill's level and XP targets together inside its category. Filters, pinning and recurring sections still apply.", section = DISPLAY_SECTION, position = 16)
    default boolean groupSkillGoals() {
        return true;
    }
    @ConfigItem(keyName = "questGoalsEnabled", name = "Quest milestones", description = "Show individual quests and quest series.", section = GOALS_SECTION, position = 4)
    default boolean questGoalsEnabled() {
        return true;
    }
    @ConfigItem(keyName = "diaryGoalsEnabled", name = "Achievement diary milestones", description = "Show region and all-region diary tier milestones.", section = GOALS_SECTION, position = 5)
    default boolean diaryGoalsEnabled() {
        return true;
    }
    @ConfigItem(keyName = "combatAchievementGoalsEnabled", name = "Combat achievement milestones", description = "Show boss, tier, difficulty and individual combat-task milestones.", section = GOALS_SECTION, position = 6)
    default boolean combatAchievementGoalsEnabled() {
        return true;
    }

    @ConfigItem(keyName = "combatAchievementGoalMenu", name = "Combat task milestone shortcut", description = "Add Track task to Combat Achievement right-click menus. Opens the selected task in the milestone editor without performing game actions.", section = GOALS_SECTION, position = 10)
    default boolean combatAchievementGoalMenu() {
        return true;
    }

    @ConfigItem(keyName = "bossGoalMenu", name = "Boss milestone shortcuts", description = "Add Track kills to supported bosses and Track boss achievements to Combat Achievements boss entries. Opens the editor without performing game actions.", section = GOALS_SECTION, position = 11)
    default boolean bossGoalMenu() {
        return true;
    }

    @ConfigItem(keyName = "skillGoalMenu", name = "Skills tab milestone shortcuts", description = "Add level and XP milestone shortcuts to skills. Right-click Total level for total and base level/XP milestones. Opens the milestone editor without changing game stats.", section = GOALS_SECTION, position = 12)
    default boolean skillGoalMenu() {
        return true;
    }

    @ConfigItem(keyName = "combatLevelGoalMenu", name = "Combat level milestone shortcut", description = "Right-click the combat-level display in Combat Options to open a combat-level milestone. Does not change combat settings or perform game actions.", section = GOALS_SECTION, position = 13)
    default boolean combatLevelGoalMenu() {
        return true;
    }

    @ConfigItem(keyName = "questGoalMenu", name = "Quest milestone shortcuts", description = "Right-click a quest in the quest list to open its milestone in Grindstone. No game actions are performed.", section = GOALS_SECTION, position = 14)
    default boolean questGoalMenu() {
        return true;
    }

    @ConfigItem(keyName = "diaryGoalMenu", name = "Diary milestone shortcuts", description = "Right-click an achievement diary region to choose an Easy, Medium, Hard or Elite diary milestone. No game actions are performed.", section = GOALS_SECTION, position = 15)
    default boolean diaryGoalMenu() {
        return true;
    }
    @ConfigItem(keyName = "ownedItemGoalsEnabled", name = "Owned item milestones", description = "Show quantities across known personal item storage.", section = GOALS_SECTION, position = 7)
    default boolean ownedItemGoalsEnabled() {
        return true;
    }
    @ConfigItem(keyName = "customGoalsEnabled", name = "Custom milestones", description = "Show manually managed milestones and notes.", section = GOALS_SECTION, position = 8)
    default boolean customGoalsEnabled() {
        return true;
    }
    @ConfigItem(keyName = "categorizedGoals", name = "Group by category", description = "Group unpinned milestones into categories. Turn off for a single sorted list.", section = DISPLAY_SECTION, position = 6)
    default boolean categorizedGoals() {
        return true;
    }
    @ConfigItem(keyName = "showUpdateStatus", name = "Show snapshots", description = "Show dashboard snapshot status for storage and collection log, with refresh tips.", section = DISPLAY_SECTION, position = 7)
    default boolean showUpdateStatus() {
        return true;
    }
    @ConfigItem(keyName = "showPinnedSection", name = "Show pinned section", description = "Keep pinned milestones above categories. Turning this off displays them in the normal list.", section = DISPLAY_SECTION, position = 8)
    default boolean showPinnedSection() {
        return true;
    }
    @ConfigItem(keyName = "showCompletionHistory", name = "Show milestone history", description = "Show recurring period history, observed lifetime completions and reminders. Saved history is retained.", section = DISPLAY_SECTION, position = 9)
    default boolean showCompletionHistory() {
        return true;
    }
    @ConfigItem(keyName = "showActivityIcons", name = "Activity-specific icons", description = "Use native item icons for activities when milestone icons are enabled.", section = DISPLAY_SECTION, position = 10)
    default boolean showActivityIcons() {
        return true;
    }
    @ConfigItem(keyName = "groupRecurringGoals", name = "Group routines", description = "Group routines by daily, weekly and monthly schedules with UTC reset countdowns.", section = DISPLAY_SECTION, position = 11)
    default boolean groupRecurringGoals() {
        return true;
    }
    @ConfigItem(keyName = "showTodayOverview", name = "Show routine overview", description = "Show recurring completion and next reset above the All view. The Routines view always shows it.", section = DISPLAY_SECTION, position = 12)
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
    @ConfigItem(keyName = "collapseDetailsByDefault", name = "Start details collapsed", description = "Default for milestones you have not expanded or collapsed manually. Progress remains visible.", section = DISPLAY_SECTION, position = 5)
    default boolean collapseDetailsByDefault() {
        return false;
    }
    String GENERAL_SECTION = "general";
    String GOALS_SECTION = "goals";
    String COLLECTION_LOG_SECTION = "collectionLog";
    String DISPLAY_SECTION = "display";
    String NOTIFICATIONS_SECTION = "notifications";
    String OVERLAY_SECTION = "progressOverlay";

    @ConfigSection(name = "Progress overlay", description = "Track milestones and routines beside the game. Hold Alt to move the overlay.", position = 5)
    String overlaySection = OVERLAY_SECTION;

    @ConfigItem(keyName = "progressOverlayEnabled", name = "Show progress overlay", description = "Show a movable progress tracker in the game view, even when the sidebar is closed.", section = OVERLAY_SECTION, position = 0)
    default boolean progressOverlayEnabled() {
        return false;
    }

    @ConfigItem(keyName = "progressOverlayView", name = "Track", description = "Choose daily routines, all routines, milestones, pinned entries or everything.", section = OVERLAY_SECTION, position = 1)
    default ProgressOverlayView progressOverlayView() {
        return ProgressOverlayView.DAILY;
    }

    @Range(min = 1, max = 20)
    @ConfigItem(keyName = "progressOverlayRows", name = "Maximum entries", description = "Show up to this many entries. Smaller game windows use fewer rows; additional entries stay in the sidebar.", section = OVERLAY_SECTION, position = 2)
    default int progressOverlayRows() {
        return 8;
    }

    @ConfigItem(keyName = "progressOverlayCompletedRoutines", name = "Keep completed routines", description = "Keep completed routines checked off until their next reset. Finished one-time milestones stay hidden.", section = OVERLAY_SECTION, position = 3)
    default boolean progressOverlayCompletedRoutines() {
        return true;
    }

    @ConfigItem(keyName = "progressOverlayBars", name = "Progress bars", description = "Show a small progress bar below each entry. Turn off for a shorter tracker.", section = OVERLAY_SECTION, position = 4)
    default boolean progressOverlayBars() {
        return true;
    }

    @Range(min = 180, max = 360)
    @ConfigItem(keyName = "progressOverlayWidth", name = "Width", description = "Width of the progress overlay in pixels.", section = OVERLAY_SECTION, position = 5)
    default int progressOverlayWidth() {
        return 240;
    }

    @ConfigSection(name = "Notifications", description = "Control completion messages, colours and reminders for unfinished routines.", position = 4)
    String notificationsSection = NOTIFICATIONS_SECTION;

    @ConfigItem(keyName = "notificationColours", name = "Colourful chat messages", description = "Colour milestone completions and routine reminders in local chat.", section = NOTIFICATIONS_SECTION, position = 4)
    default boolean notificationColours() {
        return true;
    }

    @ConfigItem(keyName = "completionNumbers", name = "Completion progress numbers", description = "Include exact comma-separated current and target numbers in completion messages.", section = NOTIFICATIONS_SECTION, position = 5)
    default boolean completionNumbers() {
        return true;
    }

    @ConfigItem(keyName = "routineReminders", name = "Enable routine reminders", description = "Remind you about unfinished tracked routines while logged in.", section = NOTIFICATIONS_SECTION, position = 6)
    default boolean routineReminders() {
        return false;
    }

    @ConfigItem(keyName = "dailyReminders", name = "Daily reminders", description = "Include unfinished daily routines.", section = NOTIFICATIONS_SECTION, position = 7)
    default boolean dailyReminders() {
        return true;
    }

    @ConfigItem(keyName = "weeklyReminders", name = "Weekly reminders", description = "Include unfinished weekly routines.", section = NOTIFICATIONS_SECTION, position = 8)
    default boolean weeklyReminders() {
        return true;
    }

    @ConfigItem(keyName = "monthlyReminders", name = "Monthly reminders", description = "Include unfinished monthly routines.", section = NOTIFICATIONS_SECTION, position = 9)
    default boolean monthlyReminders() {
        return true;
    }

    @ConfigItem(keyName = "routineReminderChat", name = "Reminder chat messages", description = "Show routine reminders in local chat. Nothing is sent to other players.", section = NOTIFICATIONS_SECTION, position = 10)
    default boolean routineReminderChat() {
        return true;
    }

    @ConfigItem(keyName = "routineReminderOnLogin", name = "Remind when loaded", description = "Remind when routines first load. Reconnecting on the same account does not bypass the reminder interval.", section = NOTIFICATIONS_SECTION, position = 11)
    default boolean routineReminderOnLogin() {
        return true;
    }

    @Range(min = 5, max = 240)
    @ConfigItem(keyName = "routineReminderMinutes", name = "Reminder interval (minutes)", description = "Minimum time between reminders for each routine schedule while logged in. Only unfinished routines are included.", section = NOTIFICATIONS_SECTION, position = 12)
    default int routineReminderMinutes() {
        return 60;
    }

    @ConfigItem(keyName = "routineReminderNotification", name = "Desktop routine reminders", description = "Optional RuneLite notifications for unfinished routines. The separate reminder chat toggle controls Grindstone chat messages.", section = NOTIFICATIONS_SECTION, position = 13)
    default Notification routineReminderNotification() {
        return Notification.OFF;
    }

    @ConfigItem(keyName = "routineReminderOnReset", name = "Remind after routine reset", description = "Remind about unfinished routines when a new daily, weekly or monthly period starts while logged in.", section = NOTIFICATIONS_SECTION, position = 14)
    default boolean routineReminderOnReset() {
        return true;
    }

    @ConfigItem(keyName = "routineReminderNumbers", name = "Reminder progress numbers", description = "Include exact comma-separated current and target numbers in routine reminders, with XP labelled where applicable.", section = NOTIFICATIONS_SECTION, position = 15)
    default boolean routineReminderNumbers() {
        return true;
    }

    @ConfigItem(keyName = "routineReminderCountdown", name = "Reminder reset countdown", description = "Include the time remaining until the routine's configured UTC reset in reminder messages.", section = NOTIFICATIONS_SECTION, position = 16)
    default boolean routineReminderCountdown() {
        return true;
    }

    @ConfigItem(keyName = "completionPopup", name = "Completion popup", description = "Show a non-blocking RuneLite overlay when a personal milestone is newly completed. Already-complete milestones loaded or added are excluded.", section = NOTIFICATIONS_SECTION, position = 0)
    default boolean completionPopup() {
        return true;
    }

    @ConfigItem(keyName = "completionChat", name = "Completion chat message", description = "Add a local RuneLite chat message for newly completed personal milestones. Nothing is sent to other players.", section = NOTIFICATIONS_SECTION, position = 1)
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

    @ConfigSection(name = "Milestones & routines", description = "Choose what to track and which right-click shortcuts to show.", position = 1)
    String goalsSection = GOALS_SECTION;

    @ConfigSection(name = "Collection Log", description = "Control collection-log cards and item details.", position = 2)
    String collectionLogSection = COLLECTION_LOG_SECTION;

    @ConfigSection(name = "Display", description = "Adjust the side panel layout without affecting your saved milestones.", position = 3)
    String displaySection = DISPLAY_SECTION;

    @ConfigItem(keyName = "enabled", name = "Enable Grindstone", description = "Show the Grindstone button and begin tracking while logged in.", section = GENERAL_SECTION, position = 0)
    default boolean enabled() {
        return true;
    }

    @ConfigItem(keyName = "saveOnLogout", name = "Save on logout", description = "Keep changes made to your milestones when you log out.", section = GENERAL_SECTION, position = 1)
    default boolean saveOnLogout() {
        return true;
    }

    @ConfigItem(keyName = "saveOnShutdown", name = "Save on shutdown", description = "Keep changes made to your milestones when the plugin is disabled or RuneLite closes.", section = GENERAL_SECTION, position = 2)
    default boolean saveOnShutdown() {
        return true;
    }

    @ConfigItem(keyName = "levelGoalsEnabled", name = "Level milestones", description = "Show skill levels, combat level, and total level.", section = GOALS_SECTION, position = 0)
    default boolean levelGoalsEnabled() {
        return true;
    }

    @ConfigItem(keyName = "experienceGoalsEnabled", name = "Experience milestones", description = "Show per-skill experience and total experience.", section = GOALS_SECTION, position = 1)
    default boolean experienceGoalsEnabled() {
        return true;
    }

    @ConfigItem(keyName = "trackerGoalsEnabled", name = "Tracker milestones", description = "Show kill-count and completion trackers.", section = GOALS_SECTION, position = 2)
    default boolean trackerGoalsEnabled() {
        return true;
    }

    @ConfigItem(keyName = "hideCompletedGoals", name = "Hide completed routines", description = "Also hide completed daily, weekly and monthly routines. Completed one-time milestones are hidden automatically. Select Done to view completed milestones.", section = GOALS_SECTION, position = 3)
    default boolean hideCompletedGoals() {
        return false;
    }

    @ConfigItem(keyName = "collectionLogEnabled", name = "Collection log milestones", description = "Show collection-log entry and item milestones.", section = COLLECTION_LOG_SECTION, position = 0)
    default boolean collectionLogEnabled() {
        return true;
    }

    @ConfigItem(keyName = "collectionLogGoalMenu", name = "Collection log milestone shortcut", description = "Add Track unlock to collection-log items and Track page to collection-log pages. Opens the milestone editor without performing game actions.", section = COLLECTION_LOG_SECTION, position = 3)
    default boolean collectionLogGoalMenu() {
        return true;
    }

    @ConfigItem(keyName = "showObtainedItems", name = "Show obtained items", description = "Include items you already have in collection-log grids.", section = COLLECTION_LOG_SECTION, position = 1)
    default boolean showObtainedItems() {
        return false;
    }

    @ConfigItem(keyName = "showItemTooltips", name = "Show item tooltips", description = "Show an item name when you hover its icon.", section = COLLECTION_LOG_SECTION, position = 2)
    default boolean showItemTooltips() {
        return true;
    }

    @ConfigItem(keyName = "showGoalIcons", name = "Show milestone icons", description = "Show category icons on milestone cards.", section = DISPLAY_SECTION, position = 0)
    default boolean showGoalIcons() {
        return true;
    }

    @ConfigItem(keyName = "showProgressBar", name = "Show progress bars", description = "Show visual progress bars below milestone cards.", section = DISPLAY_SECTION, position = 1)
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
