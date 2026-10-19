# Grinder

A Java 11 RuneLite plugin for tracking skills, experience, quests, achievement diaries, combat achievements, collection-log unlocks, and owned item quantities.

## Source layout

- `com.grinder`: plugin lifecycle and event subscriptions.
- `config`: player-facing RuneLite configuration.
- `content`: feature-specific data, storage snapshots, and goal implementations.
- `repository`: player profiles, JSON persistence, and saved goal-type aliases.
- `ui`: main panel and icons, with `components`, `editor`, `item`, and `util` subpackages.
- `util`: reusable text and number formatting.

Saved goals use stable short type IDs, not Java class names. Existing class-tagged profiles remain readable. Register new goal types and legacy aliases in `SavedGoalTypes`; do not load arbitrary class names from profile JSON.

## Development

Use Java 11, then run `./gradlew test` to run the automated tests or `./gradlew run` to launch the development client.

Only a player can confirm behavior in-game. Do not automate game input.

## Goal workflows

- Use a goal's actions menu to edit its subject and target, pin/unpin it, or inspect completion history.
- Pinned goals appear above categories but still respect search and visibility filters.
- The header shows update and bank snapshot freshness. Unknown or unexposed storage is not a confirmed zero, and a snapshot is not a live all-storage guarantee.
- Collection-page choices come from all five native collection-log tabs. The header menu can add all discovered pages without replacing existing goals.
- Activity choices include 87 named client total counters. Representative collection reward icons are used where a matching page exists, with category icons as fallbacks.
- Individual combat-achievement task goals read native task names, descriptions, and completion bits. Defaults include an example of this goal type.
- Display settings support categorized or flat lists, optional sections and icons, and default detail collapsing. Collapsed goal details retain their progress and counts.
- Progress uses integer-sized chunks, capped at 20 by default: a target of 100 uses 20 chunks worth 5 each. The cap is configurable.
- XP, activity and custom goals support daily, weekly and monthly repeat schedules, chosen in the editor or goal actions. Each has a separate period target; automatic goals count gains rather than existing totals.
- Personal schedules reset at midnight UTC, Monday UTC, or the first of the month UTC. They do not represent game-specific activity reset rules. Custom progress resets with the period or a schedule change. Changing repeat settings archives the observed current period.
- Recurring sections show reset countdowns while logged in and can be collapsed or disabled in Display settings. Hidden categories still record recurring progress while Grinder is enabled.
- Goal history has sortable, read-only period and lifetime tables. It shows targets, observed progress, baselines and completion dates, retaining the latest 500 period records. Offline gaps are marked unobserved, not invented failures; automatic goals capture a fresh baseline at the first observation after a reset and cannot attribute offline gains across that boundary.
- Completion history records when Grinder first observed a target completed. Spending items can make a goal incomplete without erasing its previously achieved targets.
- Use the header options menu to add a beginner, skilling, PvM, questing, or completionist preset. Existing matching goals and edited targets are kept.
- Export a private JSON backup from the header options menu. Import merges missing goals, never overwrites existing matches, and clears observed progress when transferring a setup to a different account.
- Backups are local files, not cloud synchronization. Imports accept only registered goal types and supported backup versions.

Follow [the in-game acceptance checklist](IN_GAME_TESTING.md) to verify tracker accuracy, profile isolation, editing, history, presets, and backups. These checks remain pending player confirmation.

Use same-line opening braces, one enum entry per line, and no line-length-based wrapping. Keep commits focused and separate formatting changes from behavior changes.
