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
    String GENERAL_SECTION = "general";
    String DISPLAY_SECTION = "display";
    String CARDS_SECTION = "milestoneCards";
    String OVERLAY_SECTION = "progressOverlay";
    String NOTIFICATIONS_SECTION = "notifications";
    String REMINDERS_SECTION = "reminders";
    String GOALS_SECTION = "goals";
    String SHORTCUTS_SECTION = "shortcuts";
    String COLLECTION_LOG_SECTION = "collectionLog";

    @ConfigSection(name = "General", description = "Additional profile saves. Changes save automatically while logged in.", position = 0, closedByDefault = false)
    String generalSection = GENERAL_SECTION;

    @ConfigItem(keyName = "saveOnLogout", name = "Save on logout", description = "Save again when you log out.", section = GENERAL_SECTION, position = 1)
    default boolean saveOnLogout() {
        return true;
    }

    @ConfigItem(keyName = "saveOnShutdown", name = "Save on shutdown", description = "Save again when Grindstone stops or RuneLite closes.", section = GENERAL_SECTION, position = 2)
    default boolean saveOnShutdown() {
        return true;
    }

    @ConfigSection(name = "Sidebar layout", description = "Sidebar grouping and visible details.", position = 1, closedByDefault = true)
    String displaySection = DISPLAY_SECTION;

    @ConfigItem(keyName = "categorizedGoals", name = "Group by category", description = "Show categories in the sidebar.", section = DISPLAY_SECTION, position = 0)
    default boolean categorizedGoals() {
        return true;
    }

    @ConfigItem(keyName = "groupSkillGoals", name = "Group by skill", description = "Keep level and XP targets for each skill together.", section = DISPLAY_SECTION, position = 1)
    default boolean groupSkillGoals() {
        return true;
    }

    @ConfigItem(keyName = "showPinnedSection", name = "Pinned section", description = "Show pinned milestones above other categories.", section = DISPLAY_SECTION, position = 2)
    default boolean showPinnedSection() {
        return true;
    }

    @ConfigItem(keyName = "groupRecurringGoals", name = "Group routines by reset", description = "Group daily, weekly and monthly routines separately.", section = DISPLAY_SECTION, position = 3)
    default boolean groupRecurringGoals() {
        return true;
    }

    @ConfigItem(keyName = "showTodayOverview", name = "Routine overview", description = "Show routine totals and the next reset in All. Always shown in Routines.", section = DISPLAY_SECTION, position = 4)
    default boolean showTodayOverview() {
        return true;
    }

    @ConfigItem(keyName = "hideCompletedGoals", name = "Hide completed routines", description = "Hide finished routines until reset. Select Done in the sidebar to see them.", section = DISPLAY_SECTION, position = 5)
    default boolean hideCompletedGoals() {
        return false;
    }

    @ConfigItem(keyName = "showUpdateStatus", name = "Game data status", description = "Show storage and collection-log status in the Game data menu.", section = DISPLAY_SECTION, position = 6)
    default boolean showUpdateStatus() {
        return true;
    }

    @ConfigItem(keyName = "showCompletionHistory", name = "Completion history", description = "Show completion history and reminders on milestone cards.", section = DISPLAY_SECTION, position = 7)
    default boolean showCompletionHistory() {
        return true;
    }

    @ConfigItem(keyName = "showDeadlineReminders", name = "Deadline reminders", description = "Show enabled deadline reminders when due within 24 hours or overdue.", section = DISPLAY_SECTION, position = 8)
    default boolean showDeadlineReminders() {
        return true;
    }

    @ConfigSection(name = "Milestone cards", description = "Card icons, progress bars and details.", position = 2, closedByDefault = true)
    String cardsSection = CARDS_SECTION;

    @ConfigItem(keyName = "showGoalIcons", name = "Icons", description = "Show milestone icons.", section = CARDS_SECTION, position = 0)
    default boolean showGoalIcons() {
        return true;
    }

    @ConfigItem(keyName = "showActivityIcons", name = "Activity icons", description = "Use boss, raid and activity icons. Requires Icons.", section = CARDS_SECTION, position = 1)
    default boolean showActivityIcons() {
        return true;
    }

    @ConfigItem(keyName = "showProgressBar", name = "Progress bars", description = "Show progress bars on cards.", section = CARDS_SECTION, position = 2)
    default boolean showProgressBar() {
        return true;
    }

    @Range(min = 1, max = 20)
    @ConfigItem(keyName = "maxProgressChunks", name = "Progress segments", description = "Maximum sections in each progress bar.", section = CARDS_SECTION, position = 3)
    default int maxProgressChunks() {
        return 20;
    }

    @ConfigItem(keyName = "collapseDetailsByDefault", name = "Collapse details initially", description = "Start cards with details collapsed. Expand them to see more.", section = CARDS_SECTION, position = 4)
    default boolean collapseDetailsByDefault() {
        return false;
    }

    @ConfigItem(keyName = "showTrackingState", name = "Period warnings", description = "Mark routines that started tracking partway through a period.", section = CARDS_SECTION, position = 5)
    default boolean showTrackingState() {
        return true;
    }

    @ConfigSection(name = "Progress overlay", description = "Hold Alt to move the overlay.", position = 3, closedByDefault = true)
    String overlaySection = OVERLAY_SECTION;

    @ConfigItem(keyName = "progressOverlayEnabled", name = "Enable overlay", description = "Show milestone progress beside the game.", section = OVERLAY_SECTION, position = 0)
    default boolean progressOverlayEnabled() {
        return false;
    }

    @ConfigItem(keyName = "progressOverlayTheme", name = "Style", description = "Choose Guild, Slate, Classic or RuneLite. Completion popups use Guild.", section = OVERLAY_SECTION, position = 1)
    default OverlayTheme progressOverlayTheme() {
        return OverlayTheme.GUILD;
    }

    @ConfigItem(keyName = "progressOverlayView", name = "Show", description = "Choose which milestones and routines to show.", section = OVERLAY_SECTION, position = 2)
    default ProgressOverlayView progressOverlayView() {
        return ProgressOverlayView.DAILY;
    }

    @Range(min = 1, max = 20)
    @ConfigItem(keyName = "progressOverlayRows", name = "Entry limit", description = "Maximum entries shown. Smaller game windows may show fewer.", section = OVERLAY_SECTION, position = 3)
    default int progressOverlayRows() {
        return 8;
    }

    @ConfigItem(keyName = "progressOverlayWidth", name = "Width (pixels)", description = "Overlay width in pixels.", section = OVERLAY_SECTION, position = 4, hidden = true)
    default int progressOverlayWidth() {
        return 240;
    }

    @ConfigItem(keyName = "progressOverlayBars", name = "Progress bars", description = "Show progress bars in the overlay.", section = OVERLAY_SECTION, position = 5)
    default boolean progressOverlayBars() {
        return true;
    }

    @ConfigItem(keyName = "progressOverlayCompletedRoutines", name = "Keep completed routines", description = "Show completed routines until reset. Completed one-time milestones are hidden.", section = OVERLAY_SECTION, position = 6)
    default boolean progressOverlayCompletedRoutines() {
        return true;
    }

    @ConfigSection(name = "Completion alerts", description = "Popups, chat alerts and desktop notifications.", position = 4, closedByDefault = true)
    String notificationsSection = NOTIFICATIONS_SECTION;

    @ConfigItem(keyName = "completionPopup", name = "Show popup", description = "Show a Guild popup when a milestone is newly completed.", section = NOTIFICATIONS_SECTION, position = 0)
    default boolean completionPopup() {
        return true;
    }

    @ConfigItem(keyName = "completionPopupAnimations", name = "Popup animation", description = "Animate the popup sliding in and out. Turn off for a fade.", section = NOTIFICATIONS_SECTION, position = 1)
    default boolean completionPopupAnimations() {
        return true;
    }

    @Range(min = 3, max = 15)
    @ConfigItem(keyName = "completionPopupSeconds", name = "Popup duration (seconds)", description = "Time each popup stays visible.", section = NOTIFICATIONS_SECTION, position = 2)
    default int completionPopupSeconds() {
        return 6;
    }

    @ConfigItem(keyName = "completionChat", name = "Chat messages", description = "Show private completion messages in chat.", section = NOTIFICATIONS_SECTION, position = 3)
    default boolean completionChat() {
        return true;
    }

    @ConfigItem(keyName = "completionNumbers", name = "Progress numbers", description = "Include current progress and targets.", section = NOTIFICATIONS_SECTION, position = 4)
    default boolean completionNumbers() {
        return true;
    }

    @ConfigItem(keyName = "notificationColours", name = "Chat highlights", description = "Use your RuneLite chat colours for alerts.", section = NOTIFICATIONS_SECTION, position = 5)
    default boolean notificationColours() {
        return true;
    }

    @ConfigItem(keyName = "completionNotification", name = "Desktop notification", description = "Send RuneLite notifications. Use the cogwheel to choose delivery. Game messages may duplicate chat alerts.", section = NOTIFICATIONS_SECTION, position = 6)
    default Notification completionNotification() {
        return Notification.OFF;
    }

    @ConfigItem(keyName = "completionPopupTheme", name = "Popup style", description = "Completion popups always use Guild.", section = NOTIFICATIONS_SECTION, position = 7, hidden = true)
    default OverlayTheme completionPopupTheme() {
        return OverlayTheme.GUILD;
    }

    @ConfigSection(name = "Routine reminders", description = "Choose when and how to receive routine reminders.", position = 5, closedByDefault = true)
    String remindersSection = REMINDERS_SECTION;

    @ConfigItem(keyName = "routineReminders", name = "Enable reminders", description = "Remind you about unfinished routines while logged in.", section = REMINDERS_SECTION, position = 0)
    default boolean routineReminders() {
        return false;
    }

    @ConfigItem(keyName = "dailyReminders", name = "Daily routines", description = "Remind about unfinished daily routines.", section = REMINDERS_SECTION, position = 1)
    default boolean dailyReminders() {
        return true;
    }

    @ConfigItem(keyName = "weeklyReminders", name = "Weekly routines", description = "Remind about unfinished weekly routines.", section = REMINDERS_SECTION, position = 2)
    default boolean weeklyReminders() {
        return true;
    }

    @ConfigItem(keyName = "monthlyReminders", name = "Monthly routines", description = "Remind about unfinished monthly routines.", section = REMINDERS_SECTION, position = 3)
    default boolean monthlyReminders() {
        return true;
    }

    @ConfigItem(keyName = "routineReminderOnLogin", name = "When routines load", description = "Remind when routines load. Reconnects still respect the reminder interval.", section = REMINDERS_SECTION, position = 4)
    default boolean routineReminderOnLogin() {
        return true;
    }

    @ConfigItem(keyName = "routineReminderOnReset", name = "After reset", description = "Remind after a routine resets while logged in.", section = REMINDERS_SECTION, position = 5)
    default boolean routineReminderOnReset() {
        return true;
    }

    @Range(min = 5, max = 240)
    @ConfigItem(keyName = "routineReminderMinutes", name = "Interval (minutes)", description = "Minimum minutes between reminders for each schedule.", section = REMINDERS_SECTION, position = 6)
    default int routineReminderMinutes() {
        return 60;
    }

    @ConfigItem(keyName = "routineReminderChat", name = "Chat messages", description = "Show private chat reminders. Colours use the Chat highlights setting.", section = REMINDERS_SECTION, position = 7)
    default boolean routineReminderChat() {
        return true;
    }

    @ConfigItem(keyName = "routineReminderNumbers", name = "Progress numbers", description = "Include current progress and targets.", section = REMINDERS_SECTION, position = 8)
    default boolean routineReminderNumbers() {
        return true;
    }

    @ConfigItem(keyName = "routineReminderCountdown", name = "Reset countdown", description = "Show time until the next UTC reset.", section = REMINDERS_SECTION, position = 9)
    default boolean routineReminderCountdown() {
        return true;
    }

    @ConfigItem(keyName = "routineReminderNotification", name = "Desktop notification", description = "Send RuneLite notifications for unfinished routines. Use the cogwheel to choose delivery.", section = REMINDERS_SECTION, position = 10)
    default Notification routineReminderNotification() {
        return Notification.OFF;
    }

    @ConfigSection(name = "Milestone types", description = "Choose which milestone types to show. Hidden milestones stay saved.", position = 6, closedByDefault = true)
    String goalsSection = GOALS_SECTION;

    @ConfigItem(keyName = "levelGoalsEnabled", name = "Levels", description = "Show skill, combat, total and base levels.", section = GOALS_SECTION, position = 0)
    default boolean levelGoalsEnabled() {
        return true;
    }

    @ConfigItem(keyName = "experienceGoalsEnabled", name = "Experience", description = "Show skill, total, base and session XP.", section = GOALS_SECTION, position = 1)
    default boolean experienceGoalsEnabled() {
        return true;
    }

    @ConfigItem(keyName = "trackerGoalsEnabled", name = "Boss & activity counts", description = "Show boss and activity counters.", section = GOALS_SECTION, position = 2)
    default boolean trackerGoalsEnabled() {
        return true;
    }

    @ConfigItem(keyName = "questGoalsEnabled", name = "Quests", description = "Show quests and quest series.", section = GOALS_SECTION, position = 3)
    default boolean questGoalsEnabled() {
        return true;
    }

    @ConfigItem(keyName = "diaryGoalsEnabled", name = "Achievement diaries", description = "Show diary regions and tiers.", section = GOALS_SECTION, position = 4)
    default boolean diaryGoalsEnabled() {
        return true;
    }

    @ConfigItem(keyName = "combatAchievementGoalsEnabled", name = "Combat achievements", description = "Show task, boss, difficulty and reward-tier targets.", section = GOALS_SECTION, position = 5)
    default boolean combatAchievementGoalsEnabled() {
        return true;
    }

    @ConfigItem(keyName = "collectionLogEnabled", name = "Collection log", description = "Show collection-log page and item milestones. Grid appearance is configured under Collection log.", section = GOALS_SECTION, position = 6)
    default boolean collectionLogEnabled() {
        return true;
    }

    @ConfigItem(keyName = "ownedItemGoalsEnabled", name = "Owned items", description = "Count items in known personal storage.", section = GOALS_SECTION, position = 7)
    default boolean ownedItemGoalsEnabled() {
        return true;
    }

    @ConfigItem(keyName = "customGoalsEnabled", name = "Custom milestones", description = "Show personal milestones and checklists.", section = GOALS_SECTION, position = 8)
    default boolean customGoalsEnabled() {
        return true;
    }

    @ConfigItem(keyName = "linkedGoalsEnabled", name = "Milestone groups", description = "Show milestone groups, checkpoints and prerequisites.", section = GOALS_SECTION, position = 9)
    default boolean linkedGoalsEnabled() {
        return true;
    }

    @ConfigSection(name = "Right-click shortcuts", description = "Create milestones from in-game right-click menus.", position = 7, closedByDefault = true)
    String shortcutsSection = SHORTCUTS_SECTION;

    @ConfigItem(keyName = "skillGoalMenu", name = "Skills tab", description = "Right-click skills for level or XP milestones. Total level also offers total and base targets.", section = SHORTCUTS_SECTION, position = 0)
    default boolean skillGoalMenu() {
        return true;
    }

    @ConfigItem(keyName = "combatLevelGoalMenu", name = "Combat level", description = "Right-click Combat Options to track your combat level.", section = SHORTCUTS_SECTION, position = 1)
    default boolean combatLevelGoalMenu() {
        return true;
    }

    @ConfigItem(keyName = "bossGoalMenu", name = "Bosses", description = "Right-click bosses to track kills or Combat Achievement tasks.", section = SHORTCUTS_SECTION, position = 2)
    default boolean bossGoalMenu() {
        return true;
    }

    @ConfigItem(keyName = "questGoalMenu", name = "Quests", description = "Right-click quests to track them.", section = SHORTCUTS_SECTION, position = 3)
    default boolean questGoalMenu() {
        return true;
    }

    @ConfigItem(keyName = "diaryGoalMenu", name = "Achievement diaries", description = "Right-click diary regions to choose a tier.", section = SHORTCUTS_SECTION, position = 4)
    default boolean diaryGoalMenu() {
        return true;
    }

    @ConfigItem(keyName = "combatAchievementGoalMenu", name = "Combat tasks", description = "Right-click Combat Achievement tasks to track them.", section = SHORTCUTS_SECTION, position = 5)
    default boolean combatAchievementGoalMenu() {
        return true;
    }

    @ConfigItem(keyName = "collectionLogGoalMenu", name = "Collection log", description = "Right-click collection-log items and pages to track them.", section = SHORTCUTS_SECTION, position = 6)
    default boolean collectionLogGoalMenu() {
        return true;
    }

    @ConfigSection(name = "Collection log", description = "Item grids on collection-log cards.", position = 8, closedByDefault = true)
    String collectionLogSection = COLLECTION_LOG_SECTION;

    @ConfigItem(keyName = "showObtainedItems", name = "Show obtained items", description = "Include unlocked items.", section = COLLECTION_LOG_SECTION, position = 0)
    default boolean showObtainedItems() {
        return false;
    }

    @Range(min = 1, max = 4)
    @ConfigItem(keyName = "itemsPerRow", name = "Columns", description = "Maximum columns. Narrow sidebars use fewer.", section = COLLECTION_LOG_SECTION, position = 1)
    default int itemsPerRow() {
        return 4;
    }

    @Range(min = 1, max = 10)
    @ConfigItem(keyName = "maxItemRows", name = "Preview rows", description = "Rows shown before Show all.", section = COLLECTION_LOG_SECTION, position = 2)
    default int maxItemRows() {
        return 5;
    }

    @ConfigItem(keyName = "showItemTooltips", name = "Item tooltips", description = "Show item names on hover.", section = COLLECTION_LOG_SECTION, position = 3)
    default boolean showItemTooltips() {
        return true;
    }
}
