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

    @ConfigSection(name = "General", description = "Enable tracking and choose when to make additional profile saves. Milestone changes also autosave while logged in.", position = 0, closedByDefault = false)
    String generalSection = GENERAL_SECTION;

    @ConfigItem(keyName = "enabled", name = "Enable Grindstone", description = "Show the sidebar button and track your milestones while logged in.", section = GENERAL_SECTION, position = 0)
    default boolean enabled() {
        return true;
    }

    @ConfigItem(keyName = "saveOnLogout", name = "Save on logout", description = "Make an additional profile save when you log out. Changes also autosave while logged in.", section = GENERAL_SECTION, position = 1)
    default boolean saveOnLogout() {
        return true;
    }

    @ConfigItem(keyName = "saveOnShutdown", name = "Save on shutdown", description = "Make an additional profile save when the plugin stops or RuneLite closes. Changes also autosave while logged in.", section = GENERAL_SECTION, position = 2)
    default boolean saveOnShutdown() {
        return true;
    }

    @ConfigSection(name = "Sidebar layout", description = "Organize the milestone list, routines and information shown in the side panel.", position = 1, closedByDefault = true)
    String displaySection = DISPLAY_SECTION;

    @ConfigItem(keyName = "categorizedGoals", name = "Group by category", description = "Organize unpinned milestones by category. Turn off to use a single sorted list.", section = DISPLAY_SECTION, position = 0)
    default boolean categorizedGoals() {
        return true;
    }

    @ConfigItem(keyName = "groupSkillGoals", name = "Group by skill", description = "Keep related level and XP targets together inside each category.", section = DISPLAY_SECTION, position = 1)
    default boolean groupSkillGoals() {
        return true;
    }

    @ConfigItem(keyName = "showPinnedSection", name = "Pinned section", description = "Keep pinned milestones above the rest of the list. Turn off to show them in their usual categories.", section = DISPLAY_SECTION, position = 2)
    default boolean showPinnedSection() {
        return true;
    }

    @ConfigItem(keyName = "groupRecurringGoals", name = "Group routines by reset", description = "Separate daily, weekly and monthly routines, with UTC reset countdowns.", section = DISPLAY_SECTION, position = 3)
    default boolean groupRecurringGoals() {
        return true;
    }

    @ConfigItem(keyName = "showTodayOverview", name = "Routine overview", description = "Show completion totals and the next reset above the All view. The Routines view always includes this overview.", section = DISPLAY_SECTION, position = 4)
    default boolean showTodayOverview() {
        return true;
    }

    @ConfigItem(keyName = "hideCompletedGoals", name = "Hide completed routines", description = "Hide finished routines until their next reset. Finished one-time milestones are already hidden. Choose Done in the sidebar to see completed milestones.", section = DISPLAY_SECTION, position = 5)
    default boolean hideCompletedGoals() {
        return false;
    }

    @ConfigItem(keyName = "showUpdateStatus", name = "Game data status", description = "Show Game data in the options menu, with captured storage and collection-log readings and refresh tips.", section = DISPLAY_SECTION, position = 6)
    default boolean showUpdateStatus() {
        return true;
    }

    @ConfigItem(keyName = "showCompletionHistory", name = "Completion history", description = "Show routine period history, observed lifetime completions and reminders on cards. Turning this off keeps saved history.", section = DISPLAY_SECTION, position = 7)
    default boolean showCompletionHistory() {
        return true;
    }

    @ConfigItem(keyName = "showDeadlineReminders", name = "Deadline reminders", description = "Show opted-in personal deadlines in the sidebar header when due within 24 hours or overdue. These are local reminders, separate from routine resets.", section = DISPLAY_SECTION, position = 8)
    default boolean showDeadlineReminders() {
        return true;
    }

    @ConfigSection(name = "Milestone cards", description = "Change card icons, progress bars and the details shown below each milestone.", position = 2, closedByDefault = true)
    String cardsSection = CARDS_SECTION;

    @ConfigItem(keyName = "showGoalIcons", name = "Icons", description = "Show an icon beside each milestone title.", section = CARDS_SECTION, position = 0)
    default boolean showGoalIcons() {
        return true;
    }

    @ConfigItem(keyName = "showActivityIcons", name = "Activity icons", description = "Use activity-specific native item icons instead of category icons. Requires Icons above.", section = CARDS_SECTION, position = 1)
    default boolean showActivityIcons() {
        return true;
    }

    @ConfigItem(keyName = "showProgressBar", name = "Progress bars", description = "Show a visual progress bar on each card.", section = CARDS_SECTION, position = 2)
    default boolean showProgressBar() {
        return true;
    }

    @Range(min = 1, max = 20)
    @ConfigItem(keyName = "maxProgressChunks", name = "Progress segments", description = "Maximum segments in sidebar progress bars. For a target of 100 with 20 segments, each segment represents 5.", section = CARDS_SECTION, position = 3)
    default int maxProgressChunks() {
        return 20;
    }

    @ConfigItem(keyName = "collapseDetailsByDefault", name = "Collapse details initially", description = "Start card descriptions and details collapsed until you choose otherwise for that milestone. Progress stays visible.", section = CARDS_SECTION, position = 4)
    default boolean collapseDetailsByDefault() {
        return false;
    }

    @ConfigItem(keyName = "showTrackingState", name = "Period warnings", description = "Flag routines with partially observed periods. Source details remain available by hovering the title.", section = CARDS_SECTION, position = 5)
    default boolean showTrackingState() {
        return true;
    }

    @ConfigSection(name = "Progress overlay", description = "Configure the movable tracker in the game view. Hold Alt to move it.", position = 3, closedByDefault = true)
    String overlaySection = OVERLAY_SECTION;

    @ConfigItem(keyName = "progressOverlayEnabled", name = "Enable overlay", description = "Show a movable progress tracker in the game view, including when the sidebar is closed.", section = OVERLAY_SECTION, position = 0)
    default boolean progressOverlayEnabled() {
        return false;
    }

    @ConfigItem(keyName = "progressOverlayTheme", name = "Style", description = "Choose Classic, Slate or Guild for this tracker. Completion popups always use Guild.", section = OVERLAY_SECTION, position = 1)
    default OverlayTheme progressOverlayTheme() {
        return OverlayTheme.GUILD;
    }

    @ConfigItem(keyName = "progressOverlayView", name = "Show", description = "Choose daily routines, all routines, one-time milestones, pinned entries or everything.", section = OVERLAY_SECTION, position = 2)
    default ProgressOverlayView progressOverlayView() {
        return ProgressOverlayView.DAILY;
    }

    @Range(min = 1, max = 20)
    @ConfigItem(keyName = "progressOverlayRows", name = "Entry limit", description = "Maximum entries to show. Small game windows use fewer rows; additional entries remain in the sidebar.", section = OVERLAY_SECTION, position = 3)
    default int progressOverlayRows() {
        return 8;
    }

    @Range(min = 180, max = 360)
    @ConfigItem(keyName = "progressOverlayWidth", name = "Width (pixels)", description = "Set the tracker width in pixels.", section = OVERLAY_SECTION, position = 4)
    default int progressOverlayWidth() {
        return 240;
    }

    @ConfigItem(keyName = "progressOverlayBars", name = "Progress bars", description = "Show a progress bar below each tracker entry.", section = OVERLAY_SECTION, position = 5)
    default boolean progressOverlayBars() {
        return true;
    }

    @ConfigItem(keyName = "progressOverlayCompletedRoutines", name = "Keep completed routines", description = "Keep finished routines checked off until their next reset. Finished one-time milestones stay hidden.", section = OVERLAY_SECTION, position = 6)
    default boolean progressOverlayCompletedRoutines() {
        return true;
    }

    @ConfigSection(name = "Completion alerts", description = "Choose completion popups, chat messages and desktop notifications.", position = 4, closedByDefault = true)
    String notificationsSection = NOTIFICATIONS_SECTION;

    @ConfigItem(keyName = "completionPopup", name = "Show popup", description = "Show a Guild popup for each newly completed milestone. Loading or adding an already-complete milestone does not trigger an alert.", section = NOTIFICATIONS_SECTION, position = 0)
    default boolean completionPopup() {
        return true;
    }

    @ConfigItem(keyName = "completionPopupAnimations", name = "Popup animation", description = "Animate popup entry and exit. Turn off for a short stationary fade.", section = NOTIFICATIONS_SECTION, position = 1)
    default boolean completionPopupAnimations() {
        return true;
    }

    @Range(min = 3, max = 15)
    @ConfigItem(keyName = "completionPopupSeconds", name = "Popup duration (seconds)", description = "How long each completion popup stays on screen.", section = NOTIFICATIONS_SECTION, position = 2)
    default int completionPopupSeconds() {
        return 6;
    }

    @ConfigItem(keyName = "completionChat", name = "Chat messages", description = "Show completion messages in your local chat. Nothing is sent to other players.", section = NOTIFICATIONS_SECTION, position = 3)
    default boolean completionChat() {
        return true;
    }

    @ConfigItem(keyName = "completionNumbers", name = "Progress numbers", description = "Include exact current and target values in completion messages.", section = NOTIFICATIONS_SECTION, position = 4)
    default boolean completionNumbers() {
        return true;
    }

    @ConfigItem(keyName = "notificationColours", name = "Chat highlights", description = "Use your RuneLite chat colours for completion and routine reminder headings.", section = NOTIFICATIONS_SECTION, position = 5)
    default boolean notificationColours() {
        return true;
    }

    @ConfigItem(keyName = "completionNotification", name = "Desktop notification", description = "Use RuneLite desktop and sound notifications. Customize delivery with the cogwheel; its game-message option may duplicate Chat messages above.", section = NOTIFICATIONS_SECTION, position = 6)
    default Notification completionNotification() {
        return Notification.OFF;
    }

    @ConfigItem(keyName = "completionPopupTheme", name = "Popup style", description = "Completion popups always use Guild.", section = NOTIFICATIONS_SECTION, position = 7, hidden = true)
    default OverlayTheme completionPopupTheme() {
        return OverlayTheme.GUILD;
    }

    @ConfigSection(name = "Routine reminders", description = "Choose which unfinished routines trigger reminders, when to remind you and how to deliver them.", position = 5, closedByDefault = true)
    String remindersSection = REMINDERS_SECTION;

    @ConfigItem(keyName = "routineReminders", name = "Enable reminders", description = "Remind you about unfinished tracked routines while logged in.", section = REMINDERS_SECTION, position = 0)
    default boolean routineReminders() {
        return false;
    }

    @ConfigItem(keyName = "dailyReminders", name = "Daily routines", description = "Include unfinished daily routines when reminders are enabled.", section = REMINDERS_SECTION, position = 1)
    default boolean dailyReminders() {
        return true;
    }

    @ConfigItem(keyName = "weeklyReminders", name = "Weekly routines", description = "Include unfinished weekly routines when reminders are enabled.", section = REMINDERS_SECTION, position = 2)
    default boolean weeklyReminders() {
        return true;
    }

    @ConfigItem(keyName = "monthlyReminders", name = "Monthly routines", description = "Include unfinished monthly routines when reminders are enabled.", section = REMINDERS_SECTION, position = 3)
    default boolean monthlyReminders() {
        return true;
    }

    @ConfigItem(keyName = "routineReminderOnLogin", name = "When routines load", description = "Remind when routines first load. Reconnecting on the same account does not bypass the interval.", section = REMINDERS_SECTION, position = 4)
    default boolean routineReminderOnLogin() {
        return true;
    }

    @ConfigItem(keyName = "routineReminderOnReset", name = "After reset", description = "Remind when a new daily, weekly or monthly period begins while logged in.", section = REMINDERS_SECTION, position = 5)
    default boolean routineReminderOnReset() {
        return true;
    }

    @Range(min = 5, max = 240)
    @ConfigItem(keyName = "routineReminderMinutes", name = "Interval (minutes)", description = "Minimum time between reminders for each routine schedule. Only unfinished routines are included.", section = REMINDERS_SECTION, position = 6)
    default int routineReminderMinutes() {
        return 60;
    }

    @ConfigItem(keyName = "routineReminderChat", name = "Chat messages", description = "Show reminders in your local chat. Nothing is sent to other players. Chat highlights are configured under Completion alerts.", section = REMINDERS_SECTION, position = 7)
    default boolean routineReminderChat() {
        return true;
    }

    @ConfigItem(keyName = "routineReminderNumbers", name = "Progress numbers", description = "Include exact current and target values, with XP labelled where applicable.", section = REMINDERS_SECTION, position = 8)
    default boolean routineReminderNumbers() {
        return true;
    }

    @ConfigItem(keyName = "routineReminderCountdown", name = "Reset countdown", description = "Include the time remaining until the routine's configured UTC reset.", section = REMINDERS_SECTION, position = 9)
    default boolean routineReminderCountdown() {
        return true;
    }

    @ConfigItem(keyName = "routineReminderNotification", name = "Desktop notification", description = "Use RuneLite desktop and sound notifications for unfinished routines. Customize delivery with the cogwheel; Chat messages above controls local chat.", section = REMINDERS_SECTION, position = 10)
    default Notification routineReminderNotification() {
        return Notification.OFF;
    }

    @ConfigSection(name = "Milestone types", description = "Choose which milestone types are visible. Turning a type off keeps its saved milestones and progress.", position = 6, closedByDefault = true)
    String goalsSection = GOALS_SECTION;

    @ConfigItem(keyName = "levelGoalsEnabled", name = "Levels", description = "Show skill, combat, total and base level milestones.", section = GOALS_SECTION, position = 0)
    default boolean levelGoalsEnabled() {
        return true;
    }

    @ConfigItem(keyName = "experienceGoalsEnabled", name = "Experience", description = "Show skill, total, base and session XP milestones.", section = GOALS_SECTION, position = 1)
    default boolean experienceGoalsEnabled() {
        return true;
    }

    @ConfigItem(keyName = "trackerGoalsEnabled", name = "Boss & activity counts", description = "Show kill-count and activity completion trackers.", section = GOALS_SECTION, position = 2)
    default boolean trackerGoalsEnabled() {
        return true;
    }

    @ConfigItem(keyName = "questGoalsEnabled", name = "Quests", description = "Show individual quests and quest series.", section = GOALS_SECTION, position = 3)
    default boolean questGoalsEnabled() {
        return true;
    }

    @ConfigItem(keyName = "diaryGoalsEnabled", name = "Achievement diaries", description = "Show regional and all-region diary tier milestones.", section = GOALS_SECTION, position = 4)
    default boolean diaryGoalsEnabled() {
        return true;
    }

    @ConfigItem(keyName = "combatAchievementGoalsEnabled", name = "Combat achievements", description = "Show boss, tier, difficulty and individual combat-task milestones.", section = GOALS_SECTION, position = 5)
    default boolean combatAchievementGoalsEnabled() {
        return true;
    }

    @ConfigItem(keyName = "collectionLogEnabled", name = "Collection log", description = "Show collection-log page and item milestones. Grid appearance is configured under Collection log.", section = GOALS_SECTION, position = 6)
    default boolean collectionLogEnabled() {
        return true;
    }

    @ConfigItem(keyName = "ownedItemGoalsEnabled", name = "Owned items", description = "Show item quantity milestones across known personal storage.", section = GOALS_SECTION, position = 7)
    default boolean ownedItemGoalsEnabled() {
        return true;
    }

    @ConfigItem(keyName = "customGoalsEnabled", name = "Custom milestones", description = "Show manual progress milestones, checklists and notes.", section = GOALS_SECTION, position = 8)
    default boolean customGoalsEnabled() {
        return true;
    }

    @ConfigItem(keyName = "linkedGoalsEnabled", name = "Linked milestones", description = "Show linked checklists, ladders, prerequisites, achievement targets and repeat-completion targets.", section = GOALS_SECTION, position = 9)
    default boolean linkedGoalsEnabled() {
        return true;
    }

    @ConfigSection(name = "Right-click shortcuts", description = "Open milestone editors from in-game right-click menus. These shortcuts do not perform game actions.", position = 7, closedByDefault = true)
    String shortcutsSection = SHORTCUTS_SECTION;

    @ConfigItem(keyName = "skillGoalMenu", name = "Skills tab", description = "Add level and XP shortcuts to skills. Right-click Total level for total and base level or XP milestones.", section = SHORTCUTS_SECTION, position = 0)
    default boolean skillGoalMenu() {
        return true;
    }

    @ConfigItem(keyName = "combatLevelGoalMenu", name = "Combat level", description = "Right-click the level display in Combat Options to open a combat-level milestone.", section = SHORTCUTS_SECTION, position = 1)
    default boolean combatLevelGoalMenu() {
        return true;
    }

    @ConfigItem(keyName = "bossGoalMenu", name = "Bosses", description = "Add Track kills to supported bosses and Track boss achievements to Combat Achievements boss entries.", section = SHORTCUTS_SECTION, position = 2)
    default boolean bossGoalMenu() {
        return true;
    }

    @ConfigItem(keyName = "questGoalMenu", name = "Quests", description = "Right-click a quest in the quest list to open its milestone editor.", section = SHORTCUTS_SECTION, position = 3)
    default boolean questGoalMenu() {
        return true;
    }

    @ConfigItem(keyName = "diaryGoalMenu", name = "Achievement diaries", description = "Right-click a diary region to choose an Easy, Medium, Hard or Elite milestone.", section = SHORTCUTS_SECTION, position = 4)
    default boolean diaryGoalMenu() {
        return true;
    }

    @ConfigItem(keyName = "combatAchievementGoalMenu", name = "Combat tasks", description = "Add Track task to Combat Achievement task menus.", section = SHORTCUTS_SECTION, position = 5)
    default boolean combatAchievementGoalMenu() {
        return true;
    }

    @ConfigItem(keyName = "collectionLogGoalMenu", name = "Collection log", description = "Add Track unlock to collection-log items and Track page to collection-log pages.", section = SHORTCUTS_SECTION, position = 6)
    default boolean collectionLogGoalMenu() {
        return true;
    }

    @ConfigSection(name = "Collection log", description = "Adjust item grids on collection-log milestone cards. Tracking and shortcuts have their own sections.", position = 8, closedByDefault = true)
    String collectionLogSection = COLLECTION_LOG_SECTION;

    @ConfigItem(keyName = "showObtainedItems", name = "Show obtained items", description = "Include already-obtained items in the grid.", section = COLLECTION_LOG_SECTION, position = 0)
    default boolean showObtainedItems() {
        return false;
    }

    @Range(min = 1, max = 4)
    @ConfigItem(keyName = "itemsPerRow", name = "Columns", description = "Maximum item columns. Narrow sidebars automatically use fewer columns to keep icons clear.", section = COLLECTION_LOG_SECTION, position = 1)
    default int itemsPerRow() {
        return 4;
    }

    @Range(min = 1, max = 10)
    @ConfigItem(keyName = "maxItemRows", name = "Preview rows", description = "Rows shown before Show all appears. Expanded grids use the main sidebar scrollbar.", section = COLLECTION_LOG_SECTION, position = 2)
    default int maxItemRows() {
        return 5;
    }

    @ConfigItem(keyName = "showItemTooltips", name = "Item tooltips", description = "Show an item name when you hover its icon.", section = COLLECTION_LOG_SECTION, position = 3)
    default boolean showItemTooltips() {
        return true;
    }
}
