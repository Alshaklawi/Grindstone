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

See [policy compatibility and profile migration](COMPLIANCE.md) for the runtime restrictions, storage guarantees and remaining player checks.

Use Java 11, then run `./gradlew test` to run the automated tests or `./gradlew run` to launch the development client.

Only a player can confirm behavior in-game. Do not automate game input.

## Goal workflows

### Expanded goal types and management

- **Base level / base experience:** Add goal → choose the base type. Every client skill must meet the target; the bar is the lowest skill reading, not an average. Base levels exclude boosts. Cards identify the lowest skill and count how many skills meet the target. These are lifetime goals, not period-gain counters.
- **Session experience:** track one skill or total observed XP gains since the first reading this login. Relogging/reconnecting resets the session; restarting one goal captures a fresh baseline. Editing its target keeps its gains. Session readings are not exported or restored.
- **Personal checklist / routine:** enter one step per line, then check steps on the card. Optional daily, weekly and monthly schedules require every step, archive observed progress and reset all checks at the UTC boundary. Checklists support guarded undo; cross-account imports clear checks.
- **Linked goal:** use the header's `+ Linked goal` button. Choose an all-of checklist, any-of count, milestone ladder, selected collection items, spending-safe achievement, completed recurring periods, or a main goal with prerequisites. Search existing source trackers and review captured thresholds before saving. Add missing source trackers first. Linked goals cannot reference other linked goals, preventing cycles.
- Collection subsets accept individual collection-item trackers and count observed unlocks. Owned-item sources retain their known-storage limitations. Milestone ladders use increasing thresholds on one source. Any-of goals finish after enough selected sources meet their thresholds. Prerequisites only gate the personal tracker; they never affect gameplay.
- Spending-safe achievements remember thresholds Grinder observes reached, including relevant recorded lifetime completions. Spending items later does not erase that observation. They cannot invent a historical balance. Repeat-completion goals count distinct observed completed UTC periods, not repeated clicks or unobserved historical activity; retained period history can seed the count. Missing sources remain unavailable until restored or replaced.
- Add-goal reviews identify existing subject/schedule matches and explain reading availability. Default and preset previews list new goals and matches that will be kept. Confirming a preview never authorizes overwriting another account's profile.
- Header menu → **Manage goals / bulk actions** provides searchable selection for pinning, unpinning, deletion, display categories, schedules, deadlines and example flags. Hidden selections remain selected; the count shows this. Unsupported or colliding schedule selections reject the whole operation. Undo restores the batch until a later edit or relevant period reset. Display-category changes do not change a tracker's underlying data source.
- Goal actions → **Deadline / reminder** sets an end-of-day UTC deadline. Reminders are optional, appear only in Grinder's header within 24 hours / while past due, and do not reset or delete progress. Global reminder visibility is configurable.
- Demonstrations are marked as examples and hidden by default. Toggle them from the header menu or Display settings. Each linked mode has an example. Choose **Use as a real goal** or the bulk equivalent to keep an example visible; restoring defaults preserves promoted examples.
- Related skill-level and XP targets share a visual group inside categorized lists, while retaining independent actions, history and progress. Turn **Group related skill goals** off to restore individual cards. Flat lists, pins and recurring sections keep their own layout rules.
- Progress-source tooltips include observation information and source limitations. Names in the new source selectors and management tables are plain text, not executable HTML.

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
- A tracker can have separate lifetime, daily, weekly and monthly goals at once. Duplicate subject/schedule combinations are skipped; targets are never silently overwritten.
- Today focuses on recurring goals in next-reset order. Its routine summary excludes unavailable readings from average progress. All goals keeps the categorized or flat layout and existing filters.
- Custom cards have a completion checkbox for target-1 goals, a +1 control for larger targets, and Set progress. These only update personal tracker data, never game input.
- History opens an account-wide browser with goal/outcome search, UTC date ranges, schedule filters, period summaries and links back to active goals. Deleted or changed-subject goals keep observed history in an archive (latest 500 archived goals), with no stale navigation link.
- Recurring routines in the header menu offer editable daily and weekly packs. Preview the selected goals and targets before adding; matches are skipped. Default, preset, routine and bulk collection-page additions support one-step undo.
- Goal edits and schedule changes show previews. Restart current period archives the observed period and captures a new baseline. The header Undo action restores the latest supported change and expires after later edits, account switches or a relevant period reset.
- Reliability labels distinguish manual progress, live client reads, client-reported counters, known storage snapshots, observed unlocks and captured-period gains. A zero observed unlock count is not proof of an unobtained item; a known-storage total is not an all-storage guarantee.
- Unchanged cards are cached. With stable list membership and ordering, only changed cards and overview/countdown labels are updated; filters, regrouping and reordered goals can still require a layout rebuild.
- Completion history records when Grinder first observed a target completed. Spending items can make a goal incomplete without erasing its previously achieved targets.
- Use the header options menu to add a beginner, skilling, PvM, questing, or completionist preset. Existing matching goals and edited targets are kept.
- Export a private JSON backup from the header options menu. Import merges missing goals, never overwrites existing matches, and clears observed progress when transferring a setup to a different account.
- Backups are local files, not cloud synchronization. Imports accept only registered goal types and supported backup versions.

Follow [the in-game acceptance checklist](IN_GAME_TESTING.md) to verify tracker accuracy, profile isolation, editing, history, presets, and backups. These checks remain pending player confirmation.

Use same-line opening braces, one enum entry per line, and no line-length-based wrapping. Keep commits focused and separate formatting changes from behavior changes.

Keep block contents multiline and indented, including short if statements and methods. Each type's @since is its creation date, not the date of its latest edit. Run `node tools/format-java.cjs --check` to check block formatting, or omit --check to apply it. Use --dates --check to audit creation dates against Git history; it does not replace knowing the creation date of a new uncommitted type.
