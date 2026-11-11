# Grindstone

[![Version](https://img.shields.io/badge/version-1.1.3-gold)](https://github.com/Alshaklawi/Grindstone)
[![Installs](https://img.shields.io/endpoint?url=https%3A%2F%2Fapi.runelite.net%2Fpluginhub%2Fshields%2Finstalls%2Fplugin%2Fgrindstone)](https://runelite.net/plugin-hub/show/grindstone)
[![Plugin Rank](https://img.shields.io/endpoint?url=https%3A%2F%2Fapi.runelite.net%2Fpluginhub%2Fshields%2Frank%2Fplugin%2Fgrindstone)](https://runelite.net/plugin-hub/show/grindstone)
[![Sponsor](https://img.shields.io/badge/Sponsor-GitHub%20Sponsors-ea4aaa?logo=github)](https://github.com/sponsors/Alshaklawi)

**Your next level. Your next unlock. Your next milestone.**

Track skills, boss kills, collection-log unlocks, supplies and personal milestones in one RuneLite sidebar. Pin your priorities, build routines and celebrate completed targets.

## Your milestones

Search and filter by status, category, type or schedule. Pin important targets, group related skills, and expand cards for details and actions.

<img src="docs/images/sidebar-current.png" width="180" alt="Current Grindstone sidebar with skill, Slayer and Fractured Archive milestones">

*Images show in-memory examples rendered with the current plugin components, not account data.*

## Get started

1. Enable **Grindstone**, log in and open its RuneLite sidebar.
2. Select **+ Add milestone**, choose a subject and set your target.
3. Save your milestone and play. Supported trackers update from available game data.

Track levels and XP, bosses and activities, quests and diaries, Combat Achievements, collection-log unlocks and owned items. Personal milestones support manual counters, notes and checklists; groups combine existing milestones.

Right-click supported skills, quests, diary regions, collection-log entries, Combat Achievements or boss NPCs to start with the subject selected. Each shortcut can be disabled in settings.

### Personal targets

Set a target and update your own progress. Use the card's **+1**, **Set progress** or checklist controls where available.

<img src="docs/images/custom-milestone-current.png" width="240" alt="Current personal milestone editor with notes, category, progress and target">

### Milestone groups

Open **… → Add group…**, name your group and select existing milestones with their targets. Expand the group to see each milestone's icon and progress bar alongside the combined progress.

This **Combat training** group has one of three XP targets complete.

<img src="docs/images/group-current.png" width="200" alt="Combat training group with native skill icons, individual XP progress bars and one of three targets complete">

When all three targets are reached, the group is complete.

<img src="docs/images/group-complete-current.png" width="200" alt="Completed Combat training group with all three skill XP targets reached and green progress bars">

## Daily, weekly and monthly routines

Create repeating milestones or add a routine preset from the **…** menu. The **Routines** view shows period progress and upcoming UTC resets. Observed periods remain available in **History**.

<img src="docs/images/routines-current.png" width="180" alt="Current Routines sidebar with a period overview and daily milestone cards">

Add personal deadlines and optional reminders without changing the game's reset schedule. In **RuneLite settings → Grindstone → Notifications**, choose completion alerts, routine reminders, chat messages and optional desktop notifications.

## Progress beside the game

Enable **RuneLite settings → Grindstone → Progress overlay**. Choose daily routines, all routines, milestones, pinned entries or all tracked. Hold **Alt** and drag to reposition it. Adjust the entry limit and progress bars in settings.

### RuneLite — default

<img src="docs/images/overlay-runelite-current.png" width="200" alt="Native RuneLite progress overlay with skill icons, progress bars and a reset countdown">

### Guild

<img src="docs/images/overlay-guild-current.png" width="200" alt="Current compact Guild overlay with native icons, progress bars and a reset countdown">

### Slate

<img src="docs/images/overlay-slate-current.png" width="200" alt="Current Slate progress overlay showing the same example routines">

Classic is also available. The progress overlay is optional; completion popups always use Guild.

### Completion popups

Newly reached targets appear together in a compact Guild popup. Entry and exit animations can be configured in Notifications. Completion chat colors adapt to RuneLite's chat background, and exact progress numbers can be switched off.

<img src="docs/images/popup-current.png" width="240" alt="Current Guild completion popup for five Fractured Archive raids">

## Saved progress and game data

Sign in to the same RuneLite account on each computer to sync your character's milestones, history and snapshots. RuneLite uses its normal sync schedule. Local recovery copies and manual backups remain available through the **…** menu; milestone packs share definitions without account progress or history.

Open **… → Game data** for capture status and refresh instructions. Bank and other storage snapshots retain their last known contents; reopen storage after playing on another device.

Before adding collection-log milestones, open your in-game collection log and check its capture status in **Game data**. Grindstone verifies loaded unlocks against the game's total; incomplete data may require refreshing the log. **Add all collection-log pages** adds missing page milestones.

Played on mobile or another computer? **Since last seen** summarizes changes from the previous saved reading. Exact achievement times and gains across routine resets cannot be reconstructed.
